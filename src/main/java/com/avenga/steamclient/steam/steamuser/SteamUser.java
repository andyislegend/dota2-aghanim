package com.avenga.steamclient.steam.steamuser;

import com.avenga.steamclient.base.ClientMessageProtobuf;
import com.avenga.steamclient.enums.EAccountType;
import com.avenga.steamclient.enums.EMsg;
import com.avenga.steamclient.enums.EResult;
import com.avenga.steamclient.generated.MsgClientLogon;
import com.avenga.steamclient.model.SteamID;
import com.avenga.steamclient.protobufs.steamclient.SteammessagesClientserverLogin;
import com.avenga.steamclient.protobufs.steamclient.SteammessagesClientserverLogin.CMsgClientLogOff;
import com.avenga.steamclient.protobufs.steamclient.SteammessagesClientserverLogin.CMsgClientLogon;
import com.avenga.steamclient.steam.client.SteamClient;
import com.avenga.steamclient.util.HardwareUtils;
import com.avenga.steamclient.util.NetworkUtils;
import com.avenga.steamclient.util.StringUtils;
import com.google.protobuf.ByteString;

public class SteamUser {

    private SteamClient client;

    public SteamUser(SteamClient client) {
        this.client = client;
    }

    /**
     * Logs the client into the Steam3 network.
     * The client should already have been connected at this point.
     *
     * @param details The details to use for logging on.
     */
    public void logOn(LogOnDetails details) {
        if (details == null) {
            throw new IllegalArgumentException("details is null");
        }

        if (StringUtils.isNullOrEmpty(details.getUsername()) || StringUtils.isNullOrEmpty(details.getPassword())
                && StringUtils.isNullOrEmpty(details.getLoginKey())) {
            throw new IllegalArgumentException("LogOn requires a username and password to be set in 'details'.");
        }

        if (!StringUtils.isNullOrEmpty(details.getLoginKey()) && !details.isShouldRememberPassword()) {
            // Prevent consumers from screwing this up.
            // If should_remember_password is false, the login_key is ignored server-side.
            // The inverse is not applicable (you can log in with should_remember_password and no login_key).
            throw new IllegalArgumentException("ShouldRememberPassword is required to be set to true in order to use LoginKey.");
        }

        ClientMessageProtobuf<CMsgClientLogon.Builder> logon = new ClientMessageProtobuf<>(CMsgClientLogon.class, EMsg.ClientLogon);

        SteamID steamID = new SteamID(details.getAccountID(), details.getAccountInstance(), client.getUniverse(), EAccountType.Individual);

        if (details.getLoginID() != null) {
            logon.getBody().setObfustucatedPrivateIp(details.getLoginID());
        } else {
            int localIp = NetworkUtils.getIPAddress(client.getLocalIP());
            logon.getBody().setObfustucatedPrivateIp(localIp ^ MsgClientLogon.ObfuscationMask);
        }

        logon.getProtoHeader().setClientSessionid(0);
        logon.getProtoHeader().setSteamid(steamID.convertToUInt64());

        logon.getBody().setAccountName(details.getUsername());
        if (!StringUtils.isNullOrEmpty(details.getPassword())) {
            logon.getBody().setPassword(details.getPassword());
        }
        logon.getBody().setShouldRememberPassword(details.isShouldRememberPassword());

        logon.getBody().setProtocolVersion(MsgClientLogon.CurrentProtocol);
        logon.getBody().setClientOsType(details.getClientOSType().code());
        logon.getBody().setClientLanguage(details.getClientLanguage());
        logon.getBody().setCellId(details.getCellID());

        logon.getBody().setSteam2TicketRequest(details.isRequestSteam2Ticket());

        // we're now using the latest steamclient package version, this is required to get a proper sentry file for steam guard
        logon.getBody().setClientPackageVersion(1771); // todo: determine if this is still required
        logon.getBody().setSupportsRateLimitResponse(true);
        logon.getBody().setMachineId(ByteString.copyFrom(HardwareUtils.getMachineID()));

        // steam guard
        if (!StringUtils.isNullOrEmpty(details.getAuthCode())) {
            logon.getBody().setAuthCode(details.getAuthCode());
        }

        if (!StringUtils.isNullOrEmpty(details.getTwoFactorCode())) {
            logon.getBody().setTwoFactorCode(details.getTwoFactorCode());
        }

        if (!StringUtils.isNullOrEmpty(details.getLoginKey())) {
            logon.getBody().setLoginKey(details.getLoginKey());
        }

        if (details.getSentryFileHash() != null) {
            logon.getBody().setShaSentryfile(ByteString.copyFrom(details.getSentryFileHash()));
        }
        logon.getBody().setEresultSentryfile(details.getSentryFileHash() != null ? EResult.OK.code() : EResult.FileNotFound.code());

        client.send(logon);
    }

    /**
     * Informs the Steam servers that this client wishes to log off from the network.
     */
    public void logOff() {
        ClientMessageProtobuf<CMsgClientLogOff.Builder> logOff = new ClientMessageProtobuf<>(CMsgClientLogOff.class, EMsg.ClientLogOff);
        client.send(logOff);

        // TODO: 2018-02-28 it seems like the socket is not closed after getting logged of or I am doing something horribly wrong, let's disconnect here
        client.disconnect();
    }
}
