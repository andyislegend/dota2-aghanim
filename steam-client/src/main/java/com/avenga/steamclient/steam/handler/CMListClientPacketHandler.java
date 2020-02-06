package com.avenga.steamclient.steam.handler;

import com.avenga.steamclient.base.ClientMessageProtobuf;
import com.avenga.steamclient.base.PacketMessage;
import com.avenga.steamclient.model.discovery.ServerRecord;
import com.avenga.steamclient.protobufs.steamclient.SteammessagesClientserver.CMsgClientCMList;
import com.avenga.steamclient.steam.CMClient;
import com.avenga.steamclient.util.NetworkUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public class CMListClientPacketHandler implements ClientPacketHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CMListClientPacketHandler.class);

    @Override
    public void handle(PacketMessage packetMessage, CMClient cmClient) {
        ClientMessageProtobuf<CMsgClientCMList.Builder> cmMsg = new ClientMessageProtobuf<>(CMsgClientCMList.class, packetMessage);

        if (cmMsg.getBody().getCmPortsCount() != cmMsg.getBody().getCmAddressesCount()) {
            LOGGER.debug("HandleCMList received malformed message");
        }

        List<Integer> addresses = cmMsg.getBody().getCmAddressesList();
        List<Integer> ports = cmMsg.getBody().getCmPortsList();

        List<ServerRecord> cmList = new ArrayList<>();
        for (int i = 0; i < Math.min(addresses.size(), ports.size()); i++) {
            cmList.add(new ServerRecord(new InetSocketAddress(NetworkUtils.getIPAddress(addresses.get(i)), ports.get(i))));
        }

        for (String webSocketAddress : cmMsg.getBody().getCmWebsocketAddressesList()) {
            cmList.add(new ServerRecord(webSocketAddress));
        }

        cmClient.getServers().replaceList(cmList);
    }
}
