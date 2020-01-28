package com.avenga.steamclient.provider;

import com.avenga.steamclient.model.discovery.ServerRecord;

import java.util.List;

/**
 * An interface for persisting the server list for connection discovery
 */
public interface ServerListProvider {

    /**
     * Ask a provider to fetch any servers that it has available
     *
     * @return A list of IPEndPoints representing servers
     */
    List<ServerRecord> fetchServerList();

    /**
     * Update the persistent list of endpoints
     *
     * @param endpoints List of endpoints
     */
    void updateServerList(List<ServerRecord> endpoints);
}
