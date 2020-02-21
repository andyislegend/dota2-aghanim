package com.avenga.steamclient.steam.steamuser;

import com.avenga.steamclient.base.ClientMessageProtobuf;
import com.avenga.steamclient.enums.EAccountType;
import com.avenga.steamclient.enums.EMsg;
import com.avenga.steamclient.enums.EResult;
import com.avenga.steamclient.exception.CallbackTimeoutException;
import com.avenga.steamclient.generated.MsgClientLogon;
import com.avenga.steamclient.model.SteamID;
import com.avenga.steamclient.protobufs.steamclient.SteammessagesClientserverLogin.CMsgClientLogOff;
import com.avenga.steamclient.protobufs.steamclient.SteammessagesClientserverLogin.CMsgClientLogon;
import com.avenga.steamclient.steam.client.SteamClient;
import com.avenga.steamclient.steam.steamuser.callback.UserLogOnCallbackHandler;
import com.avenga.steamclient.util.HardwareUtils;
import com.avenga.steamclient.util.NetworkUtils;
import com.avenga.steamclient.util.StringUtils;
import com.google.protobuf.ByteString;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class SteamUser {

    private final SteamClient client;

    public SteamUser(SteamClient client) {
        this.client = client;
    }

    /**
     * Logs the client into the Steam3 network.
     * The client should already have been connected at this point.
     *
     * @param logOnDetails The logOnDetails to use for logging on.
     * @return  CompletableFuture Callback with {@link UserLogOnResponse} on the user logOn request
     */
    public CompletableFuture<UserLogOnResponse> logOn(LogOnDetails logOnDetails) {
        var userLogOnCallback = this.client.addCallbackToQueue(UserLogOnCallbackHandler.CALLBACK_MESSAGE_CODE);
        sendLogonRequest(logOnDetails);
        return userLogOnCallback.getCallback()
                .thenApply(UserLogOnCallbackHandler::getMessage);
    }

    /**
     * Logs the client into the Steam3 network.
     * Result will be returned if callback will be finished in time, otherwise callback after specified timeout will be canceled.
     * The client should already have been connected at this point.
     *
     * @param logOnDetails The logOnDetails to use for logging on.
     * @param timeout The time which callback handler will wait before cancel it, in milliseconds.
     *
     * @throws CallbackTimeoutException if the wait timed out
     * @return response on the user logOn request
     */
    public UserLogOnResponse logOn(LogOnDetails logOnDetails, long timeout) throws CallbackTimeoutException {
        var userLogOnCallback = this.client.addCallbackToQueue(UserLogOnCallbackHandler.CALLBACK_MESSAGE_CODE);
        sendLogonRequest(logOnDetails);
        return UserLogOnCallbackHandler.handle(userLogOnCallback, timeout);
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

    private void sendLogonRequest(LogOnDetails logOnDetails) {
        Objects.requireNonNull(logOnDetails, "LogOn details wasn't provided");
        checkLogOnDetails(logOnDetails);

        ClientMessageProtobuf<CMsgClientLogon.Builder> logon = new ClientMessageProtobuf<>(CMsgClientLogon.class, EMsg.ClientLogon);
        SteamID steamID = new SteamID(logOnDetails.getAccountID(), logOnDetails.getAccountInstance(), client.getUniverse(), EAccountType.Individual);
        logon.getProtoHeader().setSteamid(steamID.convertToUInt64());
        setSessionData(logon, logOnDetails);
        setUserLogOnCredentials(logon, logOnDetails);
        setLoginMetadata(logon, logOnDetails);
        setSteamGuardProperties(logon, logOnDetails);

        client.send(logon);
    }

    private void checkLogOnDetails(LogOnDetails logOnDetails) {
        if (StringUtils.isNullOrEmpty(logOnDetails.getUsername()) || StringUtils.isNullOrEmpty(logOnDetails.getPassword())
                && StringUtils.isNullOrEmpty(logOnDetails.getLoginKey())) {
            throw new IllegalArgumentException("LogOn requires a username and password to be set in 'details'.");
        }

        if (!StringUtils.isNullOrEmpty(logOnDetails.getLoginKey()) && !logOnDetails.isShouldRememberPassword()) {
            // Prevent consumers from screwing this up.
            // If should_remember_password is false, the login_key is ignored server-side.
            // The inverse is not applicable (you can log in with should_remember_password and no login_key).
            throw new IllegalArgumentException("ShouldRememberPassword is required to be set to true in order to use LoginKey.");
        }
    }

    private void setSessionData(ClientMessageProtobuf<CMsgClientLogon.Builder> logon, LogOnDetails logOnDetails) {
        if (logOnDetails.getLoginID() != null) {
            logon.getBody().setObfustucatedPrivateIp(logOnDetails.getLoginID());
        } else {
            int localIp = NetworkUtils.getIPAddress(client.getLocalIP());
            logon.getBody().setObfustucatedPrivateIp(localIp ^ MsgClientLogon.ObfuscationMask);
        }
        logon.getProtoHeader().setClientSessionid(0);
        logon.getBody().setShouldRememberPassword(logOnDetails.isShouldRememberPassword());
    }

    private void setUserLogOnCredentials(ClientMessageProtobuf<CMsgClientLogon.Builder> logon, LogOnDetails logOnDetails) {
        logon.getBody().setAccountName(logOnDetails.getUsername());
        if (!StringUtils.isNullOrEmpty(logOnDetails.getPassword())) {
            logon.getBody().setPassword(logOnDetails.getPassword());
        }

        if (logOnDetails.getSentryFileHash() != null) {
            logon.getBody().setShaSentryfile(ByteString.copyFrom(logOnDetails.getSentryFileHash()));
        }
        logon.getBody().setEresultSentryfile(logOnDetails.getSentryFileHash() != null ? EResult.OK.code() : EResult.FileNotFound.code());
    }

    private void setLoginMetadata(ClientMessageProtobuf<CMsgClientLogon.Builder> logon, LogOnDetails logOnDetails) {
        logon.getBody().setProtocolVersion(MsgClientLogon.CurrentProtocol);
        logon.getBody().setClientOsType(logOnDetails.getClientOSType().code());
        logon.getBody().setClientLanguage(logOnDetails.getClientLanguage());
        logon.getBody().setCellId(logOnDetails.getCellID());
        logon.getBody().setSteam2TicketRequest(logOnDetails.isRequestSteam2Ticket());

        // we're now using the latest steamclient package version, this is required to get a proper sentry file for steam guard
        logon.getBody().setClientPackageVersion(1771); // todo: determine if this is still required
        logon.getBody().setSupportsRateLimitResponse(true);
        logon.getBody().setMachineId(ByteString.copyFrom(HardwareUtils.getMachineID()));

    }

    private void setSteamGuardProperties(ClientMessageProtobuf<CMsgClientLogon.Builder> logon, LogOnDetails logOnDetails) {
        if (!StringUtils.isNullOrEmpty(logOnDetails.getAuthCode())) {
            logon.getBody().setAuthCode(logOnDetails.getAuthCode());
        }

        if (!StringUtils.isNullOrEmpty(logOnDetails.getTwoFactorCode())) {
            logon.getBody().setTwoFactorCode(logOnDetails.getTwoFactorCode());
        }

        if (!StringUtils.isNullOrEmpty(logOnDetails.getLoginKey())) {
            logon.getBody().setLoginKey(logOnDetails.getLoginKey());
        }
    }
}
