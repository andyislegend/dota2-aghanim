package com.avenga.steamclient.steam;

import com.avenga.steamclient.base.*;
import com.avenga.steamclient.enums.*;
import com.avenga.steamclient.event.EventArgs;
import com.avenga.steamclient.event.EventHandler;
import com.avenga.steamclient.model.SteamID;
import com.avenga.steamclient.model.configuration.SteamConfiguration;
import com.avenga.steamclient.model.discovery.ServerRecord;
import com.avenga.steamclient.network.*;
import com.avenga.steamclient.protobufs.steamclient.SteammessagesClientserverLogin.CMsgClientHeartBeat;
import com.avenga.steamclient.provider.SmartCMServerProvider;
import com.avenga.steamclient.steam.handler.*;
import com.avenga.steamclient.util.MessageUtil;
import com.avenga.steamclient.util.ScheduledFunction;
import com.avenga.steamclient.util.stream.BinaryReader;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.avenga.steamclient.enums.EMsg.*;

@Getter
@Setter
public class CMClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(CMClient.class);

    private SteamConfiguration configuration;

    private boolean isConnected;

    private long sessionToken;

    private Integer cellID;

    private Integer sessionID;

    private SteamID steamID;

    private boolean expectDisconnection;

    // connection lock around the setup and tear down of the connection task
    private final Object connectionLock = new Object();

    private Connection connection;

    private ScheduledFunction heartBeatFunction;

    private Map<EServerType, Set<InetSocketAddress>> serverMap;

    private final Map<EMsg, ClientPacketHandler> packetHandlers;

    public CMClient(SteamConfiguration configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException("Configuration should be provided for CM client");
        }

        this.configuration = configuration;
        this.serverMap = new HashMap<>();
        this.heartBeatFunction = new ScheduledFunction(() ->
                send(new ClientMessageProtobuf<CMsgClientHeartBeat.Builder>(CMsgClientHeartBeat.class, EMsg.ClientHeartBeat)), 5000);

        this.packetHandlers = Map.of(
                Multi, new MultiClientPacketHandler(),
                ClientLogOnResponse, new LogOnClientPacketHandler(),
                ClientLoggedOff, new LoggedOffClientPacketHandler(),
                ClientServerList, new ServerListClientPacketHandler(),
                ClientCMList, new CMListClientPacketHandler(),
                ClientSessionToken, new SessionTokenClientPacketHandler(),
                ClientNewLoginKey, new UserNewLoginKeyClientPacketHandler()
        );
    }

    /**
     * Connects this client to a Steam3 server. This begins the process of connecting and encrypting the data channel
     * between the client and the server.
     * SteamKit will not attempt to reconnect to Steam, you must handle this callback and call Connect again preferably
     * after a short delay. SteamKit will randomly select a CM server from its internal list.
     */
    public void connect() {
        connect(null);
    }

    public void connect(ServerRecord cmServer) {
        synchronized (connectionLock) {
            try {
                disconnect();

                assert connection != null;

                expectDisconnection = false;

                if (cmServer == null) {
                    cmServer = getServers().getNextServerCandidate(configuration.getProtocolTypes());
                }

                connection = createConnection(configuration.getProtocolTypes());
                connection.getNetMsgReceived().addEventHandler(netMsgReceived);
                connection.getConnected().addEventHandler(connected);
                connection.getDisconnected().addEventHandler(disconnected);
                connection.connect(cmServer.getEndpoint());
            } catch (Exception e) {
                LOGGER.debug("Failed to connect to Steam network", e);
                onClientDisconnected(false);
            }
        }
    }

    /**
     * Disconnects this client.
     */
    public void disconnect() {
        synchronized (connectionLock) {
            heartBeatFunction.stop();

            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public boolean onClientMsgReceived(PacketMessage packetMessage) {
        if (packetMessage == null) {
            LOGGER.debug("Packet message failed to parse, shutting down connection");
            disconnect();
            return false;
        }

        LOGGER.debug(String.format("<- Recv'd EMsg: %s (%d) (Proto: %s)", packetMessage.getMessageType(),
                packetMessage.getMessageType().code(), packetMessage.isProto()));

        this.packetHandlers.getOrDefault(packetMessage.getMessageType(), ((packet, cmClient) ->
                LOGGER.debug("Unhandled packet message type: " + packet.getMessageType())))
                .handle(packetMessage, this);

        return true;
    }

    /**
     * Sends the specified client message to the server. This method automatically assigns the correct SessionID and
     * SteamID of the message.
     *
     * @param message The client message to send.
     */
    public void send(ClientMessage message) {
        if (message == null) {
            throw new IllegalArgumentException("A value for 'message' must be supplied");
        }

        if (sessionID != null) {
            message.setSessionID(sessionID);
        }

        if (steamID != null) {
            message.setSteamID(steamID);
        }

        LOGGER.debug(String.format("Sent -> EMsg: %s (Proto: %s)", message.getMsgType(), message.isProto()));

        // we'll swallow any network failures here because they will be thrown later
        // on the network thread, and that will lead to a disconnect callback
        // down the line

        if (connection != null) {
            connection.send(message.serialize());
        }
    }

    public static PacketMessage getPacketMsg(byte[] data) {
        if (data.length < 4) {
            LOGGER.debug("PacketMsg too small to contain a message, was only {0} bytes. Message: 0x{1}");
            return null;
        }

        BinaryReader reader = new BinaryReader(new ByteArrayInputStream(data));

        int rawEMsg = 0;
        try {
            rawEMsg = reader.readInt();
        } catch (IOException e) {
            LOGGER.debug("Exception while getting EMsg code", e);
        }
        EMsg eMsg = MessageUtil.getMessage(rawEMsg);

        if (eMsg == EMsg.ChannelEncryptRequest || eMsg == EMsg.ChannelEncryptResponse || eMsg == EMsg.ChannelEncryptResult) {
            try {
                return new DefaultPacketMessage(eMsg, data);
            } catch (IOException e) {
                LOGGER.debug("Exception deserializing emsg " + eMsg + " (" + MessageUtil.isProtoBuf(rawEMsg) + ").", e);
            }
        }

        try {
            if (MessageUtil.isProtoBuf(rawEMsg)) {
                return new ClientProtobufPacketMessage(eMsg, data);
            } else {
                return new ClientPacketMessage(eMsg, data);
            }
        } catch (IOException e) {
            LOGGER.debug("Exception deserializing emsg " + eMsg + " (" + MessageUtil.isProtoBuf(rawEMsg) + ").", e);
            return null;
        }
    }


    /**
     * @return Bootstrap list of CM servers.
     */
    public SmartCMServerProvider getServers() {
        return configuration.getServerProvider();
    }

    /**
     * Gets the universe of this client.
     *
     * @return The universe.
     */
    public EUniverse getUniverse() {
        return configuration.getUniverse();
    }

    /**
     * Returns the the local IP of this client.
     *
     * @return The local IP.
     */
    public InetAddress getLocalIP() {
        return connection.getLocalIP();
    }

    /**
     * Called when the client is physically disconnected from Steam3.
     *
     * @param userInitiated whether the disconnect was initialized by the client
     */
    protected void onClientDisconnected(boolean userInitiated) {
        for (Set<InetSocketAddress> set : serverMap.values()) {
            set.clear();
        }
    }

    /**
     * Called when the client is securely isConnected to Steam3.
     */
    protected void onClientConnected() {

    }

    private Connection createConnection(EnumSet<ProtocolType> protocol) {
        if (protocol.contains(ProtocolType.WEB_SOCKET)) {
            return new WebSocketConnection();
        } else if (protocol.contains(ProtocolType.TCP)) {
            return new EnvelopeEncryptedConnection(new TcpConnection(), getUniverse());
        } else if (protocol.contains(ProtocolType.UDP)) {
            return new EnvelopeEncryptedConnection(new UdpConnection(), getUniverse());
        }

        throw new IllegalArgumentException("Protocol bitmask has no supported protocols set.");
    }

    private final EventHandler<NetMsgEventArgs> netMsgReceived = (sender, netMsgEventArgs) ->
            onClientMsgReceived(getPacketMsg(netMsgEventArgs.getData()));

    private final EventHandler<EventArgs> connected = (sender, eventArgs) -> {
        getServers().tryMark(connection.getCurrentEndPoint(), connection.getProtocolTypes(), ServerQuality.GOOD);

        isConnected = true;
        onClientConnected();
    };

    private final EventHandler<DisconnectedEventArgs> disconnected = (sender, disconnectedEventArgs) -> {
        isConnected = false;

        if (!disconnectedEventArgs.isUserInitiated() && !expectDisconnection) {
            getServers().tryMark(connection.getCurrentEndPoint(), connection.getProtocolTypes(), ServerQuality.BAD);
        }

        sessionID = null;
        steamID = null;

        connection.getNetMsgReceived().removeEventHandler(netMsgReceived);
        connection.getConnected().removeEventHandler(connected);
        connection.getDisconnected().removeEventHandler(this.disconnected);
        connection = null;

        heartBeatFunction.stop();

        onClientDisconnected(disconnectedEventArgs.isUserInitiated() || expectDisconnection);
    };
}
