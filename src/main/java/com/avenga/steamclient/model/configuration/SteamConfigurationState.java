package com.avenga.steamclient.model.configuration;

import com.avenga.steamclient.constant.Constant;
import com.avenga.steamclient.enums.EClientPersonaStateFlag;
import com.avenga.steamclient.enums.EUniverse;
import com.avenga.steamclient.enums.ProtocolType;
import com.avenga.steamclient.provider.NullServerListProvider;
import com.avenga.steamclient.provider.ServerListProvider;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.EnumSet;

@Getter
@Setter
@Builder
public class SteamConfigurationState {

    private boolean allowDirectoryFetch;
    private int cellID;
    private long connectionTimeout;
    private EnumSet<EClientPersonaStateFlag> defaultPersonaStateFlags;
    private EnumSet<ProtocolType> protocolTypes;
    private ServerListProvider serverListProvider;
    private EUniverse universe;
    private String webAPIBaseAddress;
    private String webAPIKey;

    public static SteamConfigurationState buildDefaultState() {
        return SteamConfigurationState.builder()
                .allowDirectoryFetch(true)
                .connectionTimeout(5000L)
                .defaultPersonaStateFlags(EnumSet.of(EClientPersonaStateFlag.PlayerName, EClientPersonaStateFlag.Presence,
                        EClientPersonaStateFlag.SourceID, EClientPersonaStateFlag.GameExtraInfo, EClientPersonaStateFlag.LastSeen))
                .protocolTypes(EnumSet.of(ProtocolType.TCP))
                .serverListProvider(new NullServerListProvider())
                .universe(EUniverse.Public)
                .webAPIBaseAddress(Constant.WEB_API_BASE_ADDRESS)
                .build();
    }
}
