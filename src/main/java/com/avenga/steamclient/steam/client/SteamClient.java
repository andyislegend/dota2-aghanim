package com.avenga.steamclient.steam.client;

import com.avenga.steamclient.base.PacketMessage;
import com.avenga.steamclient.constant.Constant;
import com.avenga.steamclient.exception.CallbackQueueException;
import com.avenga.steamclient.model.configuration.SteamConfiguration;
import com.avenga.steamclient.model.steam.SteamMessageCallback;
import com.avenga.steamclient.steam.CMClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.*;

public class SteamClient extends CMClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(SteamClient.class);

    private BlockingQueue<SteamMessageCallback> callbacksQueue = new LinkedBlockingQueue<>();
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

    public SteamMessageCallback addCallbackToQueue(int messageCode) {
        var steamMessageCallback = new SteamMessageCallback(messageCode);

        if (!callbacksQueue.offer(steamMessageCallback)) {
            throw new CallbackQueueException("Callback for handling message with code '" + messageCode + "' wasn't added to queue");
        }

        return steamMessageCallback;
    }

    @Override
    public boolean onClientMsgReceived(PacketMessage packetMessage) {
        if (!super.onClientMsgReceived(packetMessage)) {
           return false;
        }

        findAndCompleteCallback(packetMessage.getMessageType().code(), packetMessage);

        return true;
    }

    @Override
    protected void onClientConnected() {
        super.onClientConnected();
        findAndCompleteCallback(Constant.CONNECTED_PACKET_CODE, null);
    }

    @Override
    protected void onClientDisconnected(boolean userInitiated) {
        super.onClientDisconnected(userInitiated);
        findAndCompleteCallback(Constant.DISCONNECTED_PACKET_CODE, null);
    }

    private void findAndCompleteCallback(int messageCode, PacketMessage packetMessage) {
        Optional<SteamMessageCallback> messageCallback = callbacksQueue.stream()
                .filter(callback -> callback.getMessageCode() == messageCode)
                .findFirst();

        messageCallback.ifPresent(callback -> {
            callbacksQueue.remove(callback);
            callback.getCallback().complete(packetMessage);
        });
    }
}
