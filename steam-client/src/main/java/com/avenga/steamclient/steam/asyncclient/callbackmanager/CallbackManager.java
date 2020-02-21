package com.avenga.steamclient.steam.asyncclient.callbackmanager;

public interface CallbackManager {

   void register(BaseCallback callback);

    void unregister(BaseCallback callback);
}
