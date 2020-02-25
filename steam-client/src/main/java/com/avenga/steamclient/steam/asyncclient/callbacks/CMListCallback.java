package com.avenga.steamclient.steam.asyncclient.callbacks;

import com.avenga.steamclient.model.discovery.ServerRecord;
import com.avenga.steamclient.protobufs.steamclient.SteammessagesClientserver.CMsgClientCMList;
import com.avenga.steamclient.steam.asyncclient.callbackmanager.BaseCallbackMessage;
import com.avenga.steamclient.util.NetworkUtils;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * This callback is received when the client has received the CM list from Steam.
 */
public class CMListCallback extends BaseCallbackMessage {

    private final Collection<ServerRecord> servers;

    public CMListCallback(CMsgClientCMList.Builder cmMessage) {
        List<Integer> addresses = cmMessage.getCmAddressesList();
        List<Integer> ports = cmMessage.getCmPortsList();

        List<ServerRecord> cmList = new ArrayList<>();
        for (int i = 0; i < Math.min(addresses.size(), ports.size()); i++) {
            cmList.add(new ServerRecord(new InetSocketAddress(NetworkUtils.getIPAddress(addresses.get(i)), ports.get(i))));
        }

        cmMessage.getCmWebsocketAddressesList().forEach(webSocket -> cmList.add(new ServerRecord(webSocket)));
        servers = Collections.unmodifiableCollection(cmList);
    }

    /**
     * @return the CM server list.
     */
    public Collection<ServerRecord> getServers() {
        return servers;
    }
}
