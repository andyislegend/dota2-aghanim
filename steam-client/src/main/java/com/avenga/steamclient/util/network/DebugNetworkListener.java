package com.avenga.steamclient.util.network;

import com.avenga.steamclient.enums.EMsg;

/**
 * This is a debug utility, do not use it to implement your business logic.
 * <p>
 * This interface is used for logging network messages sent to and received from the Steam server that the client is connected to.
 */
public interface DebugNetworkListener {

    /**
     * Called when a packet is received from the Steam server.
     *
     * @param msgType Network message type of this packet message.
     * @param data    Raw packet data that was received.
     */
    void onPacketMessageReceived(EMsg msgType, byte[] data);

    /**
     * Called when a packet is about to be sent to the Steam server.
     *
     * @param msgType Network message type of this packet message.
     * @param data    Raw packet data that was received.
     */
    void onPacketMessageSent(EMsg msgType, byte[] data);
}
