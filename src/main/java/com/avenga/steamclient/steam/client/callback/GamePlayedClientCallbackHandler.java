package com.avenga.steamclient.steam.client.callback;

import com.avenga.steamclient.base.PacketMessage;
import com.avenga.steamclient.model.steam.SteamMessageCallback;

public class GamePlayedClientCallbackHandler extends AbstractCallbackHandler<PacketMessage> {

    public static void handle(SteamMessageCallback<PacketMessage> callback) {
        waitAndGetPacketMessage(callback, "GamePlayed");
    }
}
