package com.avenga.steamclient.steam.asyncclient.callbackmanager;

import com.avenga.steamclient.model.JobID;
import com.avenga.steamclient.steam.asyncclient.SteamClientAsync;

import java.io.Closeable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * This class is a utility for routing callbacks to function calls.
 * In order to bind callbacks to functions, an instance of this class must be created for the
 * {@link SteamClientAsync SteamClient} instance that will be posting callbacks.
 */
public class DefaultCallbackManager implements CallbackManager {

    private SteamClientAsync steamClientAsync;

    private Set<BaseCallback> registeredCallbacks = Collections.newSetFromMap(new ConcurrentHashMap<>());

    /**
     * Initializes a new instance of the {@link DefaultCallbackManager} class.
     *
     * @param steamClientAsync The {@link SteamClientAsync SteamClient} instance to handle the callbacks of.
     */
    public DefaultCallbackManager(SteamClientAsync steamClientAsync) {
        Objects.requireNonNull(steamClientAsync, "Async steam client wasn't provided");

        this.steamClientAsync = steamClientAsync;
    }

    /**
     * Runs a single queued callback.
     * If no callback is queued, this method will instantly return.
     */
    public void runCallbacks() {
        CallbackMessage call = steamClientAsync.getCallback(true);

        if (call != null) {
            handle(call);
        }
    }

    /**
     * Blocks the current thread to run a single queued callback.
     * If no callback is queued, the method will block for the given timeout.
     *
     * @param timeout The length of time to block.
     */
    public void runWaitCallbacks(long timeout) {
        CallbackMessage call = steamClientAsync.waitForCallback(true, timeout);

        if (call != null) {
            handle(call);
        }
    }

    /**
     * Blocks the current thread to run a single queued callback.
     * If no callback is queued, the method will block until one is posted.
     */
    public void runWaitCallbacks() {
        CallbackMessage call = steamClientAsync.waitForCallback(true);

        if (call != null) {
            handle(call);
        }
    }

    /**
     * Blocks the current thread to run all queued callbacks.
     * If no callback is queued, the method will block for the given timeout.
     *
     * @param timeout The length of time to block.
     */
    public void runWaitAllCallbacks(int timeout) {
        List<CallbackMessage> calls = steamClientAsync.getAllCallbacks(true, timeout);
        for (CallbackMessage call : calls) {
            handle(call);
        }
    }

    /**
     * Registers the provided {@link Consumer} to receive callbacks of type {@link TCallback}
     *
     * @param callbackType type of the callback
     * @param jobID        The {@link JobID}  of the callbacks that should be subscribed to.
     * @param callbackConsumer The function to invoke with the callback.
     * @param <TCallback>  The type of callback to subscribe to.
     * @return An {@link Closeable}. Disposing of the return value will unsubscribe the callbackConsumer .
     */
    public <TCallback extends CallbackMessage> Closeable subscribe(Class<? extends TCallback> callbackType, JobID jobID,
                                                                   Consumer<TCallback> callbackConsumer) {
        Objects.requireNonNull(jobID, "Job id wasn't provided");
        Objects.requireNonNull(callbackConsumer, "Callback consumer wasn't provided");

        Callback<TCallback> callback = new Callback<>(callbackType, callbackConsumer, this, jobID);
        return new Subscription(this, callback);
    }

    /**
     * REgisters the provided {@link Consumer} to receive callbacks of type {@link TCallback}
     *
     * @param callbackType type of the callback
     * @param callbackConsumer The function to invoke with the callback.
     * @param <TCallback>  The type of callback to subscribe to.
     * @return An {@link Closeable}. Disposing of the return value will unsubscribe the callbackConsumer .
     */
    public <TCallback extends CallbackMessage> Closeable subscribe(Class<? extends TCallback> callbackType,
                                                                   Consumer<TCallback> callbackConsumer) {
        return subscribe(callbackType, JobID.INVALID, callbackConsumer);
    }

    @Override
    public void register(BaseCallback callback)
    {
        Objects.requireNonNull(callback, "Callback wasn't provided");

        registeredCallbacks.add(callback);
    }

    @Override
    public void unregister(BaseCallback callback) {
        registeredCallbacks.remove(callback);
    }

    private void handle(CallbackMessage message) {
        for (BaseCallback callback : registeredCallbacks) {
            if (callback.getCallbackType().isAssignableFrom(message.getClass())) {
                callback.run(message);
            }
        }
    }

}
