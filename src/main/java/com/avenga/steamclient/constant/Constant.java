package com.avenga.steamclient.constant;

public class Constant {

    public static final String WEB_API_BASE_ADDRESS = "https://api.steampowered.com/";

    public static final String UTILITY_CLASS_INIT_ERROR = "Instance of this class can't be initialized!";

    private Constant() {
        throw new IllegalStateException(UTILITY_CLASS_INIT_ERROR);
    }
}
