package com.avenga.steamclient.steam.asyncclient.callbackmanager;

import java.io.Closeable;

public class Subscription implements Closeable {
    private CallbackManager manager;
    private BaseCallback callback;

    public Subscription(CallbackManager manager, BaseCallback callback) {
        this.manager = manager;
        this.callback = callback;
    }

    @Override
    public void close() {
        if (callback != null && manager != null) {
            manager.unregister(callback);
            callback = null;
            manager = null;
        }
    }
}
