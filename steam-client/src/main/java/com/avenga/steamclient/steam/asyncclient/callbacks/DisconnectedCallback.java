package com.avenga.steamclient.steam.asyncclient.callbacks;

import com.avenga.steamclient.steam.CMClient;
import com.avenga.steamclient.steam.asyncclient.callbackmanager.BaseCallbackMessage;

/**
 * This callback is received when the steamclient is physically disconnected from the Steam network.
 */
public class DisconnectedCallback extends BaseCallbackMessage {

    private boolean userInitiated;

    public DisconnectedCallback(boolean userInitiated) {
        this.userInitiated = userInitiated;
    }

    /**
     * @return If true, the disconnection was initiated by calling {@link CMClient#disconnect()}.
     * If false, the disconnection was the cause of something not user-controlled, such as a network failure or
     * a forcible disconnection by the remote server.
     */
    public boolean isUserInitiated() {
        return userInitiated;
    }
}
