package com.avenga.steamclient.steam.client.callback;

import com.avenga.steamclient.constant.Constant;
import com.avenga.steamclient.exception.CallbackCompletionException;
import com.avenga.steamclient.model.steam.SteamMessageCallback;

import java.util.concurrent.ExecutionException;

import static com.avenga.steamclient.constant.Constant.CALLBACK_EXCEPTION_MESSAGE_FORMAT;

public class ConnectedClientCallbackHandler {

    public static final int CALLBACK_MESSAGE_CODE = Constant.CONNECTED_PACKET_CODE;

    public static void handle(SteamMessageCallback steamMessageCallback) {
        try {
            steamMessageCallback.getCallback().get();
        } catch (final InterruptedException | ExecutionException e) {
            throw new CallbackCompletionException(String.format(CALLBACK_EXCEPTION_MESSAGE_FORMAT, "ConnectedClient", e.getMessage()) , e);
        }
    }
}
