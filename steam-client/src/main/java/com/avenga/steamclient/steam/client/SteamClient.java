package com.avenga.steamclient.steam.client;

import com.avenga.steamclient.base.ClientMessageProtobuf;
import com.avenga.steamclient.base.GCPacketMessage;
import com.avenga.steamclient.base.Message;
import com.avenga.steamclient.base.PacketMessage;
import com.avenga.steamclient.constant.Constant;
import com.avenga.steamclient.enums.EMsg;
import com.avenga.steamclient.enums.EResult;
import com.avenga.steamclient.exception.CallbackQueueException;
import com.avenga.steamclient.exception.CallbackTimeoutException;
import com.avenga.steamclient.generated.MsgClientLoggedOff;
import com.avenga.steamclient.model.configuration.SteamConfiguration;
import com.avenga.steamclient.model.steam.ClientHandler;
import com.avenga.steamclient.model.steam.CompletableCallback;
import com.avenga.steamclient.model.steam.SteamMessageCallback;
import com.avenga.steamclient.model.steam.gamecoordinator.GCMessage;
import com.avenga.steamclient.model.steam.user.LogOnDetailsRecord;
import com.avenga.steamclient.protobufs.steamclient.SteammessagesClientserver2.CMsgClientPlayingSessionState;
import com.avenga.steamclient.protobufs.steamclient.SteammessagesClientserver2.CMsgGCClient;
import com.avenga.steamclient.protobufs.steamclient.SteammessagesClientserverLogin.CMsgClientLoggedOff;
import com.avenga.steamclient.protobufs.steamclient.SteammessagesPlayerSteamclient;
import com.avenga.steamclient.provider.UserCredentialsProvider;
import com.avenga.steamclient.steam.CMClient;
import com.avenga.steamclient.steam.client.callback.ConnectedClientCallbackHandler;
import com.avenga.steamclient.steam.client.steamgamecoordinator.SteamGameCoordinator;
import com.avenga.steamclient.steam.client.steamgameserver.SteamGameServer;
import com.avenga.steamclient.steam.client.steamuser.SteamUser;
import com.avenga.steamclient.steam.client.steamuser.UserLogOnResponse;
import com.avenga.steamclient.util.MessageUtil;
import com.avenga.steamclient.util.SteamEnumUtils;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.avenga.steamclient.constant.ServiceMethodConstant.PLAYER_LAST_PLAYED_TIMES;

public class SteamClient extends CMClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(SteamClient.class);

    private static final int DEFAULT_SEQUENCE_VALUE = 0;
    private static final int CLIENT_APPLICATION_ID = 0;
    private static final long DEFAULT_RECONECT_TIMEOUT = 15000;

    private final BlockingQueue<CompletableCallback> callbacksQueue = new LinkedBlockingQueue<>();

    @Getter
    @Setter
    /**
     * List of the user details which will be used for connection rotation, in case user can't connect to Steam Network
     * or was logged off from Steam.
     */
    private UserCredentialsProvider credentialsProvider;

    @Getter
    /**
     * Sequence generator for queue callbacks.
     */
    private final AtomicInteger queueSequence = new AtomicInteger(DEFAULT_SEQUENCE_VALUE);

    private Map<Class<? extends ClientHandler>, ClientHandler> handlers = new HashMap<>();
    private Map<EMsg, Consumer<PacketMessage>> defaultPacketHandlers;

    private LogOnDetailsRecord currentLoggedUser;
    private boolean reconnectOnUserInitiated;
    private boolean connectingInProgress;

    /**
     * Initializes a new instance of the {@link SteamClient} class with the default configuration.
     */
    public SteamClient() {
        this(new SteamConfiguration());
    }

    /**
     * Initializes a new instance of the {@link SteamClient} class with a specific configuration.
     *
     * @param configuration The configuration to use for this client.
     */
    public SteamClient(SteamConfiguration configuration) {
        super(configuration);
        queueSequence.getAndIncrement();

        defaultPacketHandlers = Map.of(
                EMsg.ClientFromGC, this::handleClientFromGC,
                EMsg.ClientPlayingSessionState, this::handleGamePlayingSession,
                EMsg.ClientConcurrentSessionsBase, this::handleGamePlayingSession,
                EMsg.ServiceMethod, this::handleServiceMethod,
                EMsg.ClientLoggedOff, this::handleClientLogOff
        );

        addHandler(new SteamUser());
        addHandler(new SteamGameServer());
        addHandler(new SteamGameCoordinator());
    }

    /**
     * Creates and add steam client callback wrapper for handling {@link PacketMessage} received from Steam Network.
     *
     * @param messageCode Code of the packet message.
     * @return Callback wrapper which hold packet message callback.
     */
    public SteamMessageCallback<PacketMessage> addCallbackToQueue(int messageCode) {
        var steamCallback = new SteamMessageCallback<>(messageCode, CLIENT_APPLICATION_ID, queueSequence.getAndIncrement(),
                new CompletableFuture<PacketMessage>());

        if (!callbacksQueue.offer(steamCallback)) {
            throw new CallbackQueueException("Callback for handling message with code '" + messageCode + "' wasn't added to queue");
        }

        return steamCallback;
    }

    /**
     * Creates and add steam client callback wrapper for handling {@link PacketMessage} received from Steam Network.
     *
     * @param messageCode Code of the packet message.
     * @param properties  Additional properties of the callback.
     * @return Callback wrapper which hold packet message callback.
     */
    public SteamMessageCallback<PacketMessage> addCallbackToQueue(int messageCode, Properties properties) {
        var steamCallback = new SteamMessageCallback<>(messageCode, CLIENT_APPLICATION_ID, queueSequence.getAndIncrement(),
                new CompletableFuture<PacketMessage>(), properties);

        if (!callbacksQueue.offer(steamCallback)) {
            throw new CallbackQueueException("Callback for handling message with code '" + messageCode + "' wasn't added to queue");
        }

        return steamCallback;
    }

    /**
     * Creates and add steam client callback wrapper for handling {@link GCPacketMessage} received from Game Coordinator server.
     *
     * @param messageCode   Code of the packet message.
     * @param applicationId Id of the Steam client or games.
     * @return Callback wrapper which hold Game Coordinator packet message callback.
     */
    public SteamMessageCallback<GCPacketMessage> addGCCallbackToQueue(int messageCode, int applicationId) {
        var steamCallback = new SteamMessageCallback<>(messageCode, applicationId, queueSequence.getAndIncrement(),
                new CompletableFuture<GCPacketMessage>());

        if (!callbacksQueue.offer(steamCallback)) {
            throw new CallbackQueueException(
                    "Game Coordinator callback for handling message with code '" + messageCode + "' wasn't added to queue");
        }

        return steamCallback;
    }

    /**
     * Removes registered callback from queue.
     *
     * @param messageCallback Callback registered in queue.
     */
    public void removeCallbackFromQueue(SteamMessageCallback messageCallback) {
        callbacksQueue.remove(messageCallback);
    }

    /**
     * Establish connection with Steam Network server.
     *
     * @return CompletableFuture Callback which will be complete when {@link SteamClient} will be connected to server.
     */
    public CompletableFuture<PacketMessage> connectAndGetCallback() {
        var callback = addCallbackToQueue(ConnectedClientCallbackHandler.CALLBACK_MESSAGE_CODE);
        super.connect();
        return callback.getCallback();
    }

    /**
     * Establish connection with Steam Network server and login to Steam Server using credentials
     * prvided by {@link UserCredentialsProvider}.
     *
     * @return Log on response of the logged user to Steam Network.
     * @throws CallbackTimeoutException if the wait timed out
     */
    public UserLogOnResponse connectAndLogin() {
        Objects.requireNonNull(credentialsProvider, "User credential provider wasn't initialized");

        var userLogOnResponse = loginAndGetResponse();
        credentialsProvider.startResetBannedCredentialJob();
        return userLogOnResponse;
    }

    /**
     * Establish connection with Steam Network server for specified time.
     *
     * @param timeout Time during which handler will wait for callback.
     * @throws CallbackTimeoutException if the wait timed out
     */
    public void connect(long timeout) throws CallbackTimeoutException {
        var callback = addCallbackToQueue(ConnectedClientCallbackHandler.CALLBACK_MESSAGE_CODE);
        super.connect();
        ConnectedClientCallbackHandler.handle(callback, timeout, this);
    }

    /**
     * Close connection with Steam Network server and reset {@link SteamClient} queue sequence number.
     */
    @Override
    public void disconnect() {
        super.disconnect();
        queueSequence.set(DEFAULT_SEQUENCE_VALUE);
        queueSequence.getAndIncrement();
    }

    /**
     * Handle packet message received from Steam Network or Game Coordinator server.
     * <p>
     * Based on packet message type received from server callback from the {@link SteamClient} queue will picked and complete.
     *
     * @param packetMessage Message received from Steam server.
     * @return Was packet message processed by registered handlers.
     */
    @Override
    public boolean onClientMsgReceived(PacketMessage packetMessage) {
        if (!super.onClientMsgReceived(packetMessage)) {
            return false;
        }

        var packetHandler = defaultPacketHandlers.get(packetMessage.getMessageType());

        if (Objects.nonNull(packetHandler)) {
            packetHandler.accept(packetMessage);
        } else {
            findAndCompleteCallback(packetMessage.getMessageType().code(), CLIENT_APPLICATION_ID, packetMessage);
        }
        return true;
    }

    /**
     * Adds a new handler to the internal list of message handlers.
     *
     * @param handler The handler to add.
     */
    public void addHandler(ClientHandler handler) {
        if (handlers.containsKey(handler.getClass())) {
            throw new IllegalArgumentException("A handler of type " + handler.getClass() + " is already registered.");
        }

        handler.setup(this);
        handlers.put(handler.getClass(), handler);
    }

    /**
     * Removes a registered handler by name.
     *
     * @param handler The handler name to remove.
     */
    public void removeHandler(Class<? extends ClientHandler> handler) {
        handlers.remove(handler);
    }

    /**
     * Returns a registered handler.
     *
     * @param type The type of the handler to cast to. Must derive from {@link ClientHandler}.
     * @param <T>  The type of the handler to cast to. Must derive from {@link ClientHandler}.
     * @return A registered handler on success, or null if the handler could not be found.
     */
    @SuppressWarnings("unchecked")
    public <T extends ClientHandler> T getHandler(Class<T> type) {
        return (T) handlers.get(type);
    }

    /**
     * After connection with Steam Network server will be established this callback will be executed.
     */
    @Override
    protected void onClientConnected() {
        super.onClientConnected();
        findAndCompleteCallback(Constant.CONNECTED_PACKET_CODE, CLIENT_APPLICATION_ID, null);
    }

    /**
     * After connection with Steam Network server will be closed this callback will be executed.
     *
     * @param userInitiated whether the disconnect was initialized by the client
     */
    @Override
    protected void onClientDisconnected(boolean userInitiated) {
        super.onClientDisconnected(userInitiated);
        LOGGER.debug("Client was disconnected. Disconnect initiated by user: {}. Reconnect initiated by user : {}",
                userInitiated, reconnectOnUserInitiated);
        findAndCompleteCallback(Constant.DISCONNECTED_PACKET_CODE, CLIENT_APPLICATION_ID, null);
        checkAndReconnect(userInitiated);
    }

    private <T> void findAndCompleteCallback(int messageCode, int applicationId, T packetMessage) {
        Optional<CompletableCallback> messageCallback = callbacksQueue.stream()
                .filter(callback -> callback.getMessageCode() == messageCode && callback.getApplicationId() == applicationId)
                .findFirst();

        messageCallback.ifPresent(callback -> {
            callbacksQueue.remove(callback);
            callback.complete(packetMessage);
        });
    }

    private void handleClientFromGC(PacketMessage packetMessage) {
        var gcMessage = getGCPacketMessage(packetMessage);
        LOGGER.debug("<- Recv'd GC EMsg: {} ({}) (Proto: {})", SteamEnumUtils.getEnumName(
                gcMessage.geteMsg()).orElse(""), gcMessage.geteMsg(), gcMessage.isProto());

        findAndCompleteCallback(gcMessage.geteMsg(), gcMessage.getApplicationID(), gcMessage.getMessage());
    }

    private void handleGamePlayingSession(PacketMessage packetMessage) {
        ClientMessageProtobuf<CMsgClientPlayingSessionState.Builder> playingSessionBuilder = new ClientMessageProtobuf<>(
                CMsgClientPlayingSessionState.class, packetMessage);
        var playingSession = playingSessionBuilder.getBody().build();
        var predicate = getGamePlayedPredicate(playingSession);

        LOGGER.debug("Playing session game {} blocked: {}", playingSession.getPlayingApp(), playingSession.getPlayingBlocked());
        findAndCompleteServiceMethodCallback(packetMessage.getMessageType().code(), CLIENT_APPLICATION_ID, packetMessage, predicate);
    }

    private void handleServiceMethod(PacketMessage packetMessage) {
        MessageUtil.readServiceMethodBody(packetMessage).ifPresent(body -> handleServiceMethodBody(packetMessage, body));
    }

    private void handleClientLogOff(PacketMessage packetMessage) {
        var lastLogOnResult = EResult.Invalid;
        if (packetMessage.isProto()) {
            ClientMessageProtobuf<CMsgClientLoggedOff.Builder> loggedOff = new ClientMessageProtobuf<>(CMsgClientLoggedOff.class, packetMessage);
            lastLogOnResult = EResult.from(loggedOff.getBody().getEresult());
        } else {
            Message<MsgClientLoggedOff> loggedOff = new Message<>(MsgClientLoggedOff.class, packetMessage);
            lastLogOnResult = loggedOff.getBody().getResult();
        }

        LOGGER.debug("Client was logged off due to: {}", lastLogOnResult);
        reconnectOnUserInitiated = true;
        this.disconnect();
    }

    private GCMessage getGCPacketMessage(PacketMessage packetMessage) {
        var gcClientMessage = new ClientMessageProtobuf<CMsgGCClient.Builder>(CMsgGCClient.class, packetMessage);
        return new GCMessage(gcClientMessage.getBody());
    }

    private void handleServiceMethodBody(PacketMessage packetMessage, ClientMessageProtobuf body) {
        if (body.getBody() instanceof SteammessagesPlayerSteamclient.CPlayer_GetLastPlayedTimes_Response.Builder) {
            var playedTimesResponse = (SteammessagesPlayerSteamclient.CPlayer_GetLastPlayedTimes_Response) body.getBody().build();
            var gameIds = playedTimesResponse.getGamesList().stream()
                    .map(game -> String.valueOf(game.getAppid()))
                    .collect(Collectors.toList());
            var predicate = getServiceMethodBodyPredicate(gameIds);

            LOGGER.debug("Processing PlayedGame with matches: {}", gameIds);
            findAndCompleteServiceMethodCallback(packetMessage.getMessageType().code(), CLIENT_APPLICATION_ID, packetMessage, predicate);
        }
    }

    private <T> void findAndCompleteServiceMethodCallback(int messageCode, int applicationId, T packetMessage,
                                                          Predicate<CompletableCallback> predicate) {
        Optional<CompletableCallback> messageCallback = callbacksQueue.stream()
                .filter(callback -> callback.getMessageCode() == messageCode
                        && callback.getApplicationId() == applicationId
                        && predicate.test(callback))
                .findFirst();

        messageCallback.ifPresent(callback -> {
            callbacksQueue.remove(callback);
            callback.complete(packetMessage);
        });
    }

    private Predicate<CompletableCallback> getGamePlayedPredicate(CMsgClientPlayingSessionState playingSession) {
        return completableCallback -> Objects.nonNull(completableCallback.getProperties())
                && String.valueOf(playingSession.getPlayingApp()).equals(
                completableCallback.getProperties().get(PLAYER_LAST_PLAYED_TIMES).toString());
    }

    private Predicate<CompletableCallback> getServiceMethodBodyPredicate(List<String> gameIds) {
        return completableCallback -> Objects.nonNull(completableCallback.getProperties())
                && gameIds.contains(completableCallback.getProperties().get(PLAYER_LAST_PLAYED_TIMES).toString());
    }

    private UserLogOnResponse loginAndGetResponse() {
        var steamUser = getHandler(SteamUser.class);
        UserLogOnResponse logOnResponse = null;

        do {
            openConnection();
            currentLoggedUser = credentialsProvider.getNext();
            try {
                logOnResponse = steamUser.logOn(currentLoggedUser.getLogOnDetails(), DEFAULT_RECONECT_TIMEOUT);
            } catch (CallbackTimeoutException e) {
                logOnResponse = null;
                currentLoggedUser.blockFor(LogOnDetailsRecord.RECONNECT_TIMEOUT);
            }

            if (Objects.nonNull(logOnResponse) && logOnResponse.getResult() != EResult.OK) {
                checkAndBlockCredentials(logOnResponse.getResult());
            }
            credentialsProvider.returnKey(currentLoggedUser);
        } while (Objects.isNull(logOnResponse) || logOnResponse.getResult() != EResult.OK);

        LOGGER.debug("Successfully loged on with user: {}", currentLoggedUser.getLogOnDetails().getUsername());
        return logOnResponse;
    }

    private void openConnection() {
        connectingInProgress = true;
        while (connectingInProgress) {
            try {
                this.connect(DEFAULT_RECONECT_TIMEOUT);
            } catch (CallbackTimeoutException e) {
                waitBeforeNextTry();
                continue;
            }
            connectingInProgress = false;
        }
    }

    private void checkAndReconnect(boolean userInitiated) {
        if (Objects.nonNull(credentialsProvider) && !connectingInProgress) {
            if (reconnectOnUserInitiated || !userInitiated) {
                reconnectOnUserInitiated = false;
                connectAndLogin();
            } else {
                credentialsProvider.stopResetBannedCredentialJob();
            }
        }
    }

    private void checkAndBlockCredentials(EResult logOnResult) {
        switch (logOnResult) {
            case AccountDisabled:
            case InvalidPassword:
                currentLoggedUser.blockPermanently();
                LOGGER.warn("User {} has invalid password or account was disabled.",
                        currentLoggedUser.getLogOnDetails().getUsername());
                break;
            case NoConnection:
            case ServiceUnavailable:
            case Timeout:
            case TryAnotherCM:
                currentLoggedUser.blockFor(LogOnDetailsRecord.RECONNECT_TIMEOUT);
                LOGGER.debug("User {} was temporary blocked due to Steam connection status.",
                        currentLoggedUser.getLogOnDetails().getUsername());
                break;
            case RateLimitExceeded:
                currentLoggedUser.overLogOnLimitBlock();
                LOGGER.debug("User {} was blocked log on due to rate limit.",
                        currentLoggedUser.getLogOnDetails().getUsername());
                break;
            default:
                LOGGER.debug("LogOn response result: {}", logOnResult);
        }
    }

    private void waitBeforeNextTry() {
        try {
            TimeUnit.MILLISECONDS.sleep(DEFAULT_RECONECT_TIMEOUT);
        } catch (InterruptedException ex) {
            LOGGER.debug("Exception occuers during waiting connection retry: {}", ex.getMessage());
        }
    }
}
