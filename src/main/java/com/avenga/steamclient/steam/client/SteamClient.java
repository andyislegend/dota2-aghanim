package com.avenga.steamclient.steam.client;

import com.avenga.steamclient.base.*;
import com.avenga.steamclient.constant.Constant;
import com.avenga.steamclient.enums.EMsg;
import com.avenga.steamclient.exception.CallbackQueueException;
import com.avenga.steamclient.model.configuration.SteamConfiguration;
import com.avenga.steamclient.model.steam.SteamMessageCallback;
import com.avenga.steamclient.protobufs.steamclient.SteammessagesClientserver2.CMsgGCClient;
import com.avenga.steamclient.steam.CMClient;
import com.avenga.steamclient.util.MessageUtil;
import com.google.protobuf.ByteString;

import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class SteamClient extends CMClient {
    private BlockingQueue<SteamMessageCallback<PacketMessage>> clientCallbacksQueue = new LinkedBlockingQueue<>();
    private BlockingQueue<SteamMessageCallback<GCPacketMessage>> gameCoordinatorCallbackQueue = new LinkedBlockingQueue<>();

    /**
     * Initializes a new instance of the {@link SteamClient} class with the default configuration.
     */
    public SteamClient() {
        this(new SteamConfiguration());
    }

    /**
     * Initializes a new instance of the {@link SteamClient} class with a specific configuration.
     *
     * @param configuration The configuration to use for this client.
     */
    public SteamClient(SteamConfiguration configuration) {
        super(configuration);
    }

    public SteamMessageCallback<PacketMessage> addCallbackToQueue(int messageCode) {
        var steamMessageCallback = new SteamMessageCallback<PacketMessage>(messageCode);

        if (!clientCallbacksQueue.offer(steamMessageCallback)) {
            throw new CallbackQueueException("Callback for handling message with code '" + messageCode + "' wasn't added to queue");
        }

        return steamMessageCallback;
    }

    public SteamMessageCallback<GCPacketMessage> addGCCallbackToQueue(int messageCode) {
        var steamMessageCallback = new SteamMessageCallback<GCPacketMessage>(messageCode);

        if (!gameCoordinatorCallbackQueue.offer(steamMessageCallback)) {
            throw new CallbackQueueException("Callback for handling message with code '" + messageCode + "' wasn't added to queue");
        }

        return steamMessageCallback;
    }

    public void sendToGC(ClientGCMessage msg, int appId, int eMsg) {
        if (msg == null) {
            throw new IllegalArgumentException("msg is null");
        }
        ClientMessageProtobuf<CMsgGCClient.Builder> clientMsg = new ClientMessageProtobuf<>(CMsgGCClient.class, EMsg.ClientToGC);
        clientMsg.getProtoHeader().setRoutingAppid(appId);
        clientMsg.getBody().setMsgtype(MessageUtil.makeGCMsg(eMsg, msg.isProto()));
        clientMsg.getBody().setAppid(appId);
        clientMsg.getBody().setPayload(ByteString.copyFrom(msg.serialize()));
        this.send(clientMsg);
    }

    @Override
    public boolean onClientMsgReceived(PacketMessage packetMessage) {
        if (!super.onClientMsgReceived(packetMessage)) {
            return false;
        }

        if (packetMessage.getMessageType() == EMsg.ClientFromGC) {
            findAndCompleteGCCallback(packetMessage);
        } else {
            findAndCompleteCallback(packetMessage.getMessageType().code(), packetMessage);
        }
        return true;
    }

    @Override
    protected void onClientConnected() {
        super.onClientConnected();
        findAndCompleteCallback(Constant.CONNECTED_PACKET_CODE, null);
    }

    @Override
    protected void onClientDisconnected(boolean userInitiated) {
        super.onClientDisconnected(userInitiated);
        findAndCompleteCallback(Constant.DISCONNECTED_PACKET_CODE, null);
    }

    private void findAndCompleteCallback(int messageCode, PacketMessage packetMessage) {
        Optional<SteamMessageCallback<PacketMessage>> messageCallback = clientCallbacksQueue.stream()
                .filter(callback -> callback.getMessageCode() == messageCode)
                .findFirst();

        messageCallback.ifPresent(callback -> {
            clientCallbacksQueue.remove(callback);
            callback.getCallback().complete(packetMessage);
        });
    }

    private void findAndCompleteGCCallback(PacketMessage packetMessage) {
        var message = new ClientMessageProtobuf<CMsgGCClient.Builder>(CMsgGCClient.class, packetMessage).getBody();
        var packetGCMessage = getPacketGCMsg(message.getMsgtype(), message.getPayload().toByteArray());
        var messageCallback = gameCoordinatorCallbackQueue.stream()
                .filter(callback -> callback.getMessageCode() == packetGCMessage.getMessageType())
                .findFirst();
        messageCallback.ifPresent(callback -> {
            gameCoordinatorCallbackQueue.remove(callback);
            callback.getCallback().complete(packetGCMessage);
        });
    }

    private GCPacketMessage getPacketGCMsg(int eMsg, byte[] data) {
        int realEMsg = MessageUtil.getGCMsg(eMsg);

        if (MessageUtil.isProtoBuf(eMsg)) {
            return new GCPacketClientMessageProtobuf(realEMsg, data);
        } else {
            return new GCPacketClientMessage(realEMsg, data);
        }
    }
}
