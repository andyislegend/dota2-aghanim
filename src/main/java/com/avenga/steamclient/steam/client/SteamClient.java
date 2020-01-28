package com.avenga.steamclient.steam.client;

import com.avenga.steamclient.base.PacketMessage;
import com.avenga.steamclient.model.configuration.SteamConfiguration;
import com.avenga.steamclient.steam.CMClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SteamClient extends CMClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(SteamClient.class);

    /**
     * Initializes a new instance of the {@link SteamClient} class with the default configuration.
     */
    public SteamClient() {
        this(new SteamConfiguration());
    }

    /**
     * Initializes a new instance of the {@link SteamClient} class with a specific configuration.
     *
     * @param configuration The configuration to use for this client.
     */
    public SteamClient(SteamConfiguration configuration) {
        super(configuration);
    }

    @Override
    public boolean onClientMsgReceived(PacketMessage packetMsg) {
        return super.onClientMsgReceived(packetMsg);
    }

    @Override
    protected void onClientConnected() {
        super.onClientConnected();
    }

    @Override
    protected void onClientDisconnected(boolean userInitiated) {
        super.onClientDisconnected(userInitiated);
    }
}
