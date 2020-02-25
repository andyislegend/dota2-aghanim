package com.avenga.steamclient.steam.client.callback;

import com.avenga.steamclient.base.PacketMessage;
import com.avenga.steamclient.constant.Constant;
import com.avenga.steamclient.exception.CallbackTimeoutException;
import com.avenga.steamclient.model.steam.SteamMessageCallback;

public class ConnectedClientCallbackHandler extends AbstractCallbackHandler<PacketMessage> {

    public static final int CALLBACK_MESSAGE_CODE = Constant.CONNECTED_PACKET_CODE;

    public static void handle(SteamMessageCallback<PacketMessage> callback, long timeout) throws CallbackTimeoutException {
        waitAndGetPacketMessage(callback, timeout, "ConnectedClient");
    }
}
