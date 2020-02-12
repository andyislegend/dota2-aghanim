package com.avenga.steamclient.steam.coordinator.callback;

import com.avenga.steamclient.base.ClientGCProtobufMessage;
import com.avenga.steamclient.base.GCPacketMessage;
import com.avenga.steamclient.exception.CallbackTimeoutException;
import com.avenga.steamclient.model.steam.SteamMessageCallback;
import com.avenga.steamclient.protobufs.dota.GCSdkGCMessages.CMsgClientWelcome;
import com.avenga.steamclient.steam.client.callback.AbstractCallbackHandler;

public class GCSessionCallbackHandler extends AbstractCallbackHandler<GCPacketMessage> {

    public static ClientGCProtobufMessage<CMsgClientWelcome.Builder> handle(SteamMessageCallback<GCPacketMessage> callback) {
        GCPacketMessage gcPacketMessage = waitAndGetPacketMessage(callback, "GCSession");

        return getMessage(gcPacketMessage);
    }

    public static ClientGCProtobufMessage<CMsgClientWelcome.Builder> handle(SteamMessageCallback<GCPacketMessage> callback, long timeout) throws CallbackTimeoutException {
        GCPacketMessage gcPacketMessage = waitAndGetPacketMessage(callback, timeout, "GCSession");

        return getMessage(gcPacketMessage);
    }

    private static ClientGCProtobufMessage<CMsgClientWelcome.Builder> getMessage(GCPacketMessage gcPacketMessage) {
        return new ClientGCProtobufMessage<>(CMsgClientWelcome.class, gcPacketMessage);
    }
}
