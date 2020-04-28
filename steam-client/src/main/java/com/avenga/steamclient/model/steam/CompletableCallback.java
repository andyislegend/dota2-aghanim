package com.avenga.steamclient.model.steam;

import com.avenga.steamclient.base.GCPacketMessage;
import com.avenga.steamclient.base.PacketMessage;
import com.avenga.steamclient.steam.client.SteamClient;

import java.time.Instant;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

/**
 * Completable callback of the {@link SteamClient} to handle received packet messages from Steam Network or Game Coordinator server.
 */
public interface CompletableCallback {

    /**
     * Gets sequence number of the registered callback.
     *
     * @return sequence number.
     */
    int getSequence();

    /**
     * Gets code of the packet message received from Steam Network, which should be handled by this callback.
     *
     * @return Steam packet message code.
     */
    int getMessageCode();

    /**
     * Gets application id of the registered appliction in Game Coordinator or default id of the client for base Steam packet messages.
     *
     * @return Steam packet message code.
     */
    int getApplicationId();

    /**
     * Gets Id of the job ID set in the header of the packet message.
     *
     * @return Id of the job stored in packet message header.
     */
    long getJobId();

    /**
     * Gets time of the callback creation.
     *
     * @return callback creation time.
     */
    Instant getCreatedAt();

    /**
     * Gets additional registered properties of the callback.
     *
     * @return propertioes of the callback.
     */
    Properties getProperties();

    /**
     * Completes {@link CompletableFuture} of the callback using correct type: {@link PacketMessage} for Steam Network
     * and {@link GCPacketMessage} for Game Coordinator messages
     *
     * @param packetMessage Base Steam Netwokr message.
     */
    void complete(Object packetMessage);

    /**
     * Cancel {@link CompletableFuture} of the callback.
     */
    void cancel();
}
