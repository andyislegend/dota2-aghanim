package com.avenga.steamclient.steam.client.callback;

import com.avenga.steamclient.exception.CallbackCompletionException;
import com.avenga.steamclient.model.steam.SteamMessageCallback;

import java.util.concurrent.ExecutionException;

import static com.avenga.steamclient.constant.Constant.CALLBACK_EXCEPTION_MESSAGE_FORMAT;

public abstract class AbstractCallbackHandler<T> {

    protected static <T> T waitAndGetPacketMessage(SteamMessageCallback<T> callback, String handlerName) {
        try {
            return callback.getCallback().get();
        } catch (final InterruptedException | ExecutionException e) {
            throw new CallbackCompletionException(String.format(CALLBACK_EXCEPTION_MESSAGE_FORMAT, handlerName, e.getMessage()) , e);
        }
    }
}
