package com.avenga.steamclient.steam.asyncclient.steamgameserver;

import com.avenga.steamclient.base.ClientMessageProtobuf;
import com.avenga.steamclient.base.PacketMessage;
import com.avenga.steamclient.constant.Constant;
import com.avenga.steamclient.enums.EAccountType;
import com.avenga.steamclient.enums.EMsg;
import com.avenga.steamclient.enums.EResult;
import com.avenga.steamclient.enums.EServerFlags;
import com.avenga.steamclient.generated.MsgClientLogon;
import com.avenga.steamclient.model.SteamID;
import com.avenga.steamclient.model.steam.ClientMessageHandler;
import com.avenga.steamclient.model.steam.gameserver.LogOnDetails;
import com.avenga.steamclient.model.steam.gameserver.StatusDetails;
import com.avenga.steamclient.protobufs.steamclient.SteammessagesClientserver;
import com.avenga.steamclient.protobufs.steamclient.SteammessagesClientserver.CMsgClientGameConnectTokens;
import com.avenga.steamclient.protobufs.steamclient.SteammessagesClientserver.CMsgClientGamesPlayed;
import com.avenga.steamclient.protobufs.steamclient.SteammessagesClientserver.CMsgClientTicketAuthComplete;
import com.avenga.steamclient.protobufs.steamclient.SteammessagesClientserverLogin.CMsgClientLogOff;
import com.avenga.steamclient.protobufs.steamclient.SteammessagesClientserverLogin.CMsgClientLogon;
import com.avenga.steamclient.steam.asyncclient.callbacks.DisconnectedCallback;
import com.avenga.steamclient.steam.asyncclient.steamgameserver.callback.GameConnectTokensCallback;
import com.avenga.steamclient.steam.asyncclient.steamgameserver.callback.StatusReplyCallback;
import com.avenga.steamclient.steam.asyncclient.steamgameserver.callback.TicketAuthCallback;
import com.avenga.steamclient.steam.asyncclient.steamuser.callback.LoggedOnCallback;
import com.avenga.steamclient.util.HardwareUtils;
import com.avenga.steamclient.util.NetworkUtils;
import com.avenga.steamclient.util.StringUtils;
import com.avenga.steamclient.util.Utils;
import com.google.protobuf.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Inet6Address;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import static com.avenga.steamclient.enums.EMsg.ClientGamesPlayed;

/**
 * This handler is used for interacting with the Steam network as a game server.
 */
public class SteamGameServerAsync extends ClientMessageHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SteamGameServerAsync.class);

    private final static int DEFAULT_ACCOUNT_ID = 0;
    private final static int DEFAULT_APPLICATION_ID = 0;
    private final static int DEFAULT_INSTANCE = 0;
    private final static int DEFAULT_SESSION_ID = 0;

    private Map<EMsg, Consumer<PacketMessage>> dispatchMap;

    public SteamGameServerAsync() {
        dispatchMap = Map.of(
                EMsg.GSStatusReply, this::handleStatusReply,
                EMsg.ClientTicketAuthComplete, this::handleAuthComplete,
                EMsg.ClientGameConnectTokens, this::handleGameConnection
        );
    }

    /**
     * Logs onto the Steam network as a persistent game server.
     * The client should already have been connected at this point.
     * Results are return in a {@link LoggedOnCallback}.
     *
     * @param details The details to use for logging on.
     */
    public void logOn(LogOnDetails details) {
        Objects.requireNonNull(details, "LogOn Game Server details wasn't provided");

        if (StringUtils.isNullOrEmpty(details.getToken())) {
            throw new IllegalArgumentException("LogOn requires a game server token to be set in 'details'.");
        }

        if (!client.isConnected()) {
            client.postCallback(new LoggedOnCallback(EResult.NoConnection));
            return;
        }

        ClientMessageProtobuf<CMsgClientLogon.Builder> logon = new ClientMessageProtobuf<>(CMsgClientLogon.class, EMsg.ClientLogonGameServer);
        setLogOnProtoHeader(logon, EAccountType.GameServer);
        setDefaultLogOnBody(logon);
        setLogOnBody(logon, details);
        client.send(logon);
    }

    /**
     * Logs the client into the Steam3 network as an anonymous game server.
     * The client should already have been connected at this point.
     * Results are return in a {@link LoggedOnCallback}.
     */
    public void logOnAnonymous() {
        logOnAnonymous(DEFAULT_APPLICATION_ID);
    }

    /**
     * Logs the client into the Steam3 network as an anonymous game server.
     * The client should already have been connected at this point.
     * Results are return in a {@link LoggedOnCallback}.
     *
     * @param applicationId The application ID served by this game server, or 0 for the default.
     */
    public void logOnAnonymous(int applicationId) {
        if (!client.isConnected()) {
            client.postCallback(new LoggedOnCallback(EResult.NoConnection));
            return;
        }

        ClientMessageProtobuf<CMsgClientLogon.Builder> logon = new ClientMessageProtobuf<>(CMsgClientLogon.class, EMsg.ClientLogonGameServer);
        setLogOnProtoHeader(logon, EAccountType.AnonGameServer);
        setDefaultLogOnBody(logon);
        logon.getBody().setGameServerAppId(applicationId);
        client.send(logon);
    }

    /**
     * Informs the Steam servers that this client wishes to log off from the network.
     * The Steam server will disconnect the client, and a {@link DisconnectedCallback} will be posted.
     */
    public void logOff() {
        setExpectDisconnection(true);

        ClientMessageProtobuf<CMsgClientLogOff.Builder> logOff = new ClientMessageProtobuf<>(CMsgClientLogOff.class, EMsg.ClientLogOff);
        client.send(logOff);
        client.disconnect();
    }

    /**
     * Sends the server's status to the Steam network.
     * Results are returned in a {@link StatusReplyCallback} callback.
     *
     * @param details A {@link StatusDetails} object containing the server's status.
     */
    public void sendStatus(StatusDetails details) {
        Objects.requireNonNull(details, "Status details wan't provided");

        if (details.getAddress() != null && details.getAddress() instanceof Inet6Address) {
            throw new IllegalArgumentException("Only IPv4 addresses are supported.");
        }

        client.send(getStatus(details));
    }

    /**
     * Sends the game played status to the Steam network.
     * Results are returned in a {@link GameConnectTokensCallback} callback.
     *
     * @param applicationIds The application IDs served by this game server.
     */
    public void sendGamePlayed(List<Integer> applicationIds) {
        Objects.requireNonNull(applicationIds, "List of the application ids wasn't provided");
        if (applicationIds.size() > Constant.MAX_PLAYED_GAMES) {
            throw new IllegalArgumentException("Steam only allow " + Constant.MAX_PLAYED_GAMES + " games to be in played status at one time");
        }

        var gamePlayedMessage = new ClientMessageProtobuf<CMsgClientGamesPlayed.Builder>(CMsgClientGamesPlayed.class, ClientGamesPlayed);

        applicationIds.forEach(applicationId -> {
            var gamePlayed = CMsgClientGamesPlayed.GamePlayed.newBuilder()
                    .setGameId(applicationId)
                    .build();
            gamePlayedMessage.getBody().addGamesPlayed(gamePlayed);
        });
        client.send(gamePlayedMessage);
    }

    @Override
    public void handleMessage(PacketMessage packetMessage) {
        Objects.requireNonNull(packetMessage, "Packet message wasn't provided");

        Consumer<PacketMessage> dispatcher = dispatchMap.get(packetMessage.getMessageType());
        if (dispatcher != null) {
            dispatcher.accept(packetMessage);
        }
    }

    private void handleStatusReply(PacketMessage packetMessage) {
        ClientMessageProtobuf<SteammessagesClientserver.CMsgGSStatusReply.Builder> statusReply =
                new ClientMessageProtobuf<>(SteammessagesClientserver.CMsgGSStatusReply.class, packetMessage);

        client.postCallback(new StatusReplyCallback(statusReply.getBody()));
    }

    private void handleAuthComplete(PacketMessage packetMessage) {
        ClientMessageProtobuf<CMsgClientTicketAuthComplete.Builder> statusReply =
                new ClientMessageProtobuf<>(CMsgClientTicketAuthComplete.class, packetMessage);

        client.postCallback(new TicketAuthCallback(statusReply.getBody()));
    }

    private void handleGameConnection(PacketMessage packetMessage) {
        ClientMessageProtobuf<CMsgClientGameConnectTokens.Builder> gameConnectTokens =
                new ClientMessageProtobuf<>(CMsgClientGameConnectTokens.class, packetMessage);

        client.postCallback(new GameConnectTokensCallback(gameConnectTokens.getBody()));
    }


    private void setLogOnProtoHeader(ClientMessageProtobuf<CMsgClientLogon.Builder> logon, EAccountType accountType) {
        SteamID gameServerId = new SteamID(DEFAULT_ACCOUNT_ID, DEFAULT_INSTANCE, client.getUniverse(), accountType);
        logon.getProtoHeader().setClientSessionid(DEFAULT_SESSION_ID);
        logon.getProtoHeader().setSteamid(gameServerId.convertToUInt64());
    }

    private void setLogOnBody(ClientMessageProtobuf<CMsgClientLogon.Builder> logon, LogOnDetails details) {
        logon.getBody().setGameServerAppId(details.getAppID());
        logon.getBody().setGameServerToken(details.getToken());
    }

    private void setDefaultLogOnBody(ClientMessageProtobuf<CMsgClientLogon.Builder> logon) {
        int localIp = NetworkUtils.getIPAddress(client.getLocalIP());
        logon.getBody().setObfustucatedPrivateIp(localIp ^ MsgClientLogon.ObfuscationMask);
        logon.getBody().setProtocolVersion(MsgClientLogon.CurrentProtocol);
        logon.getBody().setClientOsType(Utils.getOSType().code());
        logon.getBody().setMachineId(ByteString.copyFrom(HardwareUtils.getMachineID()));
    }

    private ClientMessageProtobuf<SteammessagesClientserver.CMsgGSServerType.Builder> getStatus(StatusDetails details) {
        ClientMessageProtobuf<SteammessagesClientserver.CMsgGSServerType.Builder> status =
                new ClientMessageProtobuf<>(SteammessagesClientserver.CMsgGSServerType.class, EMsg.GSServerType);
        status.getBody().setAppIdServed(details.getAppID());
        status.getBody().setFlags(EServerFlags.code(details.getServerFlags()));
        status.getBody().setGameDir(details.getGameDirectory());
        status.getBody().setGamePort(details.getPort());
        status.getBody().setGameQueryPort(details.getQueryPort());
        status.getBody().setGameVersion(details.getVersion());

        if (details.getAddress() != null) {
            status.getBody().setGameIpAddress(NetworkUtils.getIPAddress(details.getAddress()));
        }

        return status;
    }
}
