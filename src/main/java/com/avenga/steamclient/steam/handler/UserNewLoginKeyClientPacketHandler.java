package com.avenga.steamclient.steam.handler;

import com.avenga.steamclient.base.ClientMessageProtobuf;
import com.avenga.steamclient.base.PacketMessage;
import com.avenga.steamclient.enums.EMsg;
import com.avenga.steamclient.protobufs.steamclient.SteammessagesClientserverLogin.CMsgClientNewLoginKey;
import com.avenga.steamclient.protobufs.steamclient.SteammessagesClientserverLogin.CMsgClientNewLoginKeyAccepted;
import com.avenga.steamclient.steam.CMClient;

public class UserNewLoginKeyClientPacketHandler implements ClientPacketHandler {

    @Override
    public void handle(PacketMessage packetMessage, CMClient cmClient) {
        ClientMessageProtobuf<CMsgClientNewLoginKey.Builder> loginKey = new ClientMessageProtobuf<>(CMsgClientNewLoginKey.class, packetMessage);

        ClientMessageProtobuf<CMsgClientNewLoginKeyAccepted.Builder> acceptance = new ClientMessageProtobuf<>(CMsgClientNewLoginKeyAccepted.class, EMsg.ClientNewLoginKeyAccepted);
        acceptance.getBody().setUniqueId(loginKey.getBody().getUniqueId());

        cmClient.send(acceptance);
    }
}
