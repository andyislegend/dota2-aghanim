package com.avenga.steamclient.steam.asyncclient.steamuser.callback;

import com.avenga.steamclient.protobufs.steamclient.SteammessagesClientserverLogin.CMsgClientNewLoginKey;
import com.avenga.steamclient.steam.asyncclient.callbackmanager.BaseCallbackMessage;
import lombok.Getter;

/**
 * This callback is returned some time after logging onto the network.
 */
@Getter
public class LoginKeyCallback extends BaseCallbackMessage {
    private String loginKey;
    private int uniqueID;

    public LoginKeyCallback(CMsgClientNewLoginKey.Builder logKey) {
        this.loginKey = logKey.getLoginKey();
        this.uniqueID = logKey.getUniqueId();
    }
}
