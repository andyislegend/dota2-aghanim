package com.avenga.steamclient.steam.coordinator.callback;

import com.avenga.steamclient.base.ClientGCProtobufMessage;
import com.avenga.steamclient.base.GCPacketMessage;
import com.avenga.steamclient.exception.CallbackCompletionException;
import com.avenga.steamclient.model.steam.SteamMessageCallback;
import com.avenga.steamclient.protobufs.dota.GCSdkGCMessages.CMsgClientWelcome;

import java.util.concurrent.ExecutionException;

import static com.avenga.steamclient.constant.Constant.CALLBACK_EXCEPTION_MESSAGE_FORMAT;

public class GCSessionCallbackHandler {
    public static ClientGCProtobufMessage<CMsgClientWelcome.Builder> handle(SteamMessageCallback<GCPacketMessage> steamMessageCallback) {
        GCPacketMessage gcPacketMessage;
        try {
            gcPacketMessage = steamMessageCallback.getCallback().get();
        } catch (InterruptedException | ExecutionException e) {
            throw new CallbackCompletionException(String.format(CALLBACK_EXCEPTION_MESSAGE_FORMAT, "GCSession", e.getMessage()), e);
        }
        return new ClientGCProtobufMessage<>(CMsgClientWelcome.class, gcPacketMessage);
    }
}
