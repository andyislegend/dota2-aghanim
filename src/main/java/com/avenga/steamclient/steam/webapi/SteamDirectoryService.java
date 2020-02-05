package com.avenga.steamclient.steam.webapi;

import com.avenga.steamclient.enums.EResult;
import com.avenga.steamclient.model.configuration.SteamConfiguration;
import com.avenga.steamclient.model.discovery.ServerRecord;
import com.avenga.steamclient.model.webapi.CMListResponse;
import coresearch.cvurl.io.request.CVurl;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * Helper class to load servers from the Steam Directory Web API.
 */
public class SteamDirectoryService {

    private static final String CM_LIST_URL_PATH = "/ISteamDirectory/GetCMList/v1";

    private final static CVurl HTTP_CLIENT = new CVurl();

    /**
     * Load a list of servers from the Steam Directory.
     *
     * @param configuration Configuration Object
     * @return the list of servers
     * @throws IOException if the request could not be executed
     */
    public static List<ServerRecord> getServers(SteamConfiguration configuration) throws IOException {
        return getServers(configuration, -1);
    }

    /**
     * Load a list of servers from the Steam Directory.
     *
     * @param configuration Configuration Object
     * @param maxServers    Max number of servers to return. The API will typically return this number per server type
     *                      (socket and websocket). If negative, the parameter is not added to the request
     * @return the list of servers
     * @throws IOException if the request could not be executed
     */
    public static List<ServerRecord> getServers(SteamConfiguration configuration, int maxServers) throws IOException {
        Objects.requireNonNull(configuration, "Steam configuration wasn't provided");

        Map<String, String> parameters = new HashMap<>();
        parameters.put("cellid", String.valueOf(configuration.getCellID()));

        if (maxServers >= 0) {
            parameters.put("maxcount", String.valueOf(maxServers));
        }

        CMListResponse response = getCMList(configuration, parameters);

        EResult result = EResult.from(response.getResponse().getResult());

        if (result != EResult.OK) {
            throw new IllegalStateException("Steam Web API returned EResult." + result);
        }

        List<ServerRecord> records = new ArrayList<>();

        response.getResponse().getSocketServerList().forEach(socket -> {
            String[] split = socket.split(":");
            records.add(new ServerRecord(new InetSocketAddress(split[0], Integer.parseInt(split[1]))));
        });

        response.getResponse().getWebsocketServerList().forEach(webSocket -> records.add(new ServerRecord(webSocket)));

        return records;
    }

    private static CMListResponse getCMList(SteamConfiguration configuration, Map<String, String> parameters) throws MalformedURLException {
        if (configuration.getWebAPIKey() != null) {
            parameters.put("key", configuration.getWebAPIKey());
        }

        URL baseUrl = new URL(configuration.getWebAPIBaseAddress());
        URL steamDirectoryUrl = new URL(baseUrl, CM_LIST_URL_PATH);

        return HTTP_CLIENT.get(steamDirectoryUrl)
                .queryParams(parameters)
                .asObject(CMListResponse.class);
    }
}
