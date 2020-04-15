package com.avenga.steamclient.steam.asyncclient;

import com.avenga.steamclient.base.ClientMessageProtobuf;
import com.avenga.steamclient.base.PacketMessage;
import com.avenga.steamclient.enums.EMsg;
import com.avenga.steamclient.model.JobID;
import com.avenga.steamclient.model.configuration.SteamConfiguration;
import com.avenga.steamclient.model.steam.ClientMessageHandler;
import com.avenga.steamclient.protobufs.steamclient.SteammessagesClientserver.CMsgClientCMList;
import com.avenga.steamclient.steam.CMClient;
import com.avenga.steamclient.steam.asyncclient.callbackmanager.CallbackMessage;
import com.avenga.steamclient.steam.asyncclient.callbacks.CMListCallback;
import com.avenga.steamclient.steam.asyncclient.callbacks.ConnectedCallback;
import com.avenga.steamclient.steam.asyncclient.callbacks.DisconnectedCallback;
import com.avenga.steamclient.steam.asyncclient.steamgamecoordinator.SteamGameCoordinatorAsync;
import com.avenga.steamclient.steam.asyncclient.steamgameserver.SteamGameServerAsync;
import com.avenga.steamclient.steam.asyncclient.steamuser.SteamUserAsync;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class SteamClientAsync extends CMClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(SteamClientAsync.class);

    private Map<Class<? extends ClientMessageHandler>, ClientMessageHandler> handlers = new HashMap<>();

    private static final long DEFAULT_ID = 0;

    private AtomicLong currentJobId = new AtomicLong(DEFAULT_ID);

    private Instant processStartTime;

    private final Object callbackLock = new Object();

    private Queue<CallbackMessage> callbackQueue = new LinkedList<>();

    private Map<EMsg, Consumer<PacketMessage>> clientHandlers = new HashMap<>();

    /**
     * Initializes a new instance of the {@link SteamClientAsync} class with the default configuration.
     */
    public SteamClientAsync() {
        this(new SteamConfiguration());
    }

    /**
     * Initializes a new instance of the {@link SteamClientAsync} class with a specific configuration.
     *
     * @param configuration The configuration to use for this client.
     */
    public SteamClientAsync(SteamConfiguration configuration) {
        super(configuration);

        processStartTime = Instant.now();

        clientHandlers.put(EMsg.ClientCMList, this::handleCMList);

        addHandler(new SteamUserAsync());
        addHandler(new SteamGameServerAsync());
        addHandler(new SteamGameCoordinatorAsync());
    }

    /**
     * Posts a callback to the queue. This is normally used directly by client message handlers.
     *
     * @param callbackMessage The message.
     */
    public void postCallback(CallbackMessage callbackMessage) {
        if (callbackMessage == null) {
            return;
        }

        synchronized (callbackLock) {
            callbackQueue.offer(callbackMessage);
            callbackLock.notify();
        }
    }

    @Override
    public boolean onClientMsgReceived(PacketMessage packetMessage) {
        if (!super.onClientMsgReceived(packetMessage)) {
            return false;
        }

        var clientHandler = clientHandlers.get(packetMessage.getMessageType());
        if (clientHandler != null) {
            clientHandler.accept(packetMessage);
        }
        handlers.forEach((clazz, handler) -> handler.handleMessage(packetMessage));

        return true;
    }

    @Override
    protected void onClientDisconnected(boolean userInitiated) {
        super.onClientDisconnected(userInitiated);

        postCallback(new DisconnectedCallback(userInitiated));
    }

    @Override
    protected void onClientConnected() {
        super.onClientConnected();

        postCallback(new ConnectedCallback());
    }

    /**
     * Returns the next available JobID for job based messages.
     *
     * @return The next available JobID.
     */
    public JobID getNextJobID() {
        long sequence = currentJobId.incrementAndGet();

        JobID jobID = new JobID();
        jobID.setBoxID(DEFAULT_ID);
        jobID.setProcessID(DEFAULT_ID);
        jobID.setSequentialCount(sequence);
        jobID.setStartTime(processStartTime);

        return jobID;
    }

    /**
     * Adds a new handler to the internal list of message handlers.
     *
     * @param handler The handler to add.
     */
    public void addHandler(ClientMessageHandler handler) {
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
    public void removeHandler(Class<? extends ClientMessageHandler> handler) {
        handlers.remove(handler);
    }

    /**
     * Returns a registered handler.
     *
     * @param type The type of the handler to cast to. Must derive from ClientGCMessageHandler.
     * @param <T>  The type of the handler to cast to. Must derive from ClientGCMessageHandler.
     * @return A registered handler on success, or null if the handler could not be found.
     */
    @SuppressWarnings("unchecked")
    public <T extends ClientMessageHandler> T getHandler(Class<T> type) {
        return (T) handlers.get(type);
    }

    /**
     * Gets the next callback object in the queue, and optionally frees it.
     *
     * @param freeLast if set to <b>true</b> this function also frees the last callback if one existed.
     * @return The next callback in the queue, or null if no callback is waiting.
     */
    public CallbackMessage getCallback(boolean freeLast) {
        synchronized (callbackLock) {
            if (!callbackQueue.isEmpty()) {
                return freeLast ? callbackQueue.poll() : callbackQueue.peek();
            }
        }

        return null;
    }

    /**
     * Blocks the calling thread until a callback object is posted to the queue, and optionally frees it.
     *
     * @param freeLast if set to <b>true</b> this function also frees the last callback if one existed.
     * @return The callback object from the queue.
     */
    public CallbackMessage waitForCallback(boolean freeLast) {
        synchronized (callbackLock) {
            if (callbackQueue.isEmpty()) {
                try {
                    callbackLock.wait();
                } catch (final InterruptedException e) {
                    LOGGER.debug(e.getMessage(), e);
                }

                if (callbackQueue.isEmpty()) {
                    return null;
                }
            }

            return freeLast ? callbackQueue.poll() : callbackQueue.peek();
        }
    }

    /**
     * Blocks the calling thread until a callback object is posted to the queue, and optionally frees it.
     *
     * @param freeLast if set to <b>true</b> this function also frees the last callback if one existed.
     * @param timeout  The length of time to block.
     * @return A callback object from the queue if a callback has been posted, or null if the timeout has elapsed.
     */
    public CallbackMessage waitForCallback(boolean freeLast, long timeout) {
        synchronized (callbackLock) {
            if (callbackQueue.isEmpty()) {
                try {
                    callbackLock.wait(timeout);
                } catch (final InterruptedException e) {
                    LOGGER.debug(e.getMessage(), e);
                }
            }

            return freeLast ? callbackQueue.poll() : callbackQueue.peek();
        }
    }

    /**
     * Blocks the calling thread until the queue contains a callback object. Returns all callbacks, and optionally frees them.
     *
     * @param freeLast if set to <b>true</b> this function also frees all callbacks.
     * @param timeout  The length of time to block.
     * @return All current callback objects in the queue.
     */
    public List<CallbackMessage> getAllCallbacks(boolean freeLast, long timeout) {
        List<CallbackMessage> callbacks;

        synchronized (callbackLock) {
            if (callbackQueue.isEmpty()) {
                try {
                    callbackLock.wait(timeout);
                } catch (InterruptedException e) {
                    LOGGER.debug(e.getMessage(), e);
                }

                if (callbackQueue.isEmpty()) {
                    return new ArrayList<>();
                }
            }

            callbacks = new ArrayList<>(callbackQueue);

            if (freeLast) {
                callbackQueue.clear();
            }
        }

        return callbacks;
    }

    private void handleCMList(PacketMessage packetMessage) {
        ClientMessageProtobuf<CMsgClientCMList.Builder> cmMsg = new ClientMessageProtobuf<>(CMsgClientCMList.class, packetMessage);

        postCallback(new CMListCallback(cmMsg.getBody()));
    }
}
