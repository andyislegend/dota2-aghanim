package com.avenga.steamclient.steam.steamuser;

import com.avenga.steamclient.enums.EOSType;
import com.avenga.steamclient.model.SteamID;
import com.avenga.steamclient.util.Utils;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents the details required to log into Steam3 as a user.
 */
@Getter
@Setter
public class LogOnDetails {

    private String username = "";

    private String password = "";

    private int cellID;

    private Integer loginID;

    private String authCode = "";

    private String twoFactorCode = "";

    private String loginKey = "";

    private boolean shouldRememberPassword;

    private byte[] sentryFileHash;

    private long accountInstance;

    private long accountID;

    private boolean requestSteam2Ticket;

    private EOSType clientOSType;

    private String clientLanguage = "";

    public LogOnDetails() {
        accountInstance = SteamID.DESKTOP_INSTANCE;
        accountID = 0L;

        clientOSType = Utils.getOSType();
        clientLanguage = "english";
    }
}
