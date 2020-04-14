package com.avenga.steamclient.steam.client.steamuser;

import com.avenga.steamclient.enums.EAccountFlags;
import com.avenga.steamclient.enums.EResult;
import com.avenga.steamclient.generated.MsgClientLogOnResponse;
import com.avenga.steamclient.model.SteamID;
import com.avenga.steamclient.protobufs.steamclient.SteammessagesClientserverLogin;
import com.avenga.steamclient.util.NetworkUtils;
import lombok.Getter;
import lombok.Setter;

import java.net.InetAddress;
import java.time.Instant;
import java.util.EnumSet;

@Getter
@Setter
public class UserLogOnResponse {

    private EResult result;

    private EResult extendedResult;

    private int outOfGameSecsPerHeartbeat;

    private int inGameSecsPerHeartbeat;

    private InetAddress publicIP;

    private Instant serverTime;

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

    public UserLogOnResponse(SteammessagesClientserverLogin.CMsgClientLogonResponse.Builder response) {
        result = EResult.from(response.getEresult());
        extendedResult = EResult.from(response.getEresultExtended());
        outOfGameSecsPerHeartbeat = response.getOutOfGameHeartbeatSeconds();
        inGameSecsPerHeartbeat = response.getInGameHeartbeatSeconds();
        publicIP = NetworkUtils.getIPAddress(response.getDeprecatedPublicIp());
        serverTime = Instant.ofEpochMilli(response.getRtime32ServerTime() * 1000L);
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

    public UserLogOnResponse(MsgClientLogOnResponse response) {
        result = response.getResult();
        outOfGameSecsPerHeartbeat = response.getOutOfGameHeartbeatRateSec();
        inGameSecsPerHeartbeat = response.getInGameHeartbeatRateSec();
        publicIP = NetworkUtils.getIPAddress(response.getIpPublic());
        serverTime = Instant.ofEpochMilli(response.getServerRealTime() * 1000L);
        clientSteamID = response.getClientSuppliedSteamId();
    }

    public UserLogOnResponse(EResult result) {
        this.result = result;
    }
}
