package com.avenga.steamclient.model.proxy;

import lombok.Getter;

import java.net.Proxy;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
public class ProxyState {
    private Proxy proxy;
    private AtomicInteger connectionFailureCount;

    public ProxyState(Proxy proxy) {
        this.proxy = proxy;
    }

    public void incrementFailureCount() {
        connectionFailureCount.incrementAndGet();
    }
}
