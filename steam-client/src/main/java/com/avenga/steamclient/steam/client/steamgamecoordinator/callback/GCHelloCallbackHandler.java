package com.avenga.steamclient.steam.client.steamgamecoordinator.callback;

import com.avenga.steamclient.base.ClientGCProtobufMessage;
import com.avenga.steamclient.base.GCPacketMessage;
import com.avenga.steamclient.exception.CallbackTimeoutException;
import com.avenga.steamclient.model.steam.SteamMessageCallback;
import com.avenga.steamclient.protobufs.dota.GCSdkGCMessages.CMsgClientWelcome;
import com.avenga.steamclient.steam.client.callback.AbstractCallbackHandler;
import com.avenga.steamclient.util.CallbackHandlerUtils;

import java.util.Optional;

public class GCHelloCallbackHandler extends AbstractCallbackHandler<GCPacketMessage> {

    public static Optional<CMsgClientWelcome> handle(SteamMessageCallback<GCPacketMessage> callback, long timeout) throws CallbackTimeoutException {
        var gcPacketMessage = waitAndGetPacketMessage(callback, timeout, "GCHello");

        return CallbackHandlerUtils.getValueOrDefault(gcPacketMessage, GCHelloCallbackHandler::getMessage);
    }

    public static CMsgClientWelcome getMessage(GCPacketMessage gcPacketMessage) {
        ClientGCProtobufMessage<CMsgClientWelcome.Builder> clientWelcome = new ClientGCProtobufMessage<>(CMsgClientWelcome.class, gcPacketMessage);
        return clientWelcome.getBody().build();
    }
}
