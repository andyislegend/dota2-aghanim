package com.avenga.steamclient.steam.asyncclient.steamgameserver.callback;

import com.avenga.steamclient.protobufs.steamclient.SteammessagesClientserver.CMsgGSStatusReply;
import com.avenga.steamclient.steam.asyncclient.callbackmanager.BaseCallbackMessage;

/**
 * This callback is fired when the game server receives a status reply.
 */
public class StatusReplyCallback extends BaseCallbackMessage {
    private boolean secure;

    public StatusReplyCallback(CMsgGSStatusReply.Builder reply) {
        secure = reply.getIsSecure();
    }

    /**
     * @return <b>true</b> if this server is VAC secure; otherwise, <b>false</b>.
     */
    public boolean isSecure() {
        return secure;
    }
}
