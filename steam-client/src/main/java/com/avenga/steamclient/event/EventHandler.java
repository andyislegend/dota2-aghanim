package com.avenga.steamclient.event;

public interface EventHandler<T extends EventArgs> {
    void handleEvent(Object sender, T e);
}
