package com.avenga.steamclient.steam.asyncclient.steamgamecoordinator;

import com.avenga.steamclient.base.ClientGCMessage;
import com.avenga.steamclient.base.ClientGCProtobufMessage;
import com.avenga.steamclient.base.ClientMessageProtobuf;
import com.avenga.steamclient.base.PacketMessage;
import com.avenga.steamclient.enums.EMsg;
import com.avenga.steamclient.model.steam.ClientMessageHandler;
import com.avenga.steamclient.model.steam.gamecoordinator.ClientGCMessageHandler;
import com.avenga.steamclient.model.steam.gamecoordinator.GCMessage;
import com.avenga.steamclient.protobufs.dota.GCSdkGCMessages.CMsgClientHello;
import com.avenga.steamclient.protobufs.dota.GCSdkGCMessages.ESourceEngine;
import com.avenga.steamclient.protobufs.steamclient.SteammessagesClientserver2.CMsgGCClient;
import com.avenga.steamclient.steam.asyncclient.steamgamecoordinator.dota.DotaClientAsync;
import com.avenga.steamclient.steam.asyncclient.steamgamecoordinator.dota.callback.ClientWelcomeCallback;
import com.avenga.steamclient.util.MessageUtil;
import com.avenga.steamclient.util.SteamEnumUtils;
import com.google.protobuf.ByteString;
import com.google.protobuf.ProtocolMessageEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import static com.avenga.steamclient.protobufs.tf.GCSystemMessages.EGCBaseClientMsg.k_EMsgGCClientHello;

/**
 * This handler handles all game coordinator messaging.
 */
public class SteamGameCoordinatorAsync extends ClientMessageHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SteamGameCoordinatorAsync.class);

    private Map<Class<? extends ClientGCMessageHandler>, ClientGCMessageHandler> gcHandlers = new HashMap<>();

    private Map<EMsg, Consumer<PacketMessage>> clientHandlers;

    public SteamGameCoordinatorAsync() {
        clientHandlers = Map.of(EMsg.ClientFromGC, this::handleFromGC);

        addHandler(new DotaClientAsync());
    }

    /**
     * Sends a game coordinator message for a specific applicationId.
     *
     * @param gcMessage   The GC message to send.
     * @param applicationId The application id of the game coordinator to send to.
     */
    public void send(ClientGCMessage gcMessage, int applicationId, ProtocolMessageEnum messageEnum) {
        Objects.requireNonNull(gcMessage, "GC message wasn't provided");

        LOGGER.debug("Sent GC -> EMsg: {} (id: {})", messageEnum.getValueDescriptor().getName(),
                messageEnum.getNumber());

        ClientMessageProtobuf<CMsgGCClient.Builder> clientMsg = new ClientMessageProtobuf<>(CMsgGCClient.class, EMsg.ClientToGC);
        clientMsg.getProtoHeader().setRoutingAppid(applicationId);
        clientMsg.getBody().setMsgtype(MessageUtil.makeGCMsg(messageEnum.getNumber(), gcMessage.isProto()));
        clientMsg.getBody().setAppid(applicationId);
        clientMsg.getBody().setPayload(ByteString.copyFrom(gcMessage.serialize()));

        client.send(clientMsg);
    }

    /**
     * Sends a game coordinator message for a specific applicationId.
     * Results are returned in a {@link ClientWelcomeCallback} callback.
     *
     * @param sourceEngine The Source engine of the game.
     * @param applicationId The application id of the game coordinator to send to.
     */
    public void sendClientHello(ESourceEngine sourceEngine, int applicationId) {
        Objects.requireNonNull(sourceEngine, "Source engine wasn't provided");

        var clientHelloMessage = new ClientGCProtobufMessage<CMsgClientHello.Builder>(CMsgClientHello.class, k_EMsgGCClientHello.getNumber());
        clientHelloMessage.getBody().setEngine(sourceEngine);
        this.send(clientHelloMessage, applicationId, k_EMsgGCClientHello);
    }

    @Override
    public void handleMessage(PacketMessage packetMessage) {
        Objects.requireNonNull(packetMessage, "Packet message wasn't provided");

        Consumer<PacketMessage> dispatcher = clientHandlers.get(packetMessage.getMessageType());
        if (dispatcher != null) {
            dispatcher.accept(packetMessage);
        }
    }

    /**
     * Adds a new handler to the internal list of message handlers.
     *
     * @param handler The handler to add.
     */
    public void addHandler(ClientGCMessageHandler handler) {
        if (gcHandlers.containsKey(handler.getClass())) {
            throw new IllegalArgumentException("A handler of type " + handler.getClass() + " is already registered.");
        }

        handler.setup(this);
        gcHandlers.put(handler.getClass(), handler);
    }

    /**
     * Removes a registered handler by name.
     *
     * @param handler The handler name to remove.
     */
    public void removeHandler(Class<? extends ClientGCMessageHandler> handler) {
        gcHandlers.remove(handler);
    }

    /**
     * Returns a registered handler.
     *
     * @param type The type of the GameCoordinator handler to cast to. Must derive from ClientGCMessageHandler.
     * @param <T>  The type of the GameCoordinator handler to cast to. Must derive from ClientGCMessageHandler.
     * @return A registered handler on success, or null if the handler could not be found.
     */
    @SuppressWarnings("unchecked")
    public <T extends ClientGCMessageHandler> T getHandler(Class<T> type) {
        return (T) gcHandlers.get(type);
    }

    private void handleFromGC(PacketMessage packetMessage) {
        ClientMessageProtobuf<CMsgGCClient.Builder> msg = new ClientMessageProtobuf<>(CMsgGCClient.class, packetMessage);

        var gcMessage = new GCMessage(msg.getBody());

        LOGGER.debug("<- Recv'd GC EMsg: {} ({}) (Proto: {}) (AppId: {})",
                SteamEnumUtils.getEnumName(gcMessage.geteMsg()).orElse(""), gcMessage.geteMsg(),
                gcMessage.isProto(), gcMessage.getApplicationID());

        gcHandlers.forEach((clazz, handler) -> {
            if (handler.getApplicationId() == gcMessage.getApplicationID()) {
                handler.handleMessage(gcMessage);
            }
        });
    }
}
