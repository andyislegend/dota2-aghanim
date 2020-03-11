package com.avenga.steamclient.model.steam.gamecoordinator;

import com.avenga.steamclient.steam.client.SteamClient;
import com.avenga.steamclient.steam.client.steamgamecoordinator.SteamGameCoordinator;

/**
 * This class implements the base requirements every Steam Game Coordinator service should inherit from.
 */
public class ClientGCHandler {

    protected SteamGameCoordinator gameCoordinator;

    public void setup(SteamGameCoordinator gameCoordinator) {
        this.gameCoordinator = gameCoordinator;
    }

    /**
     * @return the underlying {@link SteamGameCoordinator} for use in sending Game Coordinator messages.
     */
    public SteamGameCoordinator getGameCoordinator() {
        return gameCoordinator;
    }

    /**
     * @return the underlying {@link SteamClient} for use in sending replies.
     */
    public SteamClient getClient() {
        return gameCoordinator.getClient();
    }
}
