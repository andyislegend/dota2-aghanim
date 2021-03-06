package com.avenga.steamclient.steam.client.steamgamecoordinator.dota.callback;

import com.avenga.steamclient.base.ClientGCProtobufMessage;
import com.avenga.steamclient.base.GCPacketMessage;
import com.avenga.steamclient.exception.CallbackTimeoutException;
import com.avenga.steamclient.mapper.DotaAccountProfileCardMapper;
import com.avenga.steamclient.model.steam.SteamMessageCallback;
import com.avenga.steamclient.model.steam.gamecoordinator.dota.account.DotaProfileCard;
import com.avenga.steamclient.protobufs.dota.DotaGCMessagesCommon.CMsgDOTAProfileCard;
import com.avenga.steamclient.steam.client.SteamClient;
import com.avenga.steamclient.steam.client.callback.AbstractCallbackHandler;
import com.avenga.steamclient.util.CallbackHandlerUtils;

import java.util.Optional;

public class ProfileCardCallbackHandler extends AbstractCallbackHandler<GCPacketMessage> {

    public static Optional<DotaProfileCard> handle(SteamMessageCallback<GCPacketMessage> callback, long timeout, SteamClient client) throws CallbackTimeoutException {
        var gcPacketMessage = waitAndGetMessageOrRemoveAfterTimeout(callback, timeout, "ProfileCard", client);

        return CallbackHandlerUtils.getValueOrDefault(gcPacketMessage, ProfileCardCallbackHandler::getMessage);
    }

    public static DotaProfileCard getMessage(GCPacketMessage gcPacketMessage) {
        ClientGCProtobufMessage<CMsgDOTAProfileCard.Builder> protobufMessage = new ClientGCProtobufMessage<>(
                CMsgDOTAProfileCard.class, gcPacketMessage);

        return DotaAccountProfileCardMapper.mapFromProto(protobufMessage.getBody());
    }
}
