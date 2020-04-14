package com.avenga.steamclient.model.configuration;

import com.avenga.steamclient.enums.EClientPersonaStateFlag;
import com.avenga.steamclient.enums.EUniverse;
import com.avenga.steamclient.enums.ProtocolType;
import com.avenga.steamclient.provider.ServerListProvider;
import com.avenga.steamclient.provider.SmartCMServerProvider;
import com.avenga.steamclient.steam.client.SteamClient;

import java.util.EnumSet;

/**
 * Configuration object to use.
 * This object should not be mutated after it is passed to one or more {@link SteamClient} objects.
 */
public class SteamConfiguration {

    private final SteamConfigurationState state;
    private SmartCMServerProvider serverProvider;

    public SteamConfiguration(SteamConfigurationState state) {
        this.state = state;
        this.serverProvider = new SmartCMServerProvider(this);
    }

    public SteamConfiguration() {
        this(SteamConfigurationState.buildDefaultState());
    }

    /**
     * @return Whether or not to use the Steam Directory to discover available servers.
     */
    public boolean isAllowDirectoryFetch() {
        return state.isAllowDirectoryFetch();
    }

    /**
     * @return The Steam Cell ID to prioritize when connecting.
     */
    public int getCellID() {
        return state.getCellID();
    }

    /**
     * @return The connection timeout used when connecting to Steam serves.
     */
    public long getConnectionTimeout() {
        return state.getConnectionTimeout();
    }

    /**
     * @return The default persona state flags used when requesting information for a new friend, or when calling <b>SteamFriends.RequestFriendInfo</b> without specifying flags.
     */
    public EnumSet<EClientPersonaStateFlag> getDefaultPersonaStateFlags() {
        return state.getDefaultPersonaStateFlags();
    }

    /**
     * @return The supported protocol types to use when attempting to connect to Steam.
     */
    public EnumSet<ProtocolType> getProtocolTypes() {
        return state.getProtocolTypes();
    }

    /**
     * @return The server list provider to use.
     */
    public ServerListProvider getServerListProvider() {
        return state.getServerListProvider();
    }

    /**
     * @return The Universe to connect to. This should always be {@link EUniverse#Public} unless you work at Valve and are using this internally. If this is you, hello there.
     */
    public EUniverse getUniverse() {
        return state.getUniverse();
    }

    /**
     * @return The base address of the Steam Web API to connect to. Use of "partner.steam-api.com" requires a Partner API key.
     */
    public String getWebAPIBaseAddress() {
        return state.getWebAPIBaseAddress();
    }

    /**
     * @return An API key to be used for authorized requests. Keys can be obtained from https://steamcommunity.com/dev or the Steamworks Partner site.
     */
    public String getWebAPIKey() {
        return state.getWebAPIKey();
    }

    /**
     * @return The server list used for this configuration. If this configuration is used by multiple {@link SteamClient} instances, they all share the server list.
     */
    public SmartCMServerProvider getServerProvider() {
        return serverProvider;
    }
}
