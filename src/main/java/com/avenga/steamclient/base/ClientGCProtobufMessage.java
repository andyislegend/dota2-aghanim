package com.avenga.steamclient.base;

import com.avenga.steamclient.enums.SeekOrigin;
import com.avenga.steamclient.generated.MsgGCHdrProtoBuf;
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

public class ClientGCProtobufMessage<BodyType extends GeneratedMessageV3.Builder<BodyType>> extends HeaderClientGCProtobufMessage {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientGCProtobufMessage.class);
    private static final int PAYLOAD_RESERVE = 64;
    private BodyType body;
    private final Class<? extends AbstractMessage> clazz;

    /**
     * Initializes a new instance of the {@link ClientGCProtobufMessage} class.
     * This is a client send constructor.
     *
     * @param clazz the type of the body
     * @param msg   The network message type this client message represents.
     */
    public ClientGCProtobufMessage(Class<? extends AbstractMessage> clazz, GCPacketMessage msg) {
        this(clazz, msg.getMessageType());
        if (!msg.isProto()) {
            LOGGER.debug("ClientMsgProtobuf<" + clazz.getSimpleName() + "> used for non-proto message!");
        }
        deserialize(msg.getData());
    }

    /**
     * Initializes a new instance of the {@link ClientGCProtobufMessage} class.
     * This is a client send constructor.
     *
     * @param clazz the type of the body
     * @param eMsg  The network message type this client message represents.
     */
    public ClientGCProtobufMessage(Class<? extends AbstractMessage> clazz, int eMsg) {
        this(clazz, eMsg, 64);
    }

    /**
     * Initializes a new instance of the {@link ClientGCProtobufMessage} class.
     * This is a client send constructor.
     *
     * @param clazz          the type of the body
     * @param eMsg           The network message type this client message represents.
     * @param payloadReserve The number of bytes to initialize the payload capacity to.
     */
    public ClientGCProtobufMessage(Class<? extends AbstractMessage> clazz, int eMsg, int payloadReserve) {
        super(MsgGCHdrProtoBuf.class, payloadReserve);
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
     * Initializes a new instance of the {@link ClientGCProtobufMessage} class.
     * This is a reply constructor.
     *
     * @param clazz the type of the body
     * @param eMsg  The network message type this client message represents.
     * @param msg   The message that this instance is a reply for.
     */
    public ClientGCProtobufMessage(Class<? extends AbstractMessage> clazz, int eMsg, GCBaseMessage<MsgGCHdrProtoBuf> msg) {
        this(clazz, eMsg, msg, PAYLOAD_RESERVE);
    }

    /**
     * Initializes a new instance of the {@link ClientGCProtobufMessage} class.
     * This is a reply constructor.
     *
     * @param clazz          the type of the body
     * @param eMsg           The network message type this client message represents.
     * @param msg            The message that this instance is a reply for.
     * @param payloadReserve The number of bytes to initialize the payload capacity to.
     */
    public ClientGCProtobufMessage(Class<? extends AbstractMessage> clazz, int eMsg, GCBaseMessage<MsgGCHdrProtoBuf> msg, int payloadReserve) {
        this(clazz, eMsg, payloadReserve);

        if (msg == null) {
            throw new IllegalArgumentException("msg is null");
        }

        // our target is where the message came from
        getHeader().getProto().setJobidTarget(msg.getHeader().getProto().getJobidSource());
    }

    @Override
    public byte[] serialize() {
        byte[] result = null;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            getHeader().serialize(baos);
            body.build().writeTo(baos);
            baos.write(payload.toByteArray());
            result = baos.toByteArray();
        } catch (IOException ex) {
            LOGGER.debug(ex.getMessage(), ex);
        }
        return result;
    }

    @Override
    public void deserialize(byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("data is null");
        }
        try (BinaryReader ms = new BinaryReader(new ByteArrayInputStream(data))) {
            getHeader().deserialize(ms);
            final Method m = clazz.getMethod("newBuilder");
            body = (BodyType) m.invoke(null);
            body.mergeFrom(ms);
            payload.write(data, ms.getPosition(), ms.available());
            payload.seek(0, SeekOrigin.BEGIN);
        } catch (Exception ex) {
            LOGGER.debug(ex.getMessage(), ex);
        }
    }

    /**
     * @return the body structure of this message.
     */
    public BodyType getBody() {
        return body;
    }
}
