package com.avenga.steamclient.steam.coordinator.callback;

import com.avenga.steamclient.base.ClientGCProtobufMessage;
import com.avenga.steamclient.base.GCPacketMessage;
import com.avenga.steamclient.model.steam.SteamMessageCallback;
import com.avenga.steamclient.protobufs.dota.DotaGCMessagesCommon.CMsgDOTAProfileCard;
import com.avenga.steamclient.steam.client.callback.AbstractCallbackHandler;

public class ProfileCardCallbackHandler extends AbstractCallbackHandler<GCPacketMessage> {
    public static ClientGCProtobufMessage<CMsgDOTAProfileCard.Builder> handle(SteamMessageCallback<GCPacketMessage> callback) {
        GCPacketMessage gcPacketMessage = waitAndGetPacketMessage(callback, "ProfileCard");

        return new ClientGCProtobufMessage<>(CMsgDOTAProfileCard.class, gcPacketMessage);
    }
}
