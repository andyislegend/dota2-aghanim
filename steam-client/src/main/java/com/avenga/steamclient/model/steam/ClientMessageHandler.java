package com.avenga.steamclient.model.steam;

import com.avenga.steamclient.base.PacketMessage;
import com.avenga.steamclient.steam.asyncclient.SteamClientAsync;
import com.avenga.steamclient.steam.asyncclient.callbacks.DisconnectedCallback;

/**
 * This class implements the base requirements every message handler should inherit from.
 */
public abstract class ClientMessageHandler {

    protected SteamClientAsync client;

    public void setup(SteamClientAsync client) {
        this.client = client;
    }

    /**
     * Gets whether or not the related {@link SteamClientAsync} should imminently expect the server to close the connection.
     * If this is true when the connection is closed, the {@link DisconnectedCallback}'s
     * {@link DisconnectedCallback#isUserInitiated()} property will be set to <b>true</b>.
     *
     * @return whether or not the related {@link SteamClientAsync} should imminently expect the server to close the connection.
     */
    protected boolean isExpectDisconnection() {
        return client.isExpectDisconnection();
    }

    /**
     * Sets whether or not the related {@link SteamClientAsync} should imminently expect the server to close the connection.
     * If this is true when the connection is closed, the {@link DisconnectedCallback}'s
     * {@link DisconnectedCallback#isUserInitiated()} property will be set to <b>true</b>.
     *
     * @param expectDisconnection whether or not the related {@link SteamClientAsync} should imminently expect the server to close the connection.
     */
    protected void setExpectDisconnection(boolean expectDisconnection) {
        client.setExpectDisconnection(expectDisconnection);
    }

    /**
     * @return the underlying {@link SteamClientAsync} for use in sending replies.
     */
    public SteamClientAsync getClient() {
        return client;
    }

    /**
     * Handles a client message. This should not be called directly.
     *
     * @param packetMessage The packet message that contains the data.
     */
    public abstract void handleMessage(PacketMessage packetMessage);
}
