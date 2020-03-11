package com.avenga.steamclient.steam.client.steamgamecoordinator;

import com.avenga.steamclient.base.ClientGCMessage;
import com.avenga.steamclient.base.ClientGCProtobufMessage;
import com.avenga.steamclient.base.ClientMessageProtobuf;
import com.avenga.steamclient.enums.EMsg;
import com.avenga.steamclient.exception.CallbackTimeoutException;
import com.avenga.steamclient.model.steam.ClientHandler;
import com.avenga.steamclient.model.steam.gamecoordinator.ClientGCHandler;
import com.avenga.steamclient.protobufs.dota.GCSdkGCMessages;
import com.avenga.steamclient.protobufs.dota.GCSdkGCMessages.CMsgClientHello;
import com.avenga.steamclient.protobufs.steamclient.SteammessagesClientserver2.CMsgGCClient;
import com.avenga.steamclient.steam.client.SteamClient;
import com.avenga.steamclient.steam.client.steamgamecoordinator.callback.GCHelloCallbackHandler;
import com.avenga.steamclient.steam.client.steamgamecoordinator.dota.DotaClient;
import com.avenga.steamclient.util.MessageUtil;
import com.avenga.steamclient.util.retry.RetryHandlerUtil;
import com.google.protobuf.ByteString;
import com.google.protobuf.ProtocolMessageEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.avenga.steamclient.protobufs.tf.GCSystemMessages.EGCBaseClientMsg.k_EMsgGCClientHello;
import static com.avenga.steamclient.protobufs.tf.GCSystemMessages.EGCBaseClientMsg.k_EMsgGCClientWelcome;

public class SteamGameCoordinator extends ClientHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SteamGameCoordinator.class);

    private static final int RETRY_COUNT = 3;

    private Map<Class<? extends ClientGCHandler>, ClientGCHandler> handlers = new HashMap<>();

    @Override
    public void setup(SteamClient client) {
        super.setup(client);

        addHandler(new DotaClient());
    }

    /**
     * Send message to Steam Game Coordinator server.
     *
     * @param message       Game Coordinator message.
     * @param applicationId ID of the application of the Steam Network.
     * @param messageEnum   Type of the provided Game Coordinator message.
     */
    public void send(ClientGCMessage message, int applicationId, ProtocolMessageEnum messageEnum) {
        Objects.requireNonNull(message, "Client Game Coordinator message wasn't provided");

        LOGGER.debug("Sent GC -> EMsg: {} (id: {})", messageEnum.getValueDescriptor().getName(), messageEnum.getNumber());

        var clientMsg = new ClientMessageProtobuf<CMsgGCClient.Builder>(CMsgGCClient.class, EMsg.ClientToGC);
        clientMsg.getProtoHeader().setRoutingAppid(applicationId);
        clientMsg.getBody().setMsgtype(MessageUtil.makeGCMsg(messageEnum.getNumber(), message.isProto()));
        clientMsg.getBody().setAppid(applicationId);
        clientMsg.getBody().setPayload(ByteString.copyFrom(message.serialize()));
        client.send(clientMsg);
    }

    /**
     * Send Hello message to the Game Coordinator server to initiate session.
     *
     * @param sourceEngine  Type of the source engine.
     * @param applicationId ID of the application of the Steam Network.
     * @param timeout       Time during which handler will wait for response.
     * @throws CallbackTimeoutException Will be thrown in case message won't be received within timeout
     *                                  period and several retries.
     */
    public void sendClientHello(GCSdkGCMessages.ESourceEngine sourceEngine, int applicationId, long timeout) throws CallbackTimeoutException {
        var gcSessionCallback = getClient().addGCCallbackToQueue(k_EMsgGCClientWelcome.getNumber(), applicationId);
        var clientHelloMessage = new ClientGCProtobufMessage<CMsgClientHello.Builder>(CMsgClientHello.class, k_EMsgGCClientHello.getNumber());
        clientHelloMessage.getBody().setEngine(sourceEngine);

        RetryHandlerUtil.getOrRetry(() -> {
            send(clientHelloMessage, applicationId, k_EMsgGCClientHello);
            return GCHelloCallbackHandler.handle(gcSessionCallback, timeout);
        }, gcSessionCallback, RETRY_COUNT, getClient());
    }

    /**
     * Adds a new handler to the internal list of Game Coordinator message handlers.
     *
     * @param handler The handler to add.
     */
    public void addHandler(ClientGCHandler handler) {
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
    public void removeHandler(Class<? extends ClientGCHandler> handler) {
        handlers.remove(handler);
    }

    /**
     * Returns a registered handler.
     *
     * @param type The type of the handler to cast to. Must derive from {@link ClientGCHandler}.
     * @param <T>  The type of the handler to cast to. Must derive from {@link ClientGCHandler}.
     * @return A registered handler on success, or null if the handler could not be found.
     */
    @SuppressWarnings("unchecked")
    public <T extends ClientGCHandler> T getHandler(Class<T> type) {
        return (T) handlers.get(type);
    }
}
