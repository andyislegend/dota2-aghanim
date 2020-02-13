package com.avenga.steamclient.steam.client.callback;

import com.avenga.steamclient.exception.CallbackCompletionException;
import com.avenga.steamclient.exception.CallbackTimeoutException;
import com.avenga.steamclient.model.steam.SteamMessageCallback;
import com.avenga.steamclient.steam.client.SteamClient;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.avenga.steamclient.constant.Constant.CALLBACK_EXCEPTION_MESSAGE_FORMAT;
import static com.avenga.steamclient.constant.Constant.TIMEOUT_EXCEPTION_MESSAGE_FORMAT;

public abstract class AbstractCallbackHandler<T> {

    private static final long CHECK_CALLBACK_TIMEOUT = 200;

    /**
     * Waits for callback completion and return packet message received from the Steam Network servers.
     *
     * @param callback Registered callback in {@link SteamClient} callback queue.
     * @param handlerName Name of the handler which handle current callback.
     * @param <T> Packet message type.
     * @return Packet message received from the the Steam Network servers.
     */
    protected static <T> T waitAndGetPacketMessage(SteamMessageCallback<T> callback, String handlerName) {
        try {
            var completableFuture = callback.getCallback();
            while (!completableFuture.isDone()) {
                TimeUnit.MILLISECONDS.sleep(CHECK_CALLBACK_TIMEOUT);
            }

            return completableFuture.get();
        } catch (final InterruptedException | ExecutionException e) {
            throw new CallbackCompletionException(String.format(CALLBACK_EXCEPTION_MESSAGE_FORMAT, handlerName, e.getMessage()) , e);
        }
    }

    /**
     * Waits for callback completion and return packet message received from the Steam Network servers.
     * Packet message will be returned if callback will be finished in time, otherwise callback after specified timeout will be canceled.
     *
     * @param callback Registered callback in {@link SteamClient} callback queue.
     * @param timeout Time during which handler will wait for callback.
     * @param handlerName Name of the handler which handle current callback.
     * @param <T> Packet message type.
     *
     * @throws CallbackTimeoutException if the wait timed out
     * @return Packet message received from the the Steam Network servers.
     */
    protected static <T> T waitAndGetPacketMessage(SteamMessageCallback<T> callback, long timeout, String handlerName) throws CallbackTimeoutException {
        try {
            return callback.getCallback().get(timeout, TimeUnit.MILLISECONDS);
        } catch (final TimeoutException e) {
            callback.getCallback().cancel(true);
            throw new CallbackTimeoutException(String.format(TIMEOUT_EXCEPTION_MESSAGE_FORMAT, handlerName, callback.getSequence()), e);
        } catch (final InterruptedException | ExecutionException e) {
            throw new CallbackCompletionException(String.format(CALLBACK_EXCEPTION_MESSAGE_FORMAT, handlerName, e.getMessage()) , e);
        }
    }
}
