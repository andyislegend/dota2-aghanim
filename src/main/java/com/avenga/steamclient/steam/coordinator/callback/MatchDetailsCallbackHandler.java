package com.avenga.steamclient.steam.coordinator.callback;

import com.avenga.steamclient.base.ClientGCProtobufMessage;
import com.avenga.steamclient.base.GCPacketMessage;
import com.avenga.steamclient.exception.CallbackCompletionException;
import com.avenga.steamclient.model.steam.SteamMessageCallback;
import com.avenga.steamclient.protobufs.dota.DotaGCMessagesClient.CMsgGCMatchDetailsResponse;

import java.util.concurrent.ExecutionException;

import static com.avenga.steamclient.constant.Constant.CALLBACK_EXCEPTION_MESSAGE_FORMAT;

public class MatchDetailsCallbackHandler {
    public static ClientGCProtobufMessage<CMsgGCMatchDetailsResponse.Builder> handle(SteamMessageCallback<GCPacketMessage> callback) {
        GCPacketMessage gcPacketMessage;
        try {
            gcPacketMessage = callback.getCallback().get();
        } catch (InterruptedException | ExecutionException e) {
            throw new CallbackCompletionException(String.format(CALLBACK_EXCEPTION_MESSAGE_FORMAT, "MatchDetails", e.getMessage()), e);
        }
        return new ClientGCProtobufMessage<>(CMsgGCMatchDetailsResponse.class, gcPacketMessage);
    }
}
