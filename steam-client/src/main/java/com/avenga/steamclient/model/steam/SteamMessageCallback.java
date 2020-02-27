package com.avenga.steamclient.model.steam;

import com.avenga.steamclient.base.GCPacketMessage;
import com.avenga.steamclient.base.PacketMessage;
import com.avenga.steamclient.steam.client.SteamClient;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.concurrent.CompletableFuture;

/**
 * Callback wrapper for {@link SteamClient} to handle client {@link PacketMessage} and Game Coordinator {@link GCPacketMessage}
 * message received from Steam Network.
 *
 * @param <TPacket> Type of the packet message received from Steam Network.
 */
@Getter
@EqualsAndHashCode(callSuper = false)
public class SteamMessageCallback<TPacket> implements CompletableCallback {
    private int sequence;
    private int applicationId;
    private int messageCode;
    private CompletableFuture<TPacket> callback;


    /**
     * Creates steam callback wrapper for packet messages.
     *
     * @param messageCode Code of the packet message.
     * @param applicationId Application id of the Steam client or games.
     * @param sequence Sequence number of the callback.
     * @param callback Completable callback which should handle correct packet message.
     */
    public SteamMessageCallback(int messageCode, int applicationId, int sequence, CompletableFuture<TPacket> callback) {
        this.sequence = sequence;
        this.applicationId = applicationId;
        this.messageCode = messageCode;
        this.callback = callback;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void complete(Object genericPacketMessage) {
        TPacket packetMessage = (TPacket) genericPacketMessage;
        callback.complete(packetMessage);
    }
}
