package com.avenga.steamclient.steam.coordinator;

import com.avenga.steamclient.base.*;
import com.avenga.steamclient.enums.EMsg;
import com.avenga.steamclient.exception.CallbackQueueException;
import com.avenga.steamclient.model.steam.SteamMessageCallback;
import com.avenga.steamclient.protobufs.steamclient.SteammessagesClientserver2.CMsgGCClient;
import com.avenga.steamclient.steam.client.SteamClient;
import com.avenga.steamclient.util.MessageUtil;
import com.google.protobuf.ByteString;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class GameCoordinator {
    private final BlockingQueue<SteamMessageCallback<GCPacketMessage>> callbackQueue = new LinkedBlockingQueue<>();
    private final SteamClient client;

    public GameCoordinator(SteamClient client) {
        this.client = client;
        client.setOnGcCallback((this::findAndCompleteCallback));
    }

    public void send(ClientGCMessage msg, int appId, int eMsg) {
        if (msg == null) {
            throw new IllegalArgumentException("msg is null");
        }
        var clientMsg = new ClientMessageProtobuf<CMsgGCClient.Builder>(CMsgGCClient.class, EMsg.ClientToGC);
        clientMsg.getProtoHeader().setRoutingAppid(appId);
        clientMsg.getBody().setMsgtype(MessageUtil.makeGCMsg(eMsg, msg.isProto()));
        clientMsg.getBody().setAppid(appId);
        clientMsg.getBody().setPayload(ByteString.copyFrom(msg.serialize()));
        client.send(clientMsg);
    }

    public SteamMessageCallback<GCPacketMessage> addCallback(int messageCode) {
        var steamMessageCallback = new SteamMessageCallback<GCPacketMessage>(messageCode);
        if (!callbackQueue.offer(steamMessageCallback)) {
            throw new CallbackQueueException("Callback for handling message with code '" + messageCode + "' wasn't added to queue");
        }
        return steamMessageCallback;
    }

    private void findAndCompleteCallback(PacketMessage packetMessage) {
        var message = new ClientMessageProtobuf<CMsgGCClient.Builder>(CMsgGCClient.class, packetMessage).getBody();
        var packetGCMessage = getPacketGCMsg(message.getMsgtype(), message.getPayload().toByteArray());
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
