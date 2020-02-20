package com.avenga.steamclient.model.steam.gamecoordinator;

import com.avenga.steamclient.steam.asyncclient.SteamClientAsync;
import com.avenga.steamclient.steam.asyncclient.callbacks.DisconnectedCallback;
import com.avenga.steamclient.steam.asyncclient.steamgamecoordinator.SteamGameCoordinatorAsync;

/**
 * This class implements the base requirements every GC message handler should inherit from.
 */
public abstract class ClientGCMessageHandler {

    protected SteamGameCoordinatorAsync gameCoordinator;

    public void setup(SteamGameCoordinatorAsync gameCoordinator) {
        this.gameCoordinator = gameCoordinator;
    }

    /**
     * Gets whether or not the related {@link SteamClientAsync} should imminently expect the server to close the connection.
     * If this is true when the connection is closed, the {@link DisconnectedCallback}'s
     * {@link DisconnectedCallback#isUserInitiated()} property will be set to <b>true</b>.
     *
     * @return whether or not the related {@link SteamClientAsync} should imminently expect the server to close the connection.
     */
    protected boolean isExpectDisconnection() {
        return gameCoordinator.getClient().isExpectDisconnection();
    }

    /**
     * Sets whether or not the related {@link SteamClientAsync} should imminently expect the server to close the connection.
     * If this is true when the connection is closed, the {@link DisconnectedCallback}'s
     * {@link DisconnectedCallback#isUserInitiated()} property will be set to <b>true</b>.
     *
     * @param expectDisconnection whether or not the related {@link SteamClientAsync} should imminently expect the server to close the connection.
     */
    protected void setExpectDisconnection(boolean expectDisconnection) {
        gameCoordinator.getClient().setExpectDisconnection(expectDisconnection);
    }

    /**
     * @return the underlying {@link SteamClientAsync} for use in sending replies.
     */
    public SteamClientAsync getClient() {
        return gameCoordinator.getClient();
    }

    /**
     * @return the underlying {@link SteamGameCoordinatorAsync} for use in sending replies.
     */
    public SteamGameCoordinatorAsync getGameCoordinator() {
        return gameCoordinator;
    }

    /**
     * Handles a Game Coordinator client message. This should not be called directly.
     *
     * @param gcMessage The packet message that contains the data.
     */
    public abstract void handleMessage(GCMessage gcMessage);

    /**
     * Gets Steam application id.
     */
    public abstract int getApplicationId();
}
