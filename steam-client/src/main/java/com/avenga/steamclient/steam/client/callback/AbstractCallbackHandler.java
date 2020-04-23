package com.avenga.steamclient.steam.client.callback;

import com.avenga.steamclient.exception.CallbackTimeoutException;
import com.avenga.steamclient.model.steam.SteamMessageCallback;
import com.avenga.steamclient.steam.client.SteamClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.*;

import static com.avenga.steamclient.constant.Constant.CALLBACK_EXCEPTION_MESSAGE_FORMAT;
import static com.avenga.steamclient.constant.Constant.TIMEOUT_EXCEPTION_MESSAGE_FORMAT;

public abstract class AbstractCallbackHandler<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractCallbackHandler.class);

    /**
     * Waits for callback completion and return packet message received from the Steam Network servers.
     * Packet message will be returned if callback will be finished in time, otherwise callback after specified timeout
     * will be removed from queue. We don't need to cancel {@link CompletableFuture} callback as we don't
     * execute any logic in it.
     * <p>
     * During reconnecting all pending callbacks will be canceled and Optional.empty() will be returned otherwise
     * Optional will contain correspond packet message.
     *
     * @param callback    Registered callback in {@link SteamClient} callback queue.
     * @param timeout     Time during which handler will wait for callback.
     * @param handlerName Name of the handler which handle current callback.
     * @param <T>         Packet message type.
     * @param client      Stean synchronous client.
     * @return Packet message received from the the Steam Network servers.
     * @throws CallbackTimeoutException if the wait timed out.
     */
    protected static <T> Optional<T> waitAndGetMessageOrRemoveAfterTimeout(SteamMessageCallback<T> callback, long timeout,
                                                                           String handlerName, SteamClient client) throws CallbackTimeoutException {
        try {
            return Optional.ofNullable(callback.getCallback().get(timeout, TimeUnit.MILLISECONDS));
        } catch (final TimeoutException e) {
            client.removeCallbackFromQueue(callback);
            throw new CallbackTimeoutException(String.format(TIMEOUT_EXCEPTION_MESSAGE_FORMAT, handlerName, callback.getSequence()), e);
        } catch (final InterruptedException | ExecutionException | CancellationException e) {
            LOGGER.debug(CALLBACK_EXCEPTION_MESSAGE_FORMAT, client.getClientName(), handlerName, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Waits for callback completion and return packet message received from the Steam Network servers.
     * Packet message will be returned if callback will be finished in time, otherwise exception will be thrown.
     * <p>
     * During reconnecting all pending callbacks will be canceled and Optional.empty() will be returned otherwise
     * Optional will contain correspond packet message.
     *
     * @param callback    Registered callback in {@link SteamClient} callback queue.
     * @param timeout     Time during which handler will wait for callback.
     * @param handlerName Name of the handler which handle current callback.
     * @param <T>         Packet message type.
     * @return Packet message received from the the Steam Network servers.
     * @throws CallbackTimeoutException if the wait timed out.
     */
    protected static <T> Optional<T> waitAndGetPacketMessage(SteamMessageCallback<T> callback, long timeout, String handlerName,
                                                             SteamClient client) throws CallbackTimeoutException {
        try {
            return Optional.ofNullable(callback.getCallback().get(timeout, TimeUnit.MILLISECONDS));
        } catch (final TimeoutException e) {
            throw new CallbackTimeoutException(String.format(TIMEOUT_EXCEPTION_MESSAGE_FORMAT, handlerName, callback.getSequence()), e);
        } catch (final InterruptedException | ExecutionException | CancellationException e) {
            LOGGER.debug(CALLBACK_EXCEPTION_MESSAGE_FORMAT, client.getClientName(), handlerName, e.getMessage());
            return Optional.empty();
        }
    }
}
