package com.avenga.steamclient.steam.asyncclient.steamgamecoordinator.dota.callback;

import com.avenga.steamclient.mapper.DotaMatchDetailsMapper;
import com.avenga.steamclient.model.steam.gamecoordinator.dota.match.*;
import com.avenga.steamclient.protobufs.dota.DotaGCMessagesClient.CMsgGCMatchDetailsResponse;
import com.avenga.steamclient.steam.asyncclient.callbackmanager.BaseCallbackMessage;
import lombok.Getter;

@Getter
public class DotaMatchDetailsCallback extends BaseCallbackMessage {

    private DotaMatchDetails dotaMatchDetails;

    public DotaMatchDetailsCallback(CMsgGCMatchDetailsResponse.Builder builder) {
        this.dotaMatchDetails = DotaMatchDetailsMapper.mapFromProto(builder);
    }
}
