package com.avenga.steamclient.types;

import com.avenga.steamclient.util.Utils;

import java.util.Objects;

/**
 * This 64bit structure represents an app, mod, shortcut, or p2p file on the Steam network.
 */
public class GameID {

    private final static int DEFAULT_APPLICATION_ID = 0;
    private final static int APPLICATION_BIT_OFFSET = 0;
    private final static long APPLICATION_BIT_MASK = 0xFFFFFFL;
    private final static int APPLICATION_TYPE_BIT_OFFSET = 24;
    private final static long APPLICATION_TYPE_BIT_MASK = 0xFFL;
    private final static int MOD_BIT_OFFSET = 32;
    private final static int MOD_APPLICATION_TYPE_BIT_OFFSET = 63;
    private final static long ACCOUNT_ID_MASK = 0xFFFFFFFFL;
    private final static long DEFAULT_MOD_MASK_VALUE = 1L;

    private BitVector64 gameId;

    /**
     * Initializes a new instance of the {@link GameID} class.
     */
    public GameID() {
        this(DEFAULT_APPLICATION_ID);
    }

    /**
     * Initializes a new instance of the {@link GameID} class.
     *
     * @param id The 64bit integer to assign this GameID from.
     */
    public GameID(long id) {
        gameId = new BitVector64(id);
    }

    /**
     * Initializes a new instance of the {@link GameID} class.
     *
     * @param applicationId The 32bit app id to assign this GameID from.
     */
    public GameID(int applicationId) {
        this((long) applicationId);
    }

    /**
     * Initializes a new instance of the {@link GameID} class.
     *
     * @param applicationId  The base app id of the mod.
     * @param modPath The game folder name of the mod.
     */
    public GameID(int applicationId, String modPath) {
        this(DEFAULT_APPLICATION_ID);
        setAppID(applicationId);
        setAppType(GameType.GAME_MOD);
        setModID(Utils.crc32(modPath));
    }

    /**
     * Initializes a new instance of the {@link GameID} class.
     *
     * @param exePath The path to the executable, usually quoted.
     * @param applicationName The name of the application shortcut.
     */
    public GameID(String exePath, String applicationName) {
        this(DEFAULT_APPLICATION_ID);

        StringBuilder builder = new StringBuilder();
        if (exePath != null) {
            builder.append(exePath);
        }
        if (applicationName != null) {
            builder.append(applicationName);
        }

        setAppID(0);
        setAppType(GameType.SHORTCUT);
        setModID(Utils.crc32(builder.toString()));
    }

    /**
     * Sets the various components of this GameID from a 64bit integer form.
     *
     * @param gameId The 64bit integer to assign this GameID from.
     */
    public void set(long gameId) {
        this.gameId.setData(gameId);
    }

    /**
     * Converts this GameID into it's 64bit integer form.
     *
     * @return A 64bit integer representing this GameID.
     */
    public long toUInt64() {
        return gameId.getData();
    }

    /**
     * Sets the app id.
     *
     * @param value The app ID.
     */
    public void setAppID(int value) {
        gameId.setMask((short) APPLICATION_BIT_OFFSET, APPLICATION_BIT_MASK, value);
    }

    /**
     * Gets the app id.
     *
     * @return The app ID.
     */
    public int getAppID() {
        return (int) gameId.getMask((short) APPLICATION_BIT_OFFSET, APPLICATION_BIT_MASK);
    }

    /**
     * Sets the type of the app.
     *
     * @param value The type of the app.
     */
    public void setAppType(GameType value) {
        gameId.setMask((short) APPLICATION_TYPE_BIT_OFFSET, APPLICATION_TYPE_BIT_MASK, value.code());
    }

    /**
     * Gets the type of the app.
     *
     * @return The type of the app.
     */
    public GameType getAppType() {
        return GameType.from((int) gameId.getMask((short) APPLICATION_TYPE_BIT_OFFSET, APPLICATION_TYPE_BIT_MASK));
    }

    /**
     * Sets the mod id.
     *
     * @param value The mod ID.
     */
    public void setModID(long value) {
        gameId.setMask((short) MOD_BIT_OFFSET, ACCOUNT_ID_MASK, value);
        gameId.setMask((short) MOD_APPLICATION_TYPE_BIT_OFFSET, APPLICATION_TYPE_BIT_MASK, DEFAULT_MOD_MASK_VALUE);
    }

    /**
     * Gets the mod id.
     *
     * @return The mod ID.
     */
    public long getModID() {
        return gameId.getMask((short) MOD_BIT_OFFSET, ACCOUNT_ID_MASK);
    }

    /**
     * Gets a value indicating whether this instance is a mod.
     *
     * @return <b>true</b> if this instance is a mod; otherwise, <b>false</b>.
     */
    public boolean isMod() {
        return getAppType() == GameType.GAME_MOD;
    }

    /**
     * Gets a value indicating whether this instance is a shortcut.
     *
     * @return <b>true</b> if this instance is a shortcut; otherwise, <b>false</b>.
     */
    public boolean isShortcut() {
        return getAppType() == GameType.SHORTCUT;
    }

    /**
     * Gets a value indicating whether this instance is a peer-to-peer file.
     *
     * @return <b>true</b> if this instance is a p2p file; otherwise, <b>false</b>.
     */
    public boolean isP2PFile() {
        return getAppType() == GameType.P2P;
    }

    /**
     * Gets a value indicating whether this instance is a steam app.
     *
     * @return <b>true</b> if this instance is a steam app; otherwise, <b>false</b>.
     */
    public boolean isSteamApp() {
        return getAppType() == GameType.APP;
    }

    /**
     * Sets the various components of this GameID from a 64bit integer form.
     *
     * @param longSteamId The 64bit integer to assign this GameID from.
     */
    public void setFromUInt64(long longSteamId) {
        this.gameId.setData(longSteamId);
    }

    /**
     * Converts this GameID into it's 64bit integer form.
     *
     * @return A 64bit integer representing this GameID.
     */
    public long convertToUInt64() {
        return this.gameId.getData();
    }

    /**
     * Determines whether the specified {@link Object} is equal to this instance.
     *
     * @param obj The {@link Object} to compare with this instance.
     * @return <b>true</b> if the specified {@link Object} is equal to this instance; otherwise, <b>false</b>.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (!(obj instanceof GameID)) {
            return false;
        }

        return Objects.equals(gameId.getData(), ((GameID) obj).gameId.getData());
    }

    /**
     * Returns a hash code for this instance.
     *
     * @return A hash code for this instance, suitable for use in hashing algorithms and data structures like a hash table.
     */
    @Override
    public int hashCode() {
        return gameId.hashCode();
    }

    /**
     * Returns a {@link String} that represents this instance.
     *
     * @return A {@link String} that represents this instance.
     */
    @Override
    public String toString() {
        return String.valueOf(toUInt64());
    }

    /**
     * Represents various types of games.
     */
    public enum GameType {

        /**
         * A Steam application.
         */
        APP(0),

        /**
         * A game modification.
         */
        GAME_MOD(1),

        /**
         * A shortcut to a program.
         */
        SHORTCUT(2),

        /**
         * A peer-to-peer file.
         */
        P2P(3);

        private final int code;

        GameType(int code) {
            this.code = code;
        }

        public int code() {
            return code;
        }

        public static GameType from(int code) {
            for (GameType e : GameType.values()) {
                if (e.code == code) {
                    return e;
                }
            }
            return null;
        }
    }
}
