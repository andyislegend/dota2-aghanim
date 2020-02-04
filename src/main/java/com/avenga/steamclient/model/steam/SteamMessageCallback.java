package com.avenga.steamclient.model.steam;

import lombok.Getter;

import java.util.concurrent.CompletableFuture;

@Getter
public class SteamMessageCallback<T> {
    private int messageCode;
    private CompletableFuture<T> callback;

    public SteamMessageCallback(int messageCode) {
        this.messageCode = messageCode;
        this.callback = new CompletableFuture<>();
    }
}
