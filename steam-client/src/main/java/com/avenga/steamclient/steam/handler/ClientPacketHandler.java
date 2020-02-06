package com.avenga.steamclient.steam.handler;

import com.avenga.steamclient.base.PacketMessage;
import com.avenga.steamclient.steam.CMClient;

public interface ClientPacketHandler {
    void handle(PacketMessage packetMessage, CMClient cmClient);
}
