package com.avenga.steamclient.model.proxy;

import lombok.Getter;

import java.net.Proxy;

@Getter
public class ProxyState {
    private Proxy proxy;
    private int connectionFailureCount;

    public ProxyState(Proxy proxy) {
        this.proxy = proxy;
    }

    public void incrementFailureCount() {
        connectionFailureCount++;
    }
}
