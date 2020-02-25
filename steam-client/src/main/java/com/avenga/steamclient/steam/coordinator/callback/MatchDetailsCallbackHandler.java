package com.avenga.steamclient.steam.coordinator.callback;

import com.avenga.steamclient.base.ClientGCProtobufMessage;
import com.avenga.steamclient.base.GCPacketMessage;
import com.avenga.steamclient.exception.CallbackTimeoutException;
import com.avenga.steamclient.mapper.DotaMatchDetailsMapper;
import com.avenga.steamclient.model.steam.SteamMessageCallback;
import com.avenga.steamclient.model.steam.gamecoordinator.dota.match.DotaMatchDetails;
import com.avenga.steamclient.protobufs.dota.DotaGCMessagesClient.CMsgGCMatchDetailsResponse;
import com.avenga.steamclient.steam.client.callback.AbstractCallbackHandler;

public class MatchDetailsCallbackHandler extends AbstractCallbackHandler<GCPacketMessage> {

    public static DotaMatchDetails handle(SteamMessageCallback<GCPacketMessage> callback, long timeout) throws CallbackTimeoutException {
        GCPacketMessage gcPacketMessage = waitAndGetPacketMessage(callback, timeout, "MatchDetails");

        return getMessage(gcPacketMessage);
    }

    public static DotaMatchDetails getMessage(GCPacketMessage gcPacketMessage) {
        ClientGCProtobufMessage<CMsgGCMatchDetailsResponse.Builder> protobufMessage = new ClientGCProtobufMessage<>(
                CMsgGCMatchDetailsResponse.class, gcPacketMessage);

        return DotaMatchDetailsMapper.mapFromProto(protobufMessage.getBody());
    }
}
