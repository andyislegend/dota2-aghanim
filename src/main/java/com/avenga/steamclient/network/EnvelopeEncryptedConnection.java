package com.avenga.steamclient.network;

import com.avenga.steamclient.base.Message;
import com.avenga.steamclient.base.PacketMessage;
import com.avenga.steamclient.crypto.CryptoHelper;
import com.avenga.steamclient.crypto.KeyDictionary;
import com.avenga.steamclient.crypto.RSACrypto;
import com.avenga.steamclient.enums.*;
import com.avenga.steamclient.event.EventArgs;
import com.avenga.steamclient.event.EventHandler;
import com.avenga.steamclient.generated.MsgChannelEncryptRequest;
import com.avenga.steamclient.generated.MsgChannelEncryptResponse;
import com.avenga.steamclient.generated.MsgChannelEncryptResult;
import com.avenga.steamclient.steam.CMClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Objects;

public class EnvelopeEncryptedConnection extends Connection {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvelopeEncryptedConnection.class);

    private final Connection inner;
    private final EUniverse universe;
    private EncryptionState state;
    private INetFilterEncryption encryption;

    private final EventHandler<EventArgs> onConnected = new EventHandler<EventArgs>() {
        @Override
        public void handleEvent(Object sender, EventArgs e) {
            state = EncryptionState.CONNECTED;
        }
    };

    private final EventHandler<DisconnectedEventArgs> onDisconnected = new EventHandler<DisconnectedEventArgs>() {
        @Override
        public void handleEvent(Object sender, DisconnectedEventArgs e) {
            state = EncryptionState.DISCONNECTED;
            encryption = null;

            disconnected.handleEvent(EnvelopeEncryptedConnection.this, e);
        }
    };

    private final EventHandler<NetMsgEventArgs> onNetMsgReceived = new EventHandler<>() {
        @Override
        public void handleEvent(Object sender, NetMsgEventArgs e) {
            if (state == EncryptionState.ENCRYPTED) {
                byte[] plaintextData = encryption.processIncoming(e.getData());
                netMsgReceived.handleEvent(EnvelopeEncryptedConnection.this, e.withData(plaintextData));
                return;
            }

            PacketMessage packetMessage = CMClient.getPacketMsg(e.getData());

            if (!isExpectedEMsg(packetMessage.getMessageType())) {
                LOGGER.debug("Rejected EMsg: " + packetMessage.getMessageType() + " during channel setup");
                return;
            }

            switch (packetMessage.getMessageType()) {
                case ChannelEncryptRequest:
                    handleEncryptRequest(packetMessage);
                    break;
                case ChannelEncryptResult:
                    handleEncryptResult(packetMessage);
                    break;
            }
        }
    };

    public EnvelopeEncryptedConnection(Connection inner, EUniverse universe) {
        Objects.requireNonNull(inner, "inner connection wasn't provided");

        this.inner = inner;
        this.universe = universe;

        inner.getNetMsgReceived().addEventHandler(onNetMsgReceived);
        inner.getConnected().addEventHandler(onConnected);
        inner.getDisconnected().addEventHandler(onDisconnected);
    }

    private void handleEncryptRequest(PacketMessage packetMessage) {
        Message<MsgChannelEncryptRequest> request = new Message<>(MsgChannelEncryptRequest.class, packetMessage);

        EUniverse connectedUniverse = request.getBody().getUniverse();
        long protoVersion = request.getBody().getProtocolVersion();

        LOGGER.debug("Got encryption request. Universe: " + connectedUniverse + " Protocol ver: " + protoVersion);

        if (protoVersion != MsgChannelEncryptRequest.PROTOCOL_VERSION) {
            LOGGER.debug("Encryption handshake protocol version mismatch!");
        }

        if (connectedUniverse != universe) {
            LOGGER.debug("Expected universe " + universe + " but server reported universe " + connectedUniverse);
        }

        byte[] randomChallenge = null;
        if (request.getPayload().getLength() >= 16) {
            randomChallenge = request.getPayload().toByteArray();
        }

        byte[] publicKey = KeyDictionary.getPublicKey(connectedUniverse);

        if (publicKey == null) {
            LOGGER.debug("HandleEncryptRequest got request for invalid universe! Universe: " + connectedUniverse + " Protocol ver: " + protoVersion);
            disconnect();
        }

        Message<MsgChannelEncryptResponse> response = new Message<>(MsgChannelEncryptResponse.class);

        byte[] tempSessionKey = CryptoHelper.generateRandomBlock(32);
        byte[] encryptedHandshakeBlob = null;

        RSACrypto rsa = new RSACrypto(publicKey);

        if (randomChallenge != null) {
            byte[] blobToEncrypt = new byte[tempSessionKey.length + randomChallenge.length];

            System.arraycopy(tempSessionKey, 0, blobToEncrypt, 0, tempSessionKey.length);
            System.arraycopy(randomChallenge, 0, blobToEncrypt, tempSessionKey.length, randomChallenge.length);

            encryptedHandshakeBlob = rsa.encrypt(blobToEncrypt);
        } else {
            encryptedHandshakeBlob = rsa.encrypt(tempSessionKey);
        }

        byte[] keyCrc = CryptoHelper.crcHash(encryptedHandshakeBlob);

        try {
            response.write(encryptedHandshakeBlob);
            response.write(keyCrc);
            response.write(0);
        } catch (IOException e) {
            LOGGER.debug(e.getMessage(), e);
        }

        if (randomChallenge != null) {
            encryption = new NetFilterEncryptionWithHMAC(tempSessionKey);
        } else {
            encryption = new NetFilterEncryption(tempSessionKey);
        }

        state = EncryptionState.CHALLENGED;

        send(response.serialize());
    }

    private void handleEncryptResult(PacketMessage packetMessage) {
        Message<MsgChannelEncryptResult> result = new Message<>(MsgChannelEncryptResult.class, packetMessage);

        LOGGER.debug("Encryption result: " + result.getBody().getResult());

        assert encryption != null;

        if (result.getBody().getResult() == EResult.OK && encryption != null) {
            state = EncryptionState.ENCRYPTED;
            connected.handleEvent(this, EventArgs.EMPTY);
        } else {
            LOGGER.debug("Encryption channel setup failed");
            disconnect();
        }
    }

    private boolean isExpectedEMsg(EMsg msg) {
        switch (state) {
            case DISCONNECTED:
                return false;
            case CONNECTED:
                return msg == EMsg.ChannelEncryptRequest;
            case CHALLENGED:
                return msg == EMsg.ChannelEncryptResult;
            case ENCRYPTED:
                return true;
            default:
                throw new IllegalStateException("Unreachable - landed up in undefined state.");
        }
    }

    @Override
    public void connect(InetSocketAddress endPoint, int timeout) {
        inner.connect(endPoint, timeout);
    }

    @Override
    public void disconnect() {
        inner.disconnect();
    }

    @Override
    public void send(byte[] data) {
        if (state == EncryptionState.ENCRYPTED) {
            data = encryption.processOutgoing(data);
        }

        inner.send(data);
    }

    @Override
    public InetAddress getLocalIP() {
        return inner.getLocalIP();
    }

    @Override
    public InetSocketAddress getCurrentEndPoint() {
        return inner.getCurrentEndPoint();
    }

    @Override
    public ProtocolType getProtocolTypes() {
        return inner.getProtocolTypes();
    }
}
