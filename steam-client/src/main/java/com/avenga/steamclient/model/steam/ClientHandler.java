package com.avenga.steamclient.model.steam;

import com.avenga.steamclient.steam.asyncclient.SteamClientAsync;
import com.avenga.steamclient.steam.asyncclient.callbacks.DisconnectedCallback;
import com.avenga.steamclient.steam.client.SteamClient;

/**
 * This class implements the base requirements every Steam service should inherit from.
 */
public abstract class ClientHandler {

    protected SteamClient client;

    public void setup(SteamClient client) {
        this.client = client;
    }

    /**
     * Gets whether or not the related {@link SteamClient} should imminently expect the server to close the connection.
     * If this is true when the connection is closed, the {@link DisconnectedCallback}'s
     * {@link DisconnectedCallback#isUserInitiated()} property will be set to <b>true</b>.
     *
     * @return whether or not the related {@link SteamClientAsync} should imminently expect the server to close the connection.
     */
    protected boolean isExpectDisconnection() {
        return client.isExpectDisconnection();
    }

    /**
     * Sets whether or not the related {@link SteamClient} should imminently expect the server to close the connection.
     * If this is true when the connection is closed, the {@link DisconnectedCallback}'s
     * {@link DisconnectedCallback#isUserInitiated()} property will be set to <b>true</b>.
     *
     * @param expectDisconnection whether or not the related {@link SteamClientAsync} should imminently expect the server to close the connection.
     */
    protected void setExpectDisconnection(boolean expectDisconnection) {
        client.setExpectDisconnection(expectDisconnection);
    }

    /**
     * @return the underlying {@link SteamClient} for use in sending replies.
     */
    public SteamClient getClient() {
        return client;
    }
}
