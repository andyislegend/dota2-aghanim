package com.avenga.steamclient.constant;

import static com.avenga.steamclient.constant.Constant.UTILITY_CLASS_INIT_ERROR;

public class TaskConstant {

    public static final String CONNECT_AND_LOGIN_TASK = "connectAndLogin";
    public static final String DISCONNECT_TASK = "disconnect";

    private TaskConstant() {
        throw new IllegalStateException(UTILITY_CLASS_INIT_ERROR);
    }
}
