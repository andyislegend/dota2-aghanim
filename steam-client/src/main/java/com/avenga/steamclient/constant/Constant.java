package com.avenga.steamclient.constant;

public class Constant {

    public static final String WEB_API_BASE_ADDRESS = "https://api.steampowered.com/";

    public static final int MAX_PLAYED_GAMES = 32;

    public static final int CONNECTED_PACKET_CODE = -1000;
    public static final int DISCONNECTED_PACKET_CODE = -1001;

    public static final String DEFAULT_CLIENT_NAME = "CMClient";

    public static final String CALLBACK_EXCEPTION_MESSAGE_FORMAT = "{}: Exception during handling {} callback with message: {}";
    public static final String TIMEOUT_EXCEPTION_MESSAGE_FORMAT = "Timeout was reached during handling %s callback with queue sequence: %d";
    public static final String RETRY_EXCEPTION_MESSAGE_FORMAT = "Message wasn't received after %d retries with message: %s";
    public static final String UTILITY_CLASS_INIT_ERROR = "Instance of this class can't be initialized!";

    private Constant() {
        throw new IllegalStateException(UTILITY_CLASS_INIT_ERROR);
    }
}
