package com.avenga.steamclient.model.proxy;

import lombok.Getter;

import java.net.Proxy;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
public class ProxyState {
    private Proxy proxy;
    private AtomicInteger connectionFailureCount = new AtomicInteger();

    public ProxyState(Proxy proxy) {
        this.proxy = proxy;
    }

    public synchronized void incrementFailureCount() {
        var currentValue = connectionFailureCount.get();
        connectionFailureCount.compareAndSet(currentValue, currentValue + 1);
    }
}
