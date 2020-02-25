package com.avenga.steamclient.steam.asyncclient.steamuser.callback;

import com.avenga.steamclient.enums.EAccountFlags;
import com.avenga.steamclient.protobufs.steamclient.SteammessagesClientserverLogin.CMsgClientAccountInfo;
import com.avenga.steamclient.steam.asyncclient.callbackmanager.BaseCallbackMessage;
import lombok.Getter;

import java.util.EnumSet;

/**
 * This callback is received when account information is recieved from the network.
 * This generally happens after logon.
 */
@Getter
public class AccountInfoCallback extends BaseCallbackMessage {
    private String personaName;

    private String country;

    private int countAuthedComputers;

    private EnumSet<EAccountFlags> accountFlags;

    private long facebookID;

    private String facebookName;

    public AccountInfoCallback(CMsgClientAccountInfo.Builder message) {
        personaName = message.getPersonaName();
        country = message.getIpCountry();
        countAuthedComputers = message.getCountAuthedComputers();
        accountFlags = EAccountFlags.from(message.getAccountFlags());
        facebookID = message.getFacebookId();
        facebookName = message.getFacebookName();
    }
}
