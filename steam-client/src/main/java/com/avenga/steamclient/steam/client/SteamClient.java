package com.avenga.steamclient.steam.client;

import com.avenga.steamclient.base.*;
import com.avenga.steamclient.constant.Constant;
import com.avenga.steamclient.constant.TaskConstant;
import com.avenga.steamclient.enums.EMsg;
import com.avenga.steamclient.enums.EResult;
import com.avenga.steamclient.enums.EServerType;
import com.avenga.steamclient.enums.SteamGame;
import com.avenga.steamclient.exception.CallbackCompletionException;
import com.avenga.steamclient.exception.CallbackQueueException;
import com.avenga.steamclient.exception.CallbackTimeoutException;
import com.avenga.steamclient.generated.MsgClientLoggedOff;
import com.avenga.steamclient.generated.MsgClientServerUnavailable;
import com.avenga.steamclient.model.JobID;
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
import com.avenga.steamclient.steam.client.task.Task;
import com.avenga.steamclient.steam.client.task.TaskHandlerJob;
import com.avenga.steamclient.util.MessageUtil;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.TemporalUnit;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static com.avenga.steamclient.constant.ServiceMethodConstant.PLAYER_LAST_PLAYED_TIMES;
import static com.avenga.steamclient.protobufs.tf.GCSystemMessages.EGCBaseClientMsg.k_EMsgGCClientHello;
import static java.util.stream.Collectors.toList;

public class SteamClient extends CMClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(SteamClient.class);

    private static final long DEFAULT_UNSET_JOB_ID = -1L;
    private static final int DEFAULT_SEQUENCE_VALUE = 0;
    private static final int CLIENT_APPLICATION_ID = 0;
    private static final long DEFAULT_RECONECT_TIMEOUT = 15000;

    private final BlockingQueue<CompletableCallback> callbacksQueue = new LinkedBlockingQueue<>();

    @Getter
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

    @Setter
    @Getter
    /**
     * Auto reconnect callback, which will be called after establishing connection with Steam Network and
     * sucessful login using credentials from {@link UserCredentialsProvider}. It will be used if user will
     * open connection using {@link #connectAndLogin()} method.
     */
    private Consumer<SteamClient> onAutoReconnect;

    /**
     * Flag to notify automated reconnect session to re-established connection after disconnect. It will be used
     * if user will open connection using {@link #connectAndLogin()} method and provide {@link UserCredentialsProvider}.
     */
    private AtomicBoolean reconnectOnUserInitiated = new AtomicBoolean();

    /**
     * Flag to check when auto reconnect execution is happaning. It could be used for tracking connection state in business logic,
     * when user will open connection using {@link #connectAndLogin()} method and provide {@link UserCredentialsProvider}.
     */
    private AtomicBoolean isAutoReconnectInProgress = new AtomicBoolean();

    /**
     * Handlers of the Steam Network APIs.
     */
    private Map<Class<? extends ClientHandler>, ClientHandler> handlers = new ConcurrentHashMap<>();

    /**
     * Handlers of the packet messages received from Steam Network.
     */
    private Map<EMsg, Consumer<PacketMessage>> clientPacketHandlers = new ConcurrentHashMap<>();

    private AtomicReference<LogOnDetailsRecord> currentLoggedUser = new AtomicReference<>();
    private AtomicBoolean connectingInProgress = new AtomicBoolean();
    private Instant processStartTime;
    private final AtomicLong jobSequence = new AtomicLong(DEFAULT_SEQUENCE_VALUE);
    private ScheduledExecutorService callbackQueueCleanJob;
    private TaskHandlerJob taskHandlerJob;
    private Map<String, Long> customCallbackTimeouts;
    private CompletableFuture<Boolean> disconnectCallback;

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
        this(configuration, Constant.DEFAULT_CLIENT_NAME);
    }

    /**
     * Initializes a new instance of the {@link SteamClient} class with the default configuration and specified
     * clientName prefix for logger messages.
     *
     * @param clientName prefix for logger messages.
     */
    public SteamClient(String clientName) {
        this(new SteamConfiguration(), clientName);
    }

    /**
     * Initializes a new instance of the {@link SteamClient} class with a specific configuration and specified
     * clientName prefix for logger messages.
     *
     * @param configuration The configuration to use for this client.
     * @param clientName prefix for logger messages.
     */
    public SteamClient(SteamConfiguration configuration, String clientName) {
        super(configuration, clientName);
        queueSequence.getAndIncrement();
        processStartTime = Instant.now();
        customCallbackTimeouts = new HashMap<>();
        addCustomCallbackTimeout(SteamGame.Dota2.getApplicationId(), k_EMsgGCClientHello.getNumber(),
                DEFAULT_RECONECT_TIMEOUT * SteamGameCoordinator.RETRY_COUNT);

        clientPacketHandlers.put(EMsg.ClientFromGC, this::handleClientFromGC);
        clientPacketHandlers.put(EMsg.ClientPlayingSessionState, this::handleGamePlayingSession);
        clientPacketHandlers.put(EMsg.ClientConcurrentSessionsBase, this::handleGamePlayingSession);
        clientPacketHandlers.put(EMsg.ServiceMethod, this::handleServiceMethod);
        clientPacketHandlers.put(EMsg.ClientLoggedOff, this::handleClientLogOff);
        clientPacketHandlers.put(EMsg.ClientServerUnavailable, this::handleClientServerUnavailable);

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
     * Creates and add steam client callback wrapper for handling {@link GCPacketMessage} received from Game Coordinator server.
     *
     * @param messageCode   Code of the packet message.
     * @param applicationId Id of the Steam client or games.
     * @param jobId Id of the job ID set in the header of the packet message.
     * @return Callback wrapper which hold Game Coordinator packet message callback.
     */
    public SteamMessageCallback<GCPacketMessage> addGCCallbackToQueue(int messageCode, int applicationId, long jobId) {
        var steamCallback = new SteamMessageCallback<>(messageCode, applicationId, queueSequence.getAndIncrement(), jobId,
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
     * @throws CallbackCompletionException if the intiating of the connection was interrupted.
     */
    public UserLogOnResponse connectAndLogin() {
        Objects.requireNonNull(credentialsProvider, "User credential provider wasn't initialized");

        try {
            return connectAndLoginAsync().get();
        } catch (InterruptedException | ExecutionException e) {
            throw new CallbackCompletionException(e.toString());
        }
    }

    public CompletableFuture<UserLogOnResponse> connectAndLoginAsync() {
        Objects.requireNonNull(credentialsProvider, "User credential provider wasn't initialized");

        initTaskHandlerJob();
        var callback = new CompletableFuture<UserLogOnResponse>();

        taskHandlerJob.registerTask(new Task(() -> {
            checkAndCleanQueue();
            var userLogOnResponse = loginAndGetResponse();
            credentialsProvider.startResetBannedCredentialJob();
            callback.complete(userLogOnResponse);
        }, TaskConstant.CONNECT_AND_LOGIN_TASK));

        return callback;
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
        if (Objects.nonNull(taskHandlerJob) && isAutoReconnectInProgress.get()) {
            taskHandlerJob.registerTask(new Task(() -> {
                disconnectCallback = new CompletableFuture<Boolean>();
                executeDisconnect();
                if (Objects.isNull(getConnection())) {
                    onClientDisconnected(true);
                }
                waitDisconnectCompletion();
            }, TaskConstant.DISCONNECT_TASK));
        } else {
            executeDisconnect();
        }
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

        var packetHandler = clientPacketHandlers.get(packetMessage.getMessageType());

        if (Objects.nonNull(packetHandler)) {
            packetHandler.accept(packetMessage);
        } else {
            findAndCompleteCallback(getCallbackPredicate(packetMessage.getMessageType().code(), CLIENT_APPLICATION_ID), packetMessage);
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
     * Provides username of the current logged user to the Steam Network.
     * Username will be available if user will open connection using {@link #connectAndLogin()} method.
     *
     * @return username of the current logged provided by {@link UserCredentialsProvider} or empty string.
     */
    public String getCurrentLoggedUsername() {
        return Objects.nonNull(currentLoggedUser.get()) ? currentLoggedUser.get().getLogOnDetails().getUsername() : "";
    }

    /**
     * Returns the next available JobID for job based messages.
     * JobID is used for specific Steam Network message with inner messages. e.g. {@link EMsg#ServiceMethodCallFromClient}
     *
     * @return The next available JobID.
     */
    public JobID getNextJobID() {
        JobID jobID = new JobID();
        jobID.setBoxID(DEFAULT_SEQUENCE_VALUE);
        jobID.setProcessID(DEFAULT_SEQUENCE_VALUE);
        jobID.setSequentialCount(jobSequence.incrementAndGet());
        jobID.setStartTime(processStartTime);

        return jobID;
    }

    /**
     * Sets list of the user details which will be used for connection rotation in {@link #connectAndLogin()} method,
     * in case user can't connect to Steam Network or was logged off from Steam.
     *
     * @param credentialsProvider of the Steam user credintails
     */
    public void setCredentialsProvider(UserCredentialsProvider credentialsProvider) {
        credentialsProvider.setClientName(clientName);
        this.credentialsProvider = credentialsProvider;
    }

    /**
     * Add custom callback expired timeout for specific Steam application packet message,
     * to prevent {@link #startCallbackCleanJob(long, long)} clean callbacks earlier.
     * Custom callback timeouts could be used for packet message with retry logic.
     *
     * @param applicationId of the Steam Game.
     * @param packetMessageCode code of the packet message sent to Steam Network.
     * @param expiredCallbackTime when callback for specific packet message should be cleaned.
     */
    public void addCustomCallbackTimeout(int applicationId, int packetMessageCode, long expiredCallbackTime) {
        customCallbackTimeouts.put(getCustomTimeoutKey(applicationId, packetMessageCode), expiredCallbackTime);
    }

    /**
     * Starts clean job of the {@link #callbacksQueue}. Callbacks registered for Steam client API methods which
     * provide {@link CompletableFuture}, won't be cleaned from callback queue automatically, in case
     * Game Coordinator won't respond on request. To handle such case user can init clean job and provide
     * time when callback should be cleaned.
     * <p>
     * Please consider expiredCallbackTime correctly based on your business logic to prevent cleaning valid callbacks,
     * e.g. {@link #connectAndLogin()} method use {@link #DEFAULT_RECONECT_TIMEOUT} in milliseconds.
     *
     * @param expiredCallbackTime in seconds after callback was registered and should be cleaned from queue.
     * @param cleanPeriod perid in seconds when job will check and clean expired callbacks.
     */
    public void startCallbackCleanJob(long expiredCallbackTime, long cleanPeriod) {
        if (expiredCallbackTime <= 0) {
            throw new IllegalArgumentException("Expired Callback time should be greater than 0");
        }
        if (cleanPeriod <= 0) {
            throw new IllegalArgumentException("Clean period should be greater than 0");
        }

        if (Objects.nonNull(callbackQueueCleanJob) && !callbackQueueCleanJob.isShutdown()) {
            return;
        }

        callbackQueueCleanJob = Executors.newSingleThreadScheduledExecutor();
        callbackQueueCleanJob.scheduleAtFixedRate(() -> {
            try {
                var currentTime = Instant.now();
                var expiredCallbacks = callbacksQueue.stream()
                        .filter(callback -> Objects.nonNull(callback.getCreatedAt())
                                && currentTime.isAfter(callback.getCreatedAt().plusSeconds(
                                        getCustomTimeoutOrDefault(callback, expiredCallbackTime))))
                        .collect(toList());

                if (!expiredCallbacks.isEmpty()) {
                    LOGGER.debug("{}: Cleaned {} expired callbacks from queue.", clientName, expiredCallbacks.size());
                    callbacksQueue.removeAll(expiredCallbacks);
                }
            } catch (Exception e) {
                LOGGER.debug("Exception thrown during cleaning callbacks queue: {}", e.toString());
            }
        }, cleanPeriod,  cleanPeriod, TimeUnit.SECONDS);
    }

    /**
     * Blocks current logged user for provided time period. User won't be provided from {@link UserCredentialsProvider}
     * during blocked time period.
     * <p>
     * This method used when user will open connection using {@link #connectAndLogin()} method.
     *
     * @param time during which user can't be used by {@link UserCredentialsProvider}.
     * @param timeUnit units of the provided time.
     */
    public void blockCurrentLoggedUser(long time, TemporalUnit timeUnit) {
        var currentUser = currentLoggedUser.get();
        blockLoggedUser(currentUser, time, timeUnit);
    }

    public boolean getReconnectOnUserInitiated() {
        return reconnectOnUserInitiated.get();
    }

    public void setReconnectOnUserInitiated(boolean reconnectOnUserInitiated) {
        var currentValue = this.reconnectOnUserInitiated.get();
        this.reconnectOnUserInitiated.compareAndSet(currentValue, reconnectOnUserInitiated);
    }

    /**
     * Blocks current logged user for provided time period and intiate reconnection of the Steam client with next user
     * provided by {@link UserCredentialsProvider}.
     * <p>
     * This method used when user will open connection using {@link #connectAndLogin()} method.
     *
     * @param time during which user can't be used by {@link UserCredentialsProvider}.
     * @param timeUnit units of the provided time.
     */
    public void blockLoggedUserAndInitReconnect(long time, TemporalUnit timeUnit) {
        blockCurrentLoggedUser(time, timeUnit);
        setReconnectOnUserInitiated(true);
        disconnect();
    }

    /**
     * Register client packet message handler for handling received {@link PacketMessage} from Steam Server Network.
     *
     * @param eMsg type of the received packet message.
     * @param packetMessageHandler handler which provide logic of the processing received message.
     */
    public void addClientPacketHandler(EMsg eMsg, Consumer<PacketMessage> packetMessageHandler) {
        Objects.requireNonNull(eMsg, "EMsg type wasn't provided.");
        Objects.requireNonNull(packetMessageHandler, "Packet message handler wasn't provided.");

        clientPacketHandlers.put(eMsg, packetMessageHandler);
    }

    /**
     * Flag to check when auto reconnect execution is happaning. It could be used for tracking connection state in business logic,
     * when user will open connection using {@link #connectAndLogin()} method and provide {@link UserCredentialsProvider}.
     */
    public boolean getIsAutoReconnectInProgress() {
        return isAutoReconnectInProgress.get();
    }

    /**
     * After connection with Steam Network server will be established this callback will be executed.
     */
    @Override
    protected void onClientConnected() {
        super.onClientConnected();
        findAndCompleteCallback(getCallbackPredicate(Constant.CONNECTED_PACKET_CODE, CLIENT_APPLICATION_ID), null);
    }

    /**
     * After connection with Steam Network server will be closed this callback will be executed.
     *
     * @param userInitiated whether the disconnect was initialized by the client
     */
    @Override
    protected void onClientDisconnected(boolean userInitiated) {
        super.onClientDisconnected(userInitiated);
        LOGGER.debug("{}: Client was disconnected. Disconnect initiated by user: {}. Reconnect initiated by user : {}",
                clientName, userInitiated, reconnectOnUserInitiated);
        findAndCompleteCallback(getCallbackPredicate(Constant.DISCONNECTED_PACKET_CODE, CLIENT_APPLICATION_ID), null);
        cleanBeforeDisconnect(userInitiated);
        checkAndReconnect(userInitiated);
        if (Objects.nonNull(disconnectCallback) && !disconnectCallback.isDone()) {
            disconnectCallback.complete(true);
        }
    }

    private <T> void findAndCompleteCallback(Predicate<CompletableCallback> callbackPredicate, T packetMessage) {
        Optional<CompletableCallback> messageCallback = callbacksQueue.stream()
                .filter(callbackPredicate)
                .findFirst();

        messageCallback.ifPresent(callback -> {
            callbacksQueue.remove(callback);
            callback.complete(packetMessage);
        });
    }

    private void handleClientFromGC(PacketMessage packetMessage) {
        var gcMessage = getGCPacketMessage(packetMessage);
        LOGGER.debug("{}: <- Recv'd GC EMsg: {} ({}) (Proto: {})", clientName, gcMessage.getMessageType(),
                gcMessage.geteMsg(), gcMessage.isProto());

        findAndCompleteCallback(getCallbackPredicate(gcMessage.geteMsg(), gcMessage.getApplicationID(),
            gcMessage.getMessage().getTargetJobID().getValue()), gcMessage.getMessage());
    }

    private void handleGamePlayingSession(PacketMessage packetMessage) {
        ClientMessageProtobuf<CMsgClientPlayingSessionState.Builder> playingSessionBuilder = new ClientMessageProtobuf<>(
                CMsgClientPlayingSessionState.class, packetMessage);
        var playingSession = playingSessionBuilder.getBody().build();
        var predicate = getCallbackPredicate(packetMessage.getMessageType().code(), CLIENT_APPLICATION_ID)
                .and(getGamePlayedPredicate(playingSession));

        LOGGER.debug("{}: Playing session game {} blocked: {}", clientName, playingSession.getPlayingApp(), playingSession.getPlayingBlocked());
        findAndCompleteCallback(predicate, packetMessage);
    }

    private void handleServiceMethod(PacketMessage packetMessage) {
        MessageUtil.readServiceMethodBody(packetMessage).ifPresent(body -> handleServiceMethodBody(packetMessage, body));
    }

    private void handleClientServerUnavailable(PacketMessage packetMessage) {
        ExtendedMessage<MsgClientServerUnavailable> serverUnavailableMessage = new ExtendedMessage<>(MsgClientServerUnavailable.class, packetMessage);
        LOGGER.debug("{}: Recieved client server unavailable for sent message: {} with server type: {}", clientName,
                MessageUtil.getMessage(serverUnavailableMessage.getBody().getEMsgSent()), serverUnavailableMessage.getBody().getEServerTypeUnavailable());

        if (serverUnavailableMessage.getBody().getEServerTypeUnavailable().equals(EServerType.GCH)) {
            try {
                TimeUnit.MINUTES.sleep(15);
            } catch (InterruptedException e) {
                LOGGER.debug("{}: Waiting of the disconnect timeout was interrupted.", clientName);
            }
            setReconnectOnUserInitiated(true);
            this.disconnect();
        }
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

        LOGGER.debug("{}: Client was logged off due to: {}", clientName, lastLogOnResult);
        setReconnectOnUserInitiated(true);
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
                    .collect(toList());
            var predicate = getCallbackPredicate(packetMessage.getMessageType().code(), CLIENT_APPLICATION_ID)
                    .and(getServiceMethodBodyPredicate(gameIds));

            LOGGER.debug("{}: Processing PlayedGame with matches: {}", clientName, gameIds);
            findAndCompleteCallback(predicate, packetMessage);
        }
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

    private synchronized UserLogOnResponse loginAndGetResponse() {
        isAutoReconnectInProgress.compareAndSet(false, true);
        var steamUser = getHandler(SteamUser.class);
        Optional<UserLogOnResponse> logOnResponse = Optional.empty();

        do {
            currentLoggedUser.set(credentialsProvider.getNext());
            openConnection();
            try {
                logOnResponse = steamUser.logOn(currentLoggedUser.get().getLogOnDetails(), DEFAULT_RECONECT_TIMEOUT);
            } catch (CallbackTimeoutException e) {
                logOnResponse = Optional.empty();
                currentLoggedUser.get().blockFor(LogOnDetailsRecord.RECONNECT_TIMEOUT);
            }

            if (logOnResponse.isPresent() && logOnResponse.get().getResult() != EResult.OK) {
                checkAndBlockCredentials(logOnResponse.get().getResult());
            }
            credentialsProvider.returnKey(currentLoggedUser.get());
        } while (!logOnResponse.isPresent() || logOnResponse.get().getResult() != EResult.OK);

        checkAndRunAutoReconnectCallback();
        isAutoReconnectInProgress.compareAndSet(true, false);
        LOGGER.debug("{}: Connection was successfully established with user: {}", clientName,
                currentLoggedUser.get().getLogOnDetails().getUsername());

        return logOnResponse.get();
    }

    private void openConnection() {
        connectingInProgress.compareAndSet(false, true);
        while (connectingInProgress.get()) {
            try {
                this.connect(DEFAULT_RECONECT_TIMEOUT);
            } catch (CallbackTimeoutException e) {
                waitBeforeNextTry();
                continue;
            }
            connectingInProgress.compareAndSet(true, false);
        }
    }

    private void checkAndReconnect(boolean userInitiated) {
        if (Objects.nonNull(credentialsProvider) && !connectingInProgress.get()) {
            if (reconnectOnUserInitiated.get() || !userInitiated) {
                setReconnectOnUserInitiated(false);
                connectAndLoginAsync();
            } else {
                credentialsProvider.stopResetBannedCredentialJob();
                checkAndStopTaskHandlerJob();
                checkAndShutdownCleanJob();
            }
        }
        if (Objects.isNull(credentialsProvider)) {
            checkAndShutdownCleanJob();
        }
    }

    private void checkAndBlockCredentials(EResult logOnResult) {
        switch (logOnResult) {
            case AccountDisabled:
            case InvalidPassword:
                currentLoggedUser.get().blockPermanently();
                LOGGER.warn("{}: User {} has invalid password or account was disabled.", clientName,
                        currentLoggedUser.get().getLogOnDetails().getUsername());
                break;
            case NoConnection:
            case ServiceUnavailable:
            case Timeout:
            case TryAnotherCM:
                currentLoggedUser.get().blockFor(LogOnDetailsRecord.RECONNECT_TIMEOUT);
                LOGGER.debug("{}: User {} was temporary blocked due to Steam connection status.", clientName,
                        currentLoggedUser.get().getLogOnDetails().getUsername());
                break;
            case RateLimitExceeded:
                currentLoggedUser.get().overLogOnLimitBlock();
                LOGGER.debug("{}: User {} was blocked log on due to rate limit.", clientName,
                        currentLoggedUser.get().getLogOnDetails().getUsername());
                break;
            default:
                LOGGER.debug("{}: User {} can't login to Steam Network.  LogOn response result: {}", clientName,
                        currentLoggedUser.get().getLogOnDetails().getUsername(),
                        logOnResult);
        }
    }

    private void waitBeforeNextTry() {
        try {
            TimeUnit.MILLISECONDS.sleep(DEFAULT_RECONECT_TIMEOUT);
        } catch (InterruptedException ex) {
            LOGGER.debug("{}: Exception occuers during waiting connection retry: {}", clientName, ex.toString());
        }
    }

    private void checkAndRunAutoReconnectCallback() {
        if (Objects.nonNull(onAutoReconnect)) {
            onAutoReconnect.accept(this);
        }
    }

    private void initTaskHandlerJob() {
        if (Objects.isNull(taskHandlerJob)) {
            taskHandlerJob = new TaskHandlerJob(clientName);
        }
    }

    private void checkAndCleanQueue() {
        if (!callbacksQueue.isEmpty()) {
            callbacksQueue.forEach(CompletableCallback::cancel);
            callbacksQueue.clear();
        }
    }

    private void cleanBeforeDisconnect(boolean userInitiated) {
        if (userInitiated && !connectingInProgress.get() && !reconnectOnUserInitiated.get()) {
            checkAndCleanQueue();
        }
    }

    private Predicate<CompletableCallback> getCallbackPredicate(int messageCode, int applicationId) {
        return completableCallback -> completableCallback.getMessageCode() == messageCode
                && completableCallback.getApplicationId() == applicationId;
    }

    private Predicate<CompletableCallback> getCallbackPredicate(int messageCode, int applicationId, long jobId) {
        if (jobId != DEFAULT_UNSET_JOB_ID) {
            return getCallbackPredicate(messageCode, applicationId)
                    .and(completableCallback -> completableCallback.getJobId() == jobId);
        }

        return getCallbackPredicate(messageCode, applicationId);
    }

    private void checkAndShutdownCleanJob() {
        if (Objects.nonNull(callbackQueueCleanJob) && !callbackQueueCleanJob.isShutdown()) {
            callbackQueueCleanJob.shutdown();
        }
    }

    private void checkAndStopReconnectionTask() {
        if (isAutoReconnectInProgress.get() && Objects.nonNull(taskHandlerJob)) {
            taskHandlerJob.cancelCurrentTask(task -> TaskConstant.CONNECT_AND_LOGIN_TASK.equals(task.getName()));
        }
    }

    private void checkAndStopTaskHandlerJob() {
        if (Objects.nonNull(taskHandlerJob)) {
            taskHandlerJob.stop();
        }
    }

    private String getCustomTimeoutKey(int applicationId, int packetMessageCode) {
        return applicationId + "-" + packetMessageCode;
    }

    private long getCustomTimeoutOrDefault(CompletableCallback callback, long defaultValue) {
        return customCallbackTimeouts.getOrDefault(
                getCustomTimeoutKey(callback.getApplicationId(), callback.getMessageCode()), defaultValue);
    }

    private void blockLoggedUser(LogOnDetailsRecord user, long time, TemporalUnit timeUnit) {
        if (Objects.nonNull(user)) {
            user.blockFor(time, timeUnit);
            credentialsProvider.returnBlockedKey(user);
        }
    }

    private void executeDisconnect() {
        super.disconnect();
        queueSequence.set(DEFAULT_SEQUENCE_VALUE);
        jobSequence.set(DEFAULT_SEQUENCE_VALUE);
        queueSequence.getAndIncrement();
    }

    private void waitDisconnectCompletion() {
        try {
            disconnectCallback.get();
        } catch (Exception e) {
            LOGGER.debug("{}: Disconnect callback completion was interrupted.", clientName);
        }
    }
}
