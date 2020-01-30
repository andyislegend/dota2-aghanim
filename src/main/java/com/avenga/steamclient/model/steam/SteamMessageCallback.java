package com.avenga.steamclient.model.steam;

import com.avenga.steamclient.base.PacketMessage;
import lombok.Getter;

import java.util.concurrent.CompletableFuture;

@Getter
public class SteamMessageCallback {
    private int messageCode;
    private CompletableFuture<PacketMessage> callback;

    public SteamMessageCallback(int messageCode) {
        this.messageCode = messageCode;
        this.callback = new CompletableFuture<>();
    }
}
