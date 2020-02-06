package com.avenga.steamclient.steam.coordinator.callback;

import com.avenga.steamclient.base.ClientGCProtobufMessage;
import com.avenga.steamclient.base.GCPacketMessage;
import com.avenga.steamclient.model.steam.SteamMessageCallback;
import com.avenga.steamclient.protobufs.dota.DotaGCMessagesClient.CMsgGCMatchDetailsResponse;
import com.avenga.steamclient.steam.client.callback.AbstractCallbackHandler;

public class MatchDetailsCallbackHandler extends AbstractCallbackHandler<GCPacketMessage> {
    public static ClientGCProtobufMessage<CMsgGCMatchDetailsResponse.Builder> handle(SteamMessageCallback<GCPacketMessage> callback) {
        GCPacketMessage gcPacketMessage = waitAndGetPacketMessage(callback, "MatchDetails");

        return new ClientGCProtobufMessage<>(CMsgGCMatchDetailsResponse.class, gcPacketMessage);
    }
}
