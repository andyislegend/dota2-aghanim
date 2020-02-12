package com.avenga.steamclient.steam.coordinator.callback;

import com.avenga.steamclient.base.ClientGCProtobufMessage;
import com.avenga.steamclient.base.GCPacketMessage;
import com.avenga.steamclient.exception.CallbackTimeoutException;
import com.avenga.steamclient.model.steam.SteamMessageCallback;
import com.avenga.steamclient.protobufs.dota.DotaGCMessagesCommon.CMsgDOTAProfileCard;
import com.avenga.steamclient.steam.client.callback.AbstractCallbackHandler;

public class ProfileCardCallbackHandler extends AbstractCallbackHandler<GCPacketMessage> {

    public static CMsgDOTAProfileCard handle(SteamMessageCallback<GCPacketMessage> callback) {
        GCPacketMessage gcPacketMessage = waitAndGetPacketMessage(callback, "ProfileCard");

        return getMessage(gcPacketMessage);
    }

    public static CMsgDOTAProfileCard handle(SteamMessageCallback<GCPacketMessage> callback, long timeout) throws CallbackTimeoutException {
        GCPacketMessage gcPacketMessage = waitAndGetPacketMessage(callback, timeout, "ProfileCard");

        return getMessage(gcPacketMessage);
    }

    private static CMsgDOTAProfileCard getMessage(GCPacketMessage gcPacketMessage) {
        ClientGCProtobufMessage<CMsgDOTAProfileCard.Builder> protobufMessage = new ClientGCProtobufMessage<>(
                CMsgDOTAProfileCard.class, gcPacketMessage);

        return protobufMessage.getBody().build();
    }
}
