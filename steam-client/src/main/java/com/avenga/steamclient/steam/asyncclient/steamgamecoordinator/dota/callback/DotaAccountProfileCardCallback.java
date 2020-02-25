package com.avenga.steamclient.steam.asyncclient.steamgamecoordinator.dota.callback;

import com.avenga.steamclient.mapper.DotaAccountProfileCardMapper;
import com.avenga.steamclient.model.steam.gamecoordinator.dota.account.DotaProfileCard;
import com.avenga.steamclient.protobufs.dota.DotaGCMessagesCommon.CMsgDOTAProfileCard;
import com.avenga.steamclient.steam.asyncclient.callbackmanager.BaseCallbackMessage;
import lombok.Getter;

@Getter
public class DotaAccountProfileCardCallback extends BaseCallbackMessage {

    private DotaProfileCard dotaProfileCard;

    public DotaAccountProfileCardCallback(CMsgDOTAProfileCard.Builder builder) {
        this.dotaProfileCard = DotaAccountProfileCardMapper.mapFromProto(builder);
    }
}
