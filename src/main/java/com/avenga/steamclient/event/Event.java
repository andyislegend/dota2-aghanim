package com.avenga.steamclient.event;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Event<T extends EventArgs> {
    protected final Set<EventHandler<T>> handlers = ConcurrentHashMap.newKeySet();

    public void addEventHandler(EventHandler<T> handler) {
        handlers.add(handler);
    }

    public void removeEventHandler(EventHandler<T> handler) {
        handlers.remove(handler);
    }

    public void handleEvent(Object sender, T e) {
        for (final EventHandler<T> handler : handlers) {
            handler.handleEvent(sender, e);
        }
    }
}