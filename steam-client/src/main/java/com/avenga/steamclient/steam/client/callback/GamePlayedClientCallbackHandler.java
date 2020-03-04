package com.avenga.steamclient.steam.client.callback;

import com.avenga.steamclient.base.PacketMessage;
import com.avenga.steamclient.exception.CallbackTimeoutException;
import com.avenga.steamclient.model.steam.SteamMessageCallback;
import com.avenga.steamclient.steam.client.SteamClient;

public class GamePlayedClientCallbackHandler extends AbstractCallbackHandler<PacketMessage> {

    public static void handle(SteamMessageCallback<PacketMessage> callback, long timeout, SteamClient client) throws CallbackTimeoutException {
        waitAndGetMessageOrRemoveAfterTimeout(callback, timeout, "GamePlayed", client);
    }
}
