package org.avenga.constant;

public class Constant {

    public static final String STEAMD_INPUT_FILE = "steammsg.steamd";

    public static final String UTILITY_CLASS_INIT_ERROR = "Instance of this class can't be initialized!";

    private Constant() {
        throw new IllegalStateException(UTILITY_CLASS_INIT_ERROR);
    }
}
