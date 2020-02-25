package com.avenga.steamclient.steam.asyncclient.callbackmanager;

/**
 * This is the base class for the utility {@link Callback} class.
 * This is for internal use only, and shouldn't be used directly.
 */
public abstract class BaseCallback {
    abstract Class getCallbackType();

    abstract void run(Object genericCallback);
}
