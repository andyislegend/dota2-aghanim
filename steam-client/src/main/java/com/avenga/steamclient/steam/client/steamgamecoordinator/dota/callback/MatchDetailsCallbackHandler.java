package com.avenga.steamclient.steam.client.steamgamecoordinator.dota.callback;

import com.avenga.steamclient.base.ClientGCProtobufMessage;
import com.avenga.steamclient.base.GCPacketMessage;
import com.avenga.steamclient.exception.CallbackTimeoutException;
import com.avenga.steamclient.mapper.DotaMatchDetailsMapper;
import com.avenga.steamclient.model.steam.SteamMessageCallback;
import com.avenga.steamclient.model.steam.gamecoordinator.dota.match.DotaMatchDetails;
import com.avenga.steamclient.protobufs.dota.DotaGCMessagesClient.CMsgGCMatchDetailsResponse;
import com.avenga.steamclient.steam.client.SteamClient;
import com.avenga.steamclient.steam.client.callback.AbstractCallbackHandler;
import com.avenga.steamclient.util.CallbackHandlerUtils;

import java.util.Optional;

public class MatchDetailsCallbackHandler extends AbstractCallbackHandler<GCPacketMessage> {

    public static Optional<DotaMatchDetails> handle(SteamMessageCallback<GCPacketMessage> callback, long timeout, SteamClient client) throws CallbackTimeoutException {
        var gcPacketMessage = waitAndGetMessageOrRemoveAfterTimeout(callback, timeout, "MatchDetails", client);

        return CallbackHandlerUtils.getValueOrDefault(gcPacketMessage, MatchDetailsCallbackHandler::getMessage);
    }

    public static DotaMatchDetails getMessage(GCPacketMessage gcPacketMessage) {
        ClientGCProtobufMessage<CMsgGCMatchDetailsResponse.Builder> protobufMessage = new ClientGCProtobufMessage<>(
                CMsgGCMatchDetailsResponse.class, gcPacketMessage);

        return DotaMatchDetailsMapper.mapFromProto(protobufMessage.getBody());
    }
}
