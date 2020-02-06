package com.avenga.steamclient.steam.handler;

import com.avenga.steamclient.base.ClientMessageProtobuf;
import com.avenga.steamclient.base.PacketMessage;
import com.avenga.steamclient.protobufs.steamclient.SteammessagesClientserver.CMsgClientSessionToken;
import com.avenga.steamclient.steam.CMClient;

public class SessionTokenClientPacketHandler implements ClientPacketHandler {

    @Override
    public void handle(PacketMessage packetMessage, CMClient cmClient) {
        ClientMessageProtobuf<CMsgClientSessionToken.Builder> sessToken = new ClientMessageProtobuf<>(CMsgClientSessionToken.class, packetMessage);
        cmClient.setSessionToken(sessToken.getBody().getToken());
    }
}
