package com.avenga.steamclient.steam.client;

import com.avenga.steamclient.base.ClientMessageProtobuf;
import com.avenga.steamclient.base.GCPacketMessage;
import com.avenga.steamclient.base.PacketMessage;
import com.avenga.steamclient.constant.Constant;
import com.avenga.steamclient.enums.EMsg;
import com.avenga.steamclient.exception.CallbackQueueException;
import com.avenga.steamclient.exception.CallbackTimeoutException;
import com.avenga.steamclient.model.configuration.SteamConfiguration;
import com.avenga.steamclient.model.steam.CompletableCallback;
import com.avenga.steamclient.model.steam.SteamMessageCallback;
import com.avenga.steamclient.model.steam.gamecoordinator.GCMessage;
import com.avenga.steamclient.protobufs.steamclient.SteammessagesClientserver2.CMsgClientPlayingSessionState;
import com.avenga.steamclient.protobufs.steamclient.SteammessagesClientserver2.CMsgGCClient;
import com.avenga.steamclient.protobufs.steamclient.SteammessagesPlayerSteamclient;
import com.avenga.steamclient.steam.CMClient;
import com.avenga.steamclient.steam.client.callback.ConnectedClientCallbackHandler;
import com.avenga.steamclient.steam.coordinator.AbstractGameCoordinator;
import com.avenga.steamclient.util.MessageUtil;
import com.avenga.steamclient.util.SteamEnumUtils;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.avenga.steamclient.constant.ServiceMethodConstant.PLAYER_LAST_PLAYED_TIMES;

public class SteamClient extends CMClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(SteamClient.class);

    private static final int DEFAULT_SEQUENCE_VALUE = 0;
    private static final int CLIENT_APPLICATION_ID = 0;

    private final BlockingQueue<CompletableCallback> callbacksQueue = new LinkedBlockingQueue<>();

    @Getter
    private final AtomicInteger queueSequence = new AtomicInteger(DEFAULT_SEQUENCE_VALUE);

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
     * Based on packet message type received from server first callback from
     * the {@link SteamClient} or {@link AbstractGameCoordinator} queue will picked and complete.
     *
     * @param packetMessage Message received from Steam server.
     * @return Was packet message processed by registered handlers.
     */
    @Override
    public boolean onClientMsgReceived(PacketMessage packetMessage) {
        if (!super.onClientMsgReceived(packetMessage)) {
            return false;
        }

        if (packetMessage.getMessageType() == EMsg.ClientFromGC) {
            var gcMessage = getGCPacketMessage(packetMessage);
            LOGGER.debug("<- Recv'd GC EMsg: {} ({}) (Proto: {})", SteamEnumUtils.getEnumName(
                    gcMessage.geteMsg()).orElse(""), gcMessage.geteMsg(), gcMessage.isProto());

            findAndCompleteCallback(gcMessage.geteMsg(), gcMessage.getApplicationID(), gcMessage.getMessage());
        } else if (packetMessage.getMessageType() == EMsg.ClientPlayingSessionState || packetMessage.getMessageType() == EMsg.ClientConcurrentSessionsBase) {
            handleGamePlayingSession(packetMessage);
        } else if (packetMessage.getMessageType() == EMsg.ServiceMethod) {
            MessageUtil.readServiceMethodBody(packetMessage).ifPresent(body -> handleServiceMethodBody(packetMessage, body));
        } else {
            findAndCompleteCallback(packetMessage.getMessageType().code(), CLIENT_APPLICATION_ID, packetMessage);
        }
        return true;
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
        findAndCompleteCallback(Constant.DISCONNECTED_PACKET_CODE, CLIENT_APPLICATION_ID, null);
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

    private GCMessage getGCPacketMessage(PacketMessage packetMessage) {
        var gcClientMessage = new ClientMessageProtobuf<CMsgGCClient.Builder>(CMsgGCClient.class, packetMessage);
        return new GCMessage(gcClientMessage.getBody());
    }

    private void handleServiceMethodBody(PacketMessage packetMessage, ClientMessageProtobuf body) {
        if (body.getBody() instanceof SteammessagesPlayerSteamclient.CPlayer_GetLastPlayedTimes_Response.Builder) {
            var playedTimesResponse = (SteammessagesPlayerSteamclient.CPlayer_GetLastPlayedTimes_Response) body.getBody().build();
            List<String> gameIds = playedTimesResponse.getGamesList().stream()
                    .map(game -> String.valueOf(game.getAppid()))
                    .collect(Collectors.toList());
            Predicate<CompletableCallback> predicate = completableCallback -> Objects.nonNull(completableCallback.getProperties())
                    && gameIds.contains(completableCallback.getProperties().get(PLAYER_LAST_PLAYED_TIMES).toString());

            LOGGER.debug("Processing PlayedGame with matches: {}", gameIds);
            findAndCompleteServiceMethodCallback(packetMessage.getMessageType().code(), CLIENT_APPLICATION_ID, packetMessage, predicate);
        }
    }

    private void handleGamePlayingSession(PacketMessage packetMessage) {
        ClientMessageProtobuf<CMsgClientPlayingSessionState.Builder> playingSessionBuilder = new ClientMessageProtobuf<>(
                CMsgClientPlayingSessionState.class, packetMessage);
        var playingSession = playingSessionBuilder.getBody().build();
        LOGGER.debug("Playing session game {} blocked: {}", playingSession.getPlayingApp(), playingSession.getPlayingBlocked());

        Predicate<CompletableCallback> predicate = completableCallback -> Objects.nonNull(completableCallback.getProperties())
                && String.valueOf(playingSession.getPlayingApp()).equals(
                completableCallback.getProperties().get(PLAYER_LAST_PLAYED_TIMES).toString());

        findAndCompleteServiceMethodCallback(packetMessage.getMessageType().code(), CLIENT_APPLICATION_ID, packetMessage, predicate);
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
}
