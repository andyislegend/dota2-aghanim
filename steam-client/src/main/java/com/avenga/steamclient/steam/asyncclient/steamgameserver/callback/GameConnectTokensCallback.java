package com.avenga.steamclient.steam.asyncclient.steamgameserver.callback;

import com.avenga.steamclient.protobufs.steamclient.SteammessagesClientserver.CMsgClientGameConnectTokens;
import com.avenga.steamclient.steam.asyncclient.callbackmanager.BaseCallbackMessage;
import lombok.Getter;

@Getter
public class GameConnectTokensCallback extends BaseCallbackMessage {
    private int maxTokensToKeep;
    private int tokensCount;

    public GameConnectTokensCallback(CMsgClientGameConnectTokens.Builder builder) {
        this.maxTokensToKeep = builder.getMaxTokensToKeep();
        this.tokensCount = builder.getTokensCount();
    }
}
