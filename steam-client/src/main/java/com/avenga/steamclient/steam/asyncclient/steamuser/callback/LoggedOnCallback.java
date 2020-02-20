package com.avenga.steamclient.steam.asyncclient.steamuser.callback;

import com.avenga.steamclient.enums.EAccountFlags;
import com.avenga.steamclient.enums.EResult;
import com.avenga.steamclient.generated.MsgClientLogOnResponse;
import com.avenga.steamclient.model.SteamID;
import com.avenga.steamclient.protobufs.steamclient.SteammessagesClientserverLogin.CMsgClientLogonResponse;
import com.avenga.steamclient.steam.asyncclient.callbackmanager.BaseCallbackMessage;
import com.avenga.steamclient.steam.asyncclient.steamuser.SteamUserAsync;
import com.avenga.steamclient.util.NetworkUtils;
import lombok.Getter;

import java.net.InetAddress;
import java.util.Date;
import java.util.EnumSet;

/**
 * This callback is returned in response to an attempt to log on to the Steam3 network through {@link SteamUserAsync}.
 */
@Getter
public class LoggedOnCallback extends BaseCallbackMessage {
    private EResult result;
    private EResult extendedResult;
    private int outOfGameSecsPerHeartbeat;
    private int inGameSecsPerHeartbeat;
    private InetAddress publicIP;
    private Date serverTime;
    private EnumSet<EAccountFlags> accountFlags;
    private SteamID clientSteamID;
    private String emailDomain;
    private int cellID;
    private int cellIDPingThreshold;
    private byte[] steam2Ticket;
    private boolean usePICS;
    private String webAPIUserNonce;
    private String ipCountryCode;
    private String vanityURL;
    private int numLoginFailuresToMigrate;
    private int numDisconnectsToMigrate;

    public LoggedOnCallback(CMsgClientLogonResponse.Builder response) {
        result = EResult.from(response.getEresult());
        extendedResult = EResult.from(response.getEresultExtended());
        outOfGameSecsPerHeartbeat = response.getOutOfGameHeartbeatSeconds();
        inGameSecsPerHeartbeat = response.getInGameHeartbeatSeconds();
        publicIP = NetworkUtils.getIPAddress(response.getPublicIp());
        serverTime = new Date(response.getRtime32ServerTime() * 1000L);
        accountFlags = EAccountFlags.from(response.getAccountFlags());
        clientSteamID = new SteamID(response.getClientSuppliedSteamid());
        emailDomain = response.getEmailDomain();
        cellID = response.getCellId();
        cellIDPingThreshold = response.getCellIdPingThreshold();
        steam2Ticket = response.getSteam2Ticket().toByteArray();
        ipCountryCode = response.getIpCountryCode();
        webAPIUserNonce = response.getWebapiAuthenticateUserNonce();
        usePICS = response.getUsePics();
        vanityURL = response.getVanityUrl();
        numLoginFailuresToMigrate = response.getCountLoginfailuresToMigrate();
        numDisconnectsToMigrate = response.getCountDisconnectsToMigrate();
    }

    public LoggedOnCallback(MsgClientLogOnResponse response) {
        result = response.getResult();
        outOfGameSecsPerHeartbeat = response.getOutOfGameHeartbeatRateSec();
        inGameSecsPerHeartbeat = response.getInGameHeartbeatRateSec();
        publicIP = NetworkUtils.getIPAddress(response.getIpPublic());
        serverTime = new Date(response.getServerRealTime() * 1000L);
        clientSteamID = response.getClientSuppliedSteamId();
    }

    public LoggedOnCallback(EResult result) {
        this.result = result;
    }
}
