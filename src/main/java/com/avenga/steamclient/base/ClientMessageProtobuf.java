package com.avenga.steamclient.base;

import com.avenga.steamclient.enums.EMsg;
import com.avenga.steamclient.enums.SeekOrigin;
import com.avenga.steamclient.generated.MsgHdrProtoBuf;
import com.avenga.steamclient.util.stream.BinaryReader;
import com.google.protobuf.AbstractMessage;
import com.google.protobuf.GeneratedMessageV3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Represents a protobuf backed client message.
 *
 * @param <BodyType> The body type of this message.
 */
public class ClientMessageProtobuf<BodyType extends GeneratedMessageV3.Builder<BodyType>> extends HeaderClientMessageProtobuf {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientMessageProtobuf.class);

    private static final int PAYLOAD_RESERVE = 64;

    private BodyType body;

    private final Class<? extends AbstractMessage> clazz;

    /**
     * Initializes a new instance of the {@link ClientMessageProtobuf} class.
     * This is a client send constructor.
     *
     * @param clazz          the type of the body
     * @param message            The network message type this client message represents.
     * @param payloadReserve The number of bytes to initialize the payload capacity to.
     */
    public ClientMessageProtobuf(Class<? extends AbstractMessage> clazz, PacketMessage message, int payloadReserve) {
        this(clazz, message.getMessageType(), payloadReserve);
    }

    /**
     * Initializes a new instance of the {@link ClientMessageProtobuf} class.
     * This is a client send constructor.
     *
     * @param clazz the type of the body
     * @param message   The network message type this client message represents.
     */
    public ClientMessageProtobuf(Class<? extends AbstractMessage> clazz, PacketMessage message) {
        this(clazz, message, PAYLOAD_RESERVE);
        if (!message.isProto()) {
            LOGGER.debug("ClientMsgProtobuf<" + clazz.getSimpleName() + "> used for non-proto message!");
        }
        deserialize(message.getData());
    }

    /**
     * Initializes a new instance of the {@link ClientMessageProtobuf} class.
     * This is a client send constructor.
     *
     * @param clazz the type of the body
     * @param eMsg  The network message type this client message represents.
     */
    public ClientMessageProtobuf(Class<? extends AbstractMessage> clazz, EMsg eMsg) {
        this(clazz, eMsg, PAYLOAD_RESERVE);
    }

    /**
     * Initializes a new instance of the {@link ClientMessageProtobuf} class.
     * This is a client send constructor.
     *
     * @param clazz          the type of the body
     * @param eMsg           The network message type this client message represents.
     * @param payloadReserve The number of bytes to initialize the payload capacity to.
     */
    public ClientMessageProtobuf(Class<? extends AbstractMessage> clazz, EMsg eMsg, int payloadReserve) {
        super(payloadReserve);
        this.clazz = clazz;

        try {
            final Method m = clazz.getMethod("newBuilder");
            body = (BodyType) m.invoke(null);
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            LOGGER.debug(e.getMessage(), e);
        }

        getHeader().setEMsg(eMsg);
    }

    /**
     * Initializes a new instance of the {@link ClientMessageProtobuf} class.
     * This is a reply constructor.
     *
     * @param clazz the type of the body
     * @param eMsg  The network message type this client message represents.
     * @param message   The message that this instance is a reply for.
     */
    public ClientMessageProtobuf(Class<? extends AbstractMessage> clazz, EMsg eMsg, BaseMessage<MsgHdrProtoBuf> message) {
        this(clazz, eMsg, message, PAYLOAD_RESERVE);
    }

    /**
     * Initializes a new instance of the {@link ClientMessageProtobuf} class.
     * This is a reply constructor.
     *
     * @param clazz          the type of the body
     * @param eMsg           The network message type this client message represents.
     * @param message            The message that this instance is a reply for.
     * @param payloadReserve The number of bytes to initialize the payload capacity to.
     */
    public ClientMessageProtobuf(Class<? extends AbstractMessage> clazz, EMsg eMsg, BaseMessage<MsgHdrProtoBuf> message,
                                 int payloadReserve) {
        this(clazz, eMsg, payloadReserve);
        // our target is where the message came from
        getHeader().getProto().setJobidTarget(message.getHeader().getProto().getJobidSource());
    }

    /**
     * @return the body structure of this message.
     */
    public BodyType getBody() {
        return body;
    }

    @Override
    public byte[] serialize() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(0);

        try {
            getHeader().serialize(outputStream);
            outputStream.write(body.build().toByteArray());
            outputStream.write(payload.toByteArray());
        } catch (IOException e) {
            LOGGER.debug(e.getMessage(), e);
        }
        return outputStream.toByteArray();
    }

    @Override
    public void deserialize(byte[] data) {
        Objects.requireNonNull(data, "data wasn't provided");
        BinaryReader reader = new BinaryReader(new ByteArrayInputStream(data));

        try {
            getHeader().deserialize(reader);
            final Method m = clazz.getMethod("newBuilder");
            body = (BodyType) m.invoke(null);
            body.mergeFrom(reader);
            payload.write(data, reader.getPosition(), reader.available());
            payload.seek(0, SeekOrigin.BEGIN);
        } catch (IOException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            LOGGER.debug(e.getMessage(), e);
        }

    }
}
