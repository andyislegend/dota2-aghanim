package com.avenga.steamclient.steam.asyncclient.steamuser.callback;

import com.avenga.steamclient.enums.EResult;
import com.avenga.steamclient.steam.asyncclient.callbackmanager.BaseCallbackMessage;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * This callback is returned when the client is told to log off by the server.
 */
@Getter
@AllArgsConstructor
public class LoggedOffCallback extends BaseCallbackMessage {
    private EResult result;
}
