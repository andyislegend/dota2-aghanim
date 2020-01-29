package com.avenga.steamclient.steam.client;

import com.avenga.steamclient.base.PacketMessage;
import com.avenga.steamclient.enums.EMsg;
import com.avenga.steamclient.model.configuration.SteamConfiguration;
import com.avenga.steamclient.steam.CMClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class SteamClient extends CMClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(SteamClient.class);

    private BlockingQueue<PacketMessage> receivedMessages = new LinkedBlockingQueue<>();
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
    }

    public Optional<PacketMessage> getResponse(EMsg messageType, int timeout) {
        Optional<PacketMessage> packetMessage = Optional.empty();
        var timeoutTime = ZonedDateTime.now().get(ChronoField.MILLI_OF_SECOND) + timeout;
        while (ZonedDateTime.now().get(ChronoField.MILLI_OF_SECOND) < timeoutTime) {
            packetMessage = Optional.ofNullable(receivedMessages.poll());
            if (packetMessage.isPresent() && packetMessage.get().getMessageType().code() == messageType.code()) {
                break;
            }
        }

        return packetMessage;
    }

    @Override
    public boolean onClientMsgReceived(PacketMessage packetMessage) {
        if (!super.onClientMsgReceived(packetMessage)) {
           return false;
        }

        return receivedMessages.offer(packetMessage);
    }

    @Override
    protected void onClientConnected() {
        super.onClientConnected();
    }

    @Override
    protected void onClientDisconnected(boolean userInitiated) {
        super.onClientDisconnected(userInitiated);
    }
}
