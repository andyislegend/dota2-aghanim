package com.avenga.steamclient.steam.coordinator.callback;

import com.avenga.steamclient.base.ClientGCProtobufMessage;
import com.avenga.steamclient.base.GCPacketMessage;
import com.avenga.steamclient.exception.CallbackTimeoutException;
import com.avenga.steamclient.model.steam.SteamMessageCallback;
import com.avenga.steamclient.protobufs.dota.GCSdkGCMessages.CMsgClientWelcome;
import com.avenga.steamclient.steam.client.callback.AbstractCallbackHandler;

public class GCSessionCallbackHandler extends AbstractCallbackHandler<GCPacketMessage> {

    public static CMsgClientWelcome handle(SteamMessageCallback<GCPacketMessage> callback, long timeout) throws CallbackTimeoutException {
        GCPacketMessage gcPacketMessage = waitAndGetPacketMessage(callback, timeout, "GCSession");

        return getMessage(gcPacketMessage);
    }

    public static CMsgClientWelcome getMessage(GCPacketMessage gcPacketMessage) {
        ClientGCProtobufMessage<CMsgClientWelcome.Builder> clientWelcome = new ClientGCProtobufMessage<>(CMsgClientWelcome.class, gcPacketMessage);
        return clientWelcome.getBody().build();
    }
}
