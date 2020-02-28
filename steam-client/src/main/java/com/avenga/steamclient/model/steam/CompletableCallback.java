package com.avenga.steamclient.model.steam;

import com.avenga.steamclient.steam.client.SteamClient;

/**
 * Completable callback of the {@link SteamClient} to handle received packet messages from Steam Network or Game Coordinator server.
 */
public interface CompletableCallback {

    int getSequence();

    int getMessageCode();

    int getApplicationId();

    void complete(Object packetMessage);
}
