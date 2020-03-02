package com.avenga.steamclient.base;

import com.avenga.steamclient.enums.EMsg;
import com.avenga.steamclient.generated.MsgHdr;
import com.avenga.steamclient.model.JobID;
import com.avenga.steamclient.model.SteamID;
import com.avenga.steamclient.util.stream.MemoryStream;
import com.avenga.steamclient.enums.SeekOrigin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

/**
 * Represents a struct backed message without session or client info.
 */
public class Message<BodyType extends SteamSerializableMessage> extends BaseMessage<MsgHdr> {

    private static final Logger LOGGER = LoggerFactory.getLogger(Message.class);

    private BodyType body;

    /**
     * Initializes a new instance of the {@link Message} class.
     *
     * @param bodyType body type
     */
    public Message(Class<? extends BodyType> bodyType) {
        this(bodyType, DEFAULT_PAYLOAD_RESERVE);
    }

    /**
     * Initializes a new instance of the {@link Message} class.
     *
     * @param bodyType       body type
     * @param payloadReserve The number of bytes to initialize the payload capacity to.
     */
    public Message(Class<? extends BodyType> bodyType, int payloadReserve) {
        super(MsgHdr.class, payloadReserve);

        try {
            body = bodyType.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            LOGGER.debug(e.getMessage(), e);
        }

        getHeader().setEMsg(body.getEMsg());
    }

    /**
     * Initializes a new instance of the {@link Message} class.
     * This a reply constructor.
     *
     * @param bodyType body type
     * @param msg      The message that this instance is a reply for.
     */
    public Message(Class<? extends BodyType> bodyType, BaseMessage<MsgHdr> msg) {
        this(bodyType, msg, DEFAULT_PAYLOAD_RESERVE);
    }

    /**
     * Initializes a new instance of the {@link Message} class.
     * This a reply constructor.
     *
     * @param bodyType       body type
     * @param message            The message that this instance is a reply for.
     * @param payloadReserve The number of bytes to initialize the payload capacity to.
     */
    public Message(Class<? extends BodyType> bodyType, BaseMessage<MsgHdr> message, int payloadReserve) {
        this(bodyType, payloadReserve);

        Objects.requireNonNull(message, "message wasn't provided");

        // our target is where the message came from
        getHeader().setTargetJobID(message.getHeader().getSourceJobID());
    }

    /**
     * Initializes a new instance of the {@link Message} class.
     * This a receive constructor.
     *
     * @param bodyType body type
     * @param message      The packet message to build this client message from.
     */
    public Message(Class<? extends BodyType> bodyType, PacketMessage message) {
        this(bodyType);

        Objects.requireNonNull(message, "message wasn't provided");

        deserialize(message.getData());
    }

    @Override
    public boolean isProto() {
        return false;
    }

    @Override
    public EMsg getMsgType() {
        return getHeader().getMsg();
    }

    @Override
    public int getSessionID() {
        return 0;
    }

    @Override
    public void setSessionID(int sessionID) {
    }

    @Override
    public SteamID getSteamID() {
        return null;
    }

    @Override
    public void setSteamID(SteamID steamID) {
    }

    @Override
    public JobID getTargetJobID() {
        return new JobID(getHeader().getTargetJobID());
    }

    @Override
    public void setTargetJobID(JobID jobID) {
        Objects.requireNonNull(jobID, "jobID wasn't provided");

        getHeader().setTargetJobID(jobID.getValue());
    }

    @Override
    public JobID getSourceJobID() {
        return new JobID(getHeader().getSourceJobID());
    }

    @Override
    public void setSourceJobID(JobID jobID) {
        Objects.requireNonNull(jobID, "jobID wasn't provided");

        getHeader().setSourceJobID(jobID.getValue());
    }

    @Override
    public byte[] serialize() {
        try (var baos = new ByteArrayOutputStream(0)) {
            getHeader().serialize(baos);
            body.serialize(baos);
            baos.write(payload.toByteArray());
            return baos.toByteArray();
        } catch (IOException e) {
            LOGGER.debug(e.getMessage(), e);
            return new byte[]{};
        }
    }

    @Override
    public void deserialize(byte[] data) {
        Objects.requireNonNull(data, "data wasn't provided");

        try (var ms = new MemoryStream(data)) {
            getHeader().deserialize(ms);
            body.deserialize(ms);

            payload.write(data, (int) ms.getPosition(), ms.available());
            payload.seek(0, SeekOrigin.BEGIN);
        } catch (IOException e) {
            LOGGER.debug(e.getMessage(), e);
        }
    }

    public BodyType getBody() {
        return body;
    }
}
