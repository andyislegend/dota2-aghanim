package com.avenga.steamclient.steam.asyncclient.steamuser.callback;

import com.avenga.steamclient.protobufs.steamclient.SteammessagesClientserver.CMsgClientSessionToken;
import com.avenga.steamclient.steam.asyncclient.callbackmanager.BaseCallbackMessage;
import lombok.Getter;

/**
 * This callback is fired when the client receives it's unique Steam3 session token. This token is used for authenticated content downloading in Steam2.
 */
@Getter
public class SessionTokenCallback extends BaseCallbackMessage {
    private long sessionToken;

    public SessionTokenCallback(CMsgClientSessionToken.Builder message) {
        sessionToken = message.getToken();
    }
}
