package com.avenga.steamclient.steam.client;

import com.avenga.steamclient.base.PacketMessage;
import com.avenga.steamclient.constant.Constant;
import com.avenga.steamclient.enums.EMsg;
import com.avenga.steamclient.exception.CallbackQueueException;
import com.avenga.steamclient.exception.CallbackTimeoutException;
import com.avenga.steamclient.model.configuration.SteamConfiguration;
import com.avenga.steamclient.model.steam.SteamMessageCallback;
import com.avenga.steamclient.steam.CMClient;
import com.avenga.steamclient.steam.client.callback.ConnectedClientCallbackHandler;
import com.avenga.steamclient.steam.coordinator.AbstractGameCoordinator;
import com.avenga.steamclient.steam.coordinator.impl.GameCoordinator;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class SteamClient extends CMClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(SteamClient.class);

    private static final int DEFAULT_SEQUENCE_VALUE = 0;

    private final BlockingQueue<SteamMessageCallback<PacketMessage>> callbacksQueue = new LinkedBlockingQueue<>();

    /**
     * Callback handler for handling {@link GameCoordinator} GC packet messages registered in GC queue.
     */
    @Setter
    private Consumer<PacketMessage> onGcCallback = (packetMessage -> {
        LOGGER.debug("Skipping callback from GC: " + packetMessage.getMessageType().code());
    });

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

    public SteamMessageCallback<PacketMessage> addCallbackToQueue(int messageCode) {
        var steamMessageCallback = new SteamMessageCallback<PacketMessage>(messageCode, queueSequence.getAndIncrement());

        if (!callbacksQueue.offer(steamMessageCallback)) {
            throw new CallbackQueueException("Callback for handling message with code '" + messageCode + "' wasn't added to queue");
        }

        return steamMessageCallback;
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
     *
     * @throws CallbackTimeoutException if the wait timed out
     */
    public void connect(long timeout) throws CallbackTimeoutException {
        var callback = addCallbackToQueue(ConnectedClientCallbackHandler.CALLBACK_MESSAGE_CODE);
        super.connect();
        ConnectedClientCallbackHandler.handle(callback, timeout);
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
     *
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
            onGcCallback.accept(packetMessage);
        } else {
            findAndCompleteCallback(packetMessage.getMessageType().code(), packetMessage);
        }
        return true;
    }

    /**
     * After connection with Steam Network server will be established this callback will be executed.
     */
    @Override
    protected void onClientConnected() {
        super.onClientConnected();
        findAndCompleteCallback(Constant.CONNECTED_PACKET_CODE, null);
    }

    /**
     * After connection with Steam Network server will be closed this callback will be executed.
     *
     * @param userInitiated whether the disconnect was initialized by the client
     */
    @Override
    protected void onClientDisconnected(boolean userInitiated) {
        super.onClientDisconnected(userInitiated);
        findAndCompleteCallback(Constant.DISCONNECTED_PACKET_CODE, null);
    }

    private void findAndCompleteCallback(int messageCode, PacketMessage packetMessage) {
        Optional<SteamMessageCallback<PacketMessage>> messageCallback = callbacksQueue.stream()
                .filter(callback -> callback.getMessageCode() == messageCode)
                .findFirst();

        messageCallback.ifPresent(callback -> {
            callbacksQueue.remove(callback);
            callback.getCallback().complete(packetMessage);
        });
    }
}
