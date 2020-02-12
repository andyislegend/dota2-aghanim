package com.avenga.steamclient.steam.coordinator;

import com.avenga.steamclient.base.*;
import com.avenga.steamclient.model.steam.SteamMessageCallback;
import com.avenga.steamclient.protobufs.steamclient.SteammessagesClientserver2.CMsgGCClient;
import com.avenga.steamclient.steam.client.SteamClient;
import com.avenga.steamclient.util.MessageUtil;
import com.avenga.steamclient.util.SteamEnumUtils;
import com.google.protobuf.ProtocolMessageEnum;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public abstract class AbstractGameCoordinator {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractGameCoordinator.class);

    protected final BlockingQueue<SteamMessageCallback<GCPacketMessage>> callbackQueue = new LinkedBlockingQueue<>();

    @Getter
    protected final SteamClient client;

    public AbstractGameCoordinator(SteamClient client) {
        this.client = client;
        client.setOnGcCallback((this::findAndCompleteCallback));
    }

    public abstract void send(ClientGCMessage msg, int appId, ProtocolMessageEnum messageEnum);

    public abstract SteamMessageCallback<GCPacketMessage> addCallback(int messageCode);

    protected void findAndCompleteCallback(PacketMessage packetMessage) {
        var message = new ClientMessageProtobuf<CMsgGCClient.Builder>(CMsgGCClient.class, packetMessage).getBody();
        var packetGCMessage = getPacketGCMsg(message.getMsgtype(), message.getPayload().toByteArray());

        LOGGER.debug(String.format("<- Recv'd GC EMsg: %s (%d) (Proto: %s)", SteamEnumUtils.getEnumName(packetGCMessage.getMessageType()),
                packetGCMessage.getMessageType(), packetGCMessage.isProto()));

        var messageCallback = callbackQueue.stream()
                .filter(callback -> callback.getMessageCode() == packetGCMessage.getMessageType())
                .findFirst();
        messageCallback.ifPresent(callback -> {
            callbackQueue.remove(callback);
            callback.getCallback().complete(packetGCMessage);
        });
    }

    private GCPacketMessage getPacketGCMsg(int eMsg, byte[] data) {
        var realEMsg = MessageUtil.getGCMsg(eMsg);
        return MessageUtil.isProtoBuf(eMsg) ? new GCPacketClientMessageProtobuf(realEMsg, data) :
                new GCPacketClientMessage(realEMsg, data);
    }
}
