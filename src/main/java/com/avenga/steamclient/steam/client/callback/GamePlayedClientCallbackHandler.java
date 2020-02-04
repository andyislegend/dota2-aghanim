package com.avenga.steamclient.steam.client.callback;

import com.avenga.steamclient.base.PacketMessage;
import com.avenga.steamclient.exception.CallbackCompletionException;
import com.avenga.steamclient.model.steam.SteamMessageCallback;

import java.util.concurrent.ExecutionException;

import static com.avenga.steamclient.constant.Constant.CALLBACK_EXCEPTION_MESSAGE_FORMAT;

public class GamePlayedClientCallbackHandler {

    public static void handle(SteamMessageCallback<PacketMessage> callback) {
        try {
            callback.getCallback().get();
        } catch (final InterruptedException | ExecutionException e) {
            throw new CallbackCompletionException(String.format(CALLBACK_EXCEPTION_MESSAGE_FORMAT, "GamePlayed", e.getMessage()) , e);
        }
    }
}
