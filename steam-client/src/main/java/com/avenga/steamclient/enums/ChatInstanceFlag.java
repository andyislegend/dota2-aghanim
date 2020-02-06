package com.avenga.steamclient.enums;

import com.avenga.steamclient.model.SteamID;

import java.util.Arrays;

/**
 * Represents various flags a chat {@link SteamID} may have, packed into its instance.
 */
public enum ChatInstanceFlag {

    /**
     * This flag is set for Unknown chat.
     */
    UNKNOWN(Long.MIN_VALUE),

    /**
     * This flag is set for clan based chat {@link SteamID SteamIDs}.
     */
    CLAN((SteamID.ACCOUNT_INSTANCE_MASK + 1) >> 1),

    /**
     * This flag is set for lobby based chat {@link SteamID SteamIDs}.
     */
    LOBBY((SteamID.ACCOUNT_INSTANCE_MASK + 1) >> 2),

    /**
     * This flag is set for matchmaking lobby based chat {@link SteamID SteamIDs}.
     */
    MMS_LOBBY((SteamID.ACCOUNT_INSTANCE_MASK + 1) >> 3);

    private final long code;

    ChatInstanceFlag(long code) {
        this.code = code;
    }

    public long code() {
        return this.code;
    }

    public static ChatInstanceFlag from(long code) {
        return Arrays.stream(ChatInstanceFlag.values())
                .filter(flag -> flag.code == code)
                .findFirst().orElse(UNKNOWN);
    }

}
