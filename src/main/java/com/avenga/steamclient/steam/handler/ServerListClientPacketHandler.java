package com.avenga.steamclient.steam.handler;

import com.avenga.steamclient.base.ClientMessageProtobuf;
import com.avenga.steamclient.base.PacketMessage;
import com.avenga.steamclient.enums.EServerType;
import com.avenga.steamclient.protobufs.steamclient.SteammessagesClientserver.CMsgClientServerList;
import com.avenga.steamclient.steam.CMClient;
import com.avenga.steamclient.util.NetworkUtils;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;

public class ServerListClientPacketHandler implements ClientPacketHandler {

    @Override
    public void handle(PacketMessage packetMessage, CMClient cmClient) {

        ClientMessageProtobuf<CMsgClientServerList.Builder> listMsg = new ClientMessageProtobuf<>(CMsgClientServerList.class, packetMessage);

        for (CMsgClientServerList.Server server : listMsg.getBody().getServersList()) {
            EServerType type = EServerType.from(server.getServerType());

            Set<InetSocketAddress> endPointSet = cmClient.getServerMap().computeIfAbsent(type, k -> new HashSet<>());

            endPointSet.add(new InetSocketAddress(NetworkUtils.getIPAddress(server.getServerIp()), server.getServerPort()));
        }
    }
}
