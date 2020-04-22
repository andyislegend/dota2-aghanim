package com.avenga.steamclient.steam;

import com.avenga.steamclient.base.*;
import com.avenga.steamclient.enums.*;
import com.avenga.steamclient.event.EventArgs;
import com.avenga.steamclient.event.EventHandler;
import com.avenga.steamclient.model.SteamID;
import com.avenga.steamclient.model.configuration.SteamConfiguration;
import com.avenga.steamclient.model.discovery.ServerRecord;
import com.avenga.steamclient.model.proxy.ProxyState;
import com.avenga.steamclient.network.*;
import com.avenga.steamclient.protobufs.steamclient.SteammessagesClientserverLogin.CMsgClientHeartBeat;
import com.avenga.steamclient.provider.SmartCMServerProvider;
import com.avenga.steamclient.steam.handler.*;
import com.avenga.steamclient.util.MessageUtil;
import com.avenga.steamclient.util.ScheduledFunction;
import com.avenga.steamclient.util.network.DebugNetworkListener;
import com.avenga.steamclient.util.network.PacketDebugNetworkListener;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static com.avenga.steamclient.enums.EMsg.*;

@Getter
@Setter
public class CMClient {

    private static final int HEART_BEAT_DELAY = 5000;

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

    private ConcurrentLinkedQueue<ProxyState> connectionProxies = new ConcurrentLinkedQueue<>();

    private ProxyState currentProxyState;

    @Setter
    @Getter
    /**
     * Set max unsuccessful amount of attemps to open connection using proxy from provided proxy list of the
     * {@link #registerConnectionProxies(List)} method. After max count of attemps will be reached, proxy connection
     * will be removed from connection proxy queue.
     */
    private int maxConnectionFialureCount = 5;

    /**
     * Use this for debugging only. For your convenience, you can use {@link PacketDebugNetworkListener} class.
     */
    private DebugNetworkListener debugNetworkListener;

    private ScheduledFunction heartBeatFunction;

    private Map<EServerType, Set<InetSocketAddress>> serverMap;

    private final Map<EMsg, ClientPacketHandler> packetHandlers;

    private CompletableFuture<Boolean> disconnectCallback;

    public CMClient(SteamConfiguration configuration) {
        Objects.requireNonNull(configuration, "Steam configuration wasn't provided");

        this.configuration = configuration;
        this.serverMap = new HashMap<>();
        this.heartBeatFunction = new ScheduledFunction(() ->
                send(new ClientMessageProtobuf<CMsgClientHeartBeat.Builder>(CMsgClientHeartBeat.class, EMsg.ClientHeartBeat)),
                HEART_BEAT_DELAY);

        this.packetHandlers = Map.of(
                Multi, new MultiClientPacketHandler(),
                ClientLogOnResponse, new LogOnClientPacketHandler(),
                ClientLoggedOff, new LoggedOffClientPacketHandler(),
                ClientCMList, new CMListClientPacketHandler(),
                ClientSessionToken, new SessionTokenClientPacketHandler(),
                ClientNewLoginKey, new UserNewLoginKeyClientPacketHandler()
        );
    }

    /**
     * Register connection proxies. They will be used to open TCP or WebSocket connection. Some of the Steam Network
     * services could have request limits by IP. This limitation could be overcome by proxy connection.
     *
     * @param proxies list of the connection proxies.
     */
    public void registerConnectionProxies(List<Proxy> proxies) {
        Objects.requireNonNull(proxies, "Connections Proxy list wasn't provided.");
        connectionProxies.clear();
        proxies.stream()
                .filter(Objects::nonNull)
                .map(ProxyState::new)
                .forEach(connectionProxies::offer);
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
                initDisconnectAndWait();

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

        LOGGER.debug("<- Recv'd EMsg: {} ({}) (Proto: {})", packetMessage.getMessageType(),
                packetMessage.getMessageType().code(), packetMessage.isProto());

        // Multi message gets logged down the line after it's decompressed
        if (!packetMessage.getMessageType().equals(Multi)) {
            try {
                if (Objects.nonNull(debugNetworkListener)) {
                    debugNetworkListener.onPacketMessageReceived(packetMessage.getMessageType(), packetMessage.getData());
                }
            } catch (Exception e) {
                LOGGER.debug("DebugNetworkListener threw an exception", e);
            }
        }

        var handler = this.packetHandlers.get(packetMessage.getMessageType());

        if (handler != null) {
            handler.handle(packetMessage, this);
        }

        return true;
    }

    /**
     * Sends the specified client message to the server. This method automatically assigns the correct SessionID and
     * SteamID of the message.
     *
     * @param message The client message to send.
     */
    public void send(ClientMessage message) {
        Objects.requireNonNull(message, "Client message wasn't provided");

        if (sessionID != null) {
            message.setSessionID(sessionID);
        }

        if (steamID != null) {
            message.setSteamID(steamID);
        }

        LOGGER.debug("Sent -> EMsg: {} (Proto: {})", message.getMsgType(), message.isProto());

        try {
            if (Objects.nonNull(debugNetworkListener)) {
                debugNetworkListener.onPacketMessageSent(message.getMsgType(), message.serialize());
            }
        } catch (Exception e) {
            LOGGER.debug("DebugNetworkListener threw an exception", e);
        }

        // we'll swallow any network failures here because they will be thrown later
        // on the network thread, and that will lead to a disconnect callback
        // down the line

        if (connection != null) {
            connection.send(message.serialize());
        }
    }

    public static PacketMessage getPacketMessage(byte[] data) {
        if (data.length < 4) {
            LOGGER.debug("PacketMsg too small to contain a message, was only {0} bytes. Message: 0x{1}");
            return null;
        }

        int rawEMsg = MessageUtil.getRawEMsg(data);
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
        checkAndReturnProxyToQueue();
    }

    private Connection createConnection(EnumSet<ProtocolType> protocol) {
        var currentProxy = getCurrentProxy();
        LOGGER.debug("Current proxy configuration: {}", currentProxy);

        if (protocol.contains(ProtocolType.WEB_SOCKET)) {
            return new WebSocketConnection(currentProxy);
        } else if (protocol.contains(ProtocolType.TCP)) {
            return new EnvelopeEncryptedConnection(new TcpConnection(currentProxy), getUniverse());
        } else if (protocol.contains(ProtocolType.UDP)) {
            return new EnvelopeEncryptedConnection(new UdpConnection(), getUniverse());
        }

        throw new IllegalArgumentException("Protocol bitmask has no supported protocols set.");
    }

    private final EventHandler<NetMsgEventArgs> netMsgReceived = (sender, netMsgEventArgs) ->
            onClientMsgReceived(getPacketMessage(netMsgEventArgs.getData()));

    private final EventHandler<EventArgs> connected = (sender, eventArgs) -> {
        getServers().tryMark(connection.getCurrentEndPoint(), connection.getProtocolTypes(), ServerQuality.GOOD);

        isConnected = true;
        onClientConnected();
    };

    private final EventHandler<DisconnectedEventArgs> disconnected = (sender, disconnectedEventArgs) -> {
        isConnected = false;

        if (!disconnectedEventArgs.isUserInitiated() && !expectDisconnection && !disconnectedEventArgs.isConnectionFailure()) {
            getServers().tryMark(connection.getCurrentEndPoint(), connection.getProtocolTypes(), ServerQuality.BAD);
        }

        sessionID = null;
        steamID = null;

        connection.getNetMsgReceived().removeEventHandler(netMsgReceived);
        connection.getConnected().removeEventHandler(connected);
        connection.getDisconnected().removeEventHandler(this.disconnected);
        connection = null;

        heartBeatFunction.stop();

        incrementCounterWhenConnectionFailure(disconnectedEventArgs.isConnectionFailure());

        onClientDisconnected(disconnectedEventArgs.isUserInitiated() || expectDisconnection);

        if (Objects.nonNull(disconnectCallback) && !disconnectCallback.isDone()) {
            var completeResult = disconnectCallback.complete(true);
        }
    };

    private void initDisconnectAndWait() {
        if (Objects.nonNull(connection)) {
            disconnectCallback = new CompletableFuture<>();
            disconnect();
            try {
                while (!disconnectCallback.isDone()) {
                    TimeUnit.MILLISECONDS.sleep(300);
                }
                disconnectCallback.get();
            } catch (InterruptedException | ExecutionException e) {
                LOGGER.debug("Disconnect callback was interupted", e);
            }
            disconnectCallback = null;
        }
    }

    private Proxy getCurrentProxy() {
        this.currentProxyState = connectionProxies.poll();
        return Objects.isNull(currentProxyState) ? Proxy.NO_PROXY : currentProxyState.getProxy();
    }

    private void checkAndReturnProxyToQueue() {
        if (Objects.nonNull(currentProxyState)) {
            LOGGER.debug("Return to connection proxy queue: " + currentProxyState.getProxy());
            this.connectionProxies.offer(currentProxyState);
        }
    }

    private void incrementCounterWhenConnectionFailure(boolean isConnectionFailure) {
        if (isConnectionFailure && Objects.nonNull(currentProxyState)) {
            currentProxyState.incrementFailureCount();
            LOGGER.debug("Failure conunter for {} equels to {}", currentProxyState.getProxy(),
                    currentProxyState.getConnectionFailureCount().get());

            if (currentProxyState.getConnectionFailureCount().get() < maxConnectionFialureCount) {
                this.connectionProxies.offer(currentProxyState);
            }
        }
    }
}
