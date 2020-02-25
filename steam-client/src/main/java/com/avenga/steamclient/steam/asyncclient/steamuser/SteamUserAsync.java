package com.avenga.steamclient.steam.asyncclient.steamuser;

import com.avenga.steamclient.base.ClientMessageProtobuf;
import com.avenga.steamclient.base.Message;
import com.avenga.steamclient.base.PacketMessage;
import com.avenga.steamclient.enums.EAccountType;
import com.avenga.steamclient.enums.EMsg;
import com.avenga.steamclient.enums.EResult;
import com.avenga.steamclient.generated.MsgClientLogOnResponse;
import com.avenga.steamclient.generated.MsgClientLoggedOff;
import com.avenga.steamclient.generated.MsgClientLogon;
import com.avenga.steamclient.generated.MsgClientMarketingMessageUpdate2;
import com.avenga.steamclient.model.JobID;
import com.avenga.steamclient.model.SteamID;
import com.avenga.steamclient.model.steam.ClientMessageHandler;
import com.avenga.steamclient.protobufs.steamclient.SteammessagesClientserver.CMsgClientSessionToken;
import com.avenga.steamclient.protobufs.steamclient.SteammessagesClientserver.CMsgClientWalletInfoUpdate;
import com.avenga.steamclient.protobufs.steamclient.SteammessagesClientserver2.CMsgClientUpdateMachineAuth;
import com.avenga.steamclient.protobufs.steamclient.SteammessagesClientserverLogin;
import com.avenga.steamclient.protobufs.steamclient.SteammessagesClientserverLogin.CMsgClientAccountInfo;
import com.avenga.steamclient.protobufs.steamclient.SteammessagesClientserverLogin.CMsgClientLoggedOff;
import com.avenga.steamclient.protobufs.steamclient.SteammessagesClientserverLogin.CMsgClientNewLoginKey;
import com.avenga.steamclient.protobufs.steamclient.SteammessagesClientserverLogin.CMsgClientRequestWebAPIAuthenticateUserNonceResponse;
import com.avenga.steamclient.steam.asyncclient.steamuser.callback.*;
import com.avenga.steamclient.steam.steamuser.LogOnDetails;
import com.avenga.steamclient.util.HardwareUtils;
import com.avenga.steamclient.util.NetworkUtils;
import com.avenga.steamclient.util.StringUtils;
import com.google.protobuf.ByteString;

import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public class SteamUserAsync extends ClientMessageHandler {

    private Map<EMsg, Consumer<PacketMessage>> registeredPacketHandlers;

    public SteamUserAsync() {
        this.registeredPacketHandlers = Map.of(
                EMsg.ClientLogOnResponse, this::handleLogOnResponse,
                EMsg.ClientLoggedOff, this::handleLoggedOff,
                EMsg.ClientNewLoginKey, this::handleLoginKey,
                EMsg.ClientSessionToken, this::handleSessionToken,
                EMsg.ClientUpdateMachineAuth, this::handleUpdateMachineAuth,
                EMsg.ClientAccountInfo, this::handleAccountInfo,
                EMsg.ClientWalletInfoUpdate, this::handleWalletInfo,
                EMsg.ClientRequestWebAPIAuthenticateUserNonceResponse, this::handleWebAPIUserNonce,
                EMsg.ClientMarketingMessageUpdate2, this::handleMarketingMessageUpdate
        );
    }

    @Override
    public void handleMessage(PacketMessage packetMessage) {
        Objects.requireNonNull(packetMessage, "Packet message wasn't provided");

        Consumer<PacketMessage> dispatcher = registeredPacketHandlers.get(packetMessage.getMessageType());
        if (dispatcher != null) {
            dispatcher.accept(packetMessage);
        }
    }

    /**
     * Logs the client into the Steam3 network.
     * The client should already have been connected at this point.
     *
     * @param logOnDetails The details to use for logging on.
     */
    public void logOn(LogOnDetails logOnDetails) {
        Objects.requireNonNull(logOnDetails, "LogOn details wasn't provided");
        checkLogOnDetails(logOnDetails);

        ClientMessageProtobuf<SteammessagesClientserverLogin.CMsgClientLogon.Builder> logon = new ClientMessageProtobuf<>(SteammessagesClientserverLogin.CMsgClientLogon.class, EMsg.ClientLogon);
        SteamID steamID = new SteamID(logOnDetails.getAccountID(), logOnDetails.getAccountInstance(), client.getUniverse(), EAccountType.Individual);
        logon.getProtoHeader().setSteamid(steamID.convertToUInt64());
        setSessionData(logon, logOnDetails);
        setUserLogOnCredentials(logon, logOnDetails);
        setLoginMetadata(logon, logOnDetails);
        setSteamGuardProperties(logon, logOnDetails);

        client.send(logon);
    }

    /**
     * Informs the Steam servers that this client wishes to log off from the network.
     */
    public void logOff() {
        ClientMessageProtobuf<SteammessagesClientserverLogin.CMsgClientLogOff.Builder> logOff = new ClientMessageProtobuf<>(SteammessagesClientserverLogin.CMsgClientLogOff.class, EMsg.ClientLogOff);
        client.send(logOff);
        client.disconnect();
    }

    private void handleLogOnResponse(PacketMessage packetMsg) {
        if (packetMsg.isProto()) {
            ClientMessageProtobuf<SteammessagesClientserverLogin.CMsgClientLogonResponse.Builder> logonResp = new ClientMessageProtobuf<>(SteammessagesClientserverLogin.CMsgClientLogonResponse.class, packetMsg);

            client.postCallback(new LoggedOnCallback(logonResp.getBody()));
        } else {
            Message<MsgClientLogOnResponse> logonResp = new Message<>(MsgClientLogOnResponse.class, packetMsg);

            client.postCallback(new LoggedOnCallback(logonResp.getBody()));
        }
    }

    private void handleLoggedOff(PacketMessage packetMsg) {
        EResult result;

        if (packetMsg.isProto()) {
            ClientMessageProtobuf<CMsgClientLoggedOff.Builder> loggedOff = new ClientMessageProtobuf<>(CMsgClientLoggedOff.class, packetMsg);
            result = EResult.from(loggedOff.getBody().getEresult());
        } else {
            Message<MsgClientLoggedOff> loggedOff = new Message<>(MsgClientLoggedOff.class, packetMsg);
            result = loggedOff.getBody().getResult();
        }

        client.postCallback(new LoggedOffCallback(result));
        client.disconnect();
    }

    private void handleLoginKey(PacketMessage packetMsg) {
        ClientMessageProtobuf<CMsgClientNewLoginKey.Builder> loginKey = new ClientMessageProtobuf<>(CMsgClientNewLoginKey.class, packetMsg);
        client.postCallback(new LoginKeyCallback(loginKey.getBody()));
    }

    private void handleSessionToken(PacketMessage packetMsg) {
        ClientMessageProtobuf<CMsgClientSessionToken.Builder> sessToken = new ClientMessageProtobuf<>(CMsgClientSessionToken.class, packetMsg);
        client.postCallback(new SessionTokenCallback(sessToken.getBody()));
    }

    private void handleUpdateMachineAuth(PacketMessage packetMsg) {
        ClientMessageProtobuf<CMsgClientUpdateMachineAuth.Builder> machineAuth = new ClientMessageProtobuf<>(CMsgClientUpdateMachineAuth.class, packetMsg);
        client.postCallback(new UpdateMachineAuthCallback(new JobID(packetMsg.getSourceJobID()), machineAuth.getBody()));
    }

    private void handleAccountInfo(PacketMessage packetMsg) {
        ClientMessageProtobuf<CMsgClientAccountInfo.Builder> accInfo = new ClientMessageProtobuf<>(CMsgClientAccountInfo.class, packetMsg);
        client.postCallback(new AccountInfoCallback(accInfo.getBody()));
    }

    private void handleWalletInfo(PacketMessage packetMsg) {
        ClientMessageProtobuf<CMsgClientWalletInfoUpdate.Builder> walletInfo = new ClientMessageProtobuf<>(CMsgClientWalletInfoUpdate.class, packetMsg);
        client.postCallback(new WalletInfoCallback(walletInfo.getBody()));
    }

    private void handleWebAPIUserNonce(PacketMessage packetMsg) {
        ClientMessageProtobuf<CMsgClientRequestWebAPIAuthenticateUserNonceResponse.Builder> userNonce = new ClientMessageProtobuf<>(CMsgClientRequestWebAPIAuthenticateUserNonceResponse.class, packetMsg);
        client.postCallback(new WebAPIUserNonceCallback(userNonce.getTargetJobID(), userNonce.getBody()));
    }

    private void handleMarketingMessageUpdate(PacketMessage packetMsg) {
        Message<MsgClientMarketingMessageUpdate2> marketingMessage = new Message<>(MsgClientMarketingMessageUpdate2.class, packetMsg);
        byte[] payload = marketingMessage.getPayload().toByteArray();
        client.postCallback(new MarketingMessageCallback(marketingMessage.getBody(), payload));
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

    private void setSessionData(ClientMessageProtobuf<SteammessagesClientserverLogin.CMsgClientLogon.Builder> logon, LogOnDetails logOnDetails) {
        if (logOnDetails.getLoginID() != null) {
            logon.getBody().setObfustucatedPrivateIp(logOnDetails.getLoginID());
        } else {
            int localIp = NetworkUtils.getIPAddress(client.getLocalIP());
            logon.getBody().setObfustucatedPrivateIp(localIp ^ MsgClientLogon.ObfuscationMask);
        }
        logon.getProtoHeader().setClientSessionid(0);
        logon.getBody().setShouldRememberPassword(logOnDetails.isShouldRememberPassword());
    }

    private void setUserLogOnCredentials(ClientMessageProtobuf<SteammessagesClientserverLogin.CMsgClientLogon.Builder> logon, LogOnDetails logOnDetails) {
        logon.getBody().setAccountName(logOnDetails.getUsername());
        if (!StringUtils.isNullOrEmpty(logOnDetails.getPassword())) {
            logon.getBody().setPassword(logOnDetails.getPassword());
        }

        if (logOnDetails.getSentryFileHash() != null) {
            logon.getBody().setShaSentryfile(ByteString.copyFrom(logOnDetails.getSentryFileHash()));
        }
        logon.getBody().setEresultSentryfile(logOnDetails.getSentryFileHash() != null ? EResult.OK.code() : EResult.FileNotFound.code());
    }

    private void setLoginMetadata(ClientMessageProtobuf<SteammessagesClientserverLogin.CMsgClientLogon.Builder> logon, LogOnDetails logOnDetails) {
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

    private void setSteamGuardProperties(ClientMessageProtobuf<SteammessagesClientserverLogin.CMsgClientLogon.Builder> logon, LogOnDetails logOnDetails) {
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
