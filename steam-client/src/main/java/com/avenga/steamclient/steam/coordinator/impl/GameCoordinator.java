package com.avenga.steamclient.steam.coordinator.impl;

import com.avenga.steamclient.base.ClientGCMessage;
import com.avenga.steamclient.base.ClientMessageProtobuf;
import com.avenga.steamclient.base.GCPacketMessage;
import com.avenga.steamclient.enums.EMsg;
import com.avenga.steamclient.exception.CallbackQueueException;
import com.avenga.steamclient.model.steam.SteamMessageCallback;
import com.avenga.steamclient.protobufs.steamclient.SteammessagesClientserver2.CMsgGCClient;
import com.avenga.steamclient.steam.client.SteamClient;
import com.avenga.steamclient.steam.coordinator.AbstractGameCoordinator;
import com.avenga.steamclient.util.MessageUtil;
import com.google.protobuf.ByteString;
import com.google.protobuf.ProtocolMessageEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class GameCoordinator extends AbstractGameCoordinator {

    private static final Logger LOGGER = LoggerFactory.getLogger(GameCoordinator.class);

    public GameCoordinator(SteamClient client) {
        super(client);
    }

    @Override
    public void send(ClientGCMessage message, int appId, ProtocolMessageEnum messageEnum) {
        Objects.requireNonNull(message, "Client Game Coordinator message wasn't provided");

        LOGGER.debug("Sent GC -> EMsg: {} (id: {})", messageEnum.getValueDescriptor().getName(), messageEnum.getNumber());

        var clientMsg = new ClientMessageProtobuf<CMsgGCClient.Builder>(CMsgGCClient.class, EMsg.ClientToGC);
        clientMsg.getProtoHeader().setRoutingAppid(appId);
        clientMsg.getBody().setMsgtype(MessageUtil.makeGCMsg(messageEnum.getNumber(), message.isProto()));
        clientMsg.getBody().setAppid(appId);
        clientMsg.getBody().setPayload(ByteString.copyFrom(message.serialize()));
        client.send(clientMsg);
    }

    @Override
    public SteamMessageCallback<GCPacketMessage> addCallback(int messageCode) {
        var steamMessageCallback = new SteamMessageCallback<GCPacketMessage>(messageCode, client.getQueueSequence().getAndIncrement());
        if (!callbackQueue.offer(steamMessageCallback)) {
            throw new CallbackQueueException("Callback for handling message with code '" + messageCode + "' wasn't added to queue");
        }
        return steamMessageCallback;
    }
}
