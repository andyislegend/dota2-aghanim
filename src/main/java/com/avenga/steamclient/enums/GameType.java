package com.avenga.steamclient.enums;

/**
 * Represents various types of games.
 */
public enum GameType {

    /**
     * Unknown type of the game.
     */
    UNKNOWN(Integer.MIN_VALUE),

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
        return GameType.UNKNOWN;
    }
}
