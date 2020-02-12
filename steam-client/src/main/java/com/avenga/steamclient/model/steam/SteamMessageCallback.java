package com.avenga.steamclient.model.steam;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.concurrent.CompletableFuture;

@Getter
@EqualsAndHashCode
public class SteamMessageCallback<T> {
    private int sequence;
    private int messageCode;
    private CompletableFuture<T> callback;

    public SteamMessageCallback(int messageCode, int sequence) {
        this.sequence = sequence;
        this.messageCode = messageCode;
        this.callback = new CompletableFuture<>();
    }
}
