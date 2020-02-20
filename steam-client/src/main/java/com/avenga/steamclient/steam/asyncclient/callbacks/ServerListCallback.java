package com.avenga.steamclient.steam.asyncclient.callbacks;

import com.avenga.steamclient.enums.EServerType;
import com.avenga.steamclient.protobufs.steamclient.SteammessagesClientserver.CMsgClientServerList;
import com.avenga.steamclient.steam.asyncclient.callbackmanager.BaseCallbackMessage;
import com.avenga.steamclient.util.NetworkUtils;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * This callback is fired when the client receives a list of all public available Steam3 servers.
 * This callback may be fired multiple times for different server lists.
 */
public class ServerListCallback extends BaseCallbackMessage {

    private final Map<EServerType, Collection<InetSocketAddress>> servers = new HashMap<>();

    public ServerListCallback(CMsgClientServerList.Builder serverList) {
        for (CMsgClientServerList.Server server : serverList.getServersList()) {
            EServerType type = EServerType.from(server.getServerType());

            Collection<InetSocketAddress> addresses = servers.computeIfAbsent(type, k -> new ArrayList<>());

            addresses.add(new InetSocketAddress(
                    NetworkUtils.getIPAddress(server.getServerIp()), server.getServerPort()
            ));
        }
    }

    public Map<EServerType, Collection<InetSocketAddress>> getServers() {
        return servers;
    }
}
