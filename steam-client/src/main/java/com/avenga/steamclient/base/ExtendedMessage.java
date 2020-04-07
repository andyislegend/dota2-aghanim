package com.avenga.steamclient.base;

import com.avenga.steamclient.enums.EMsg;
import com.avenga.steamclient.enums.SeekOrigin;
import com.avenga.steamclient.generated.ExtendedClientMsgHdr;
import com.avenga.steamclient.model.JobID;
import com.avenga.steamclient.model.SteamID;
import com.avenga.steamclient.util.stream.MemoryStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

/**
 * Represents a struct backed client message.
 *
 * @param <BodyType> The body type of this message.
 */
public class ExtendedMessage<BodyType extends SteamSerializableMessage> extends BaseMessage<ExtendedClientMsgHdr> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExtendedMessage.class);

    private static final int DEFAULT_RESERVED_PAYLOAD = 64;

    private BodyType body;

    /**
     * Initializes a new instance of the {@link ExtendedMessage} class.
     *
     * @param bodyType body type
     */
    public ExtendedMessage(Class<? extends BodyType> bodyType) {
        this(bodyType, DEFAULT_RESERVED_PAYLOAD);
    }

    /**
     * Initializes a new instance of the {@link ExtendedMessage} class.
     *
     * @param bodyType       body type
     * @param payloadReserve The number of bytes to initialize the payload capacity to.
     */
    public ExtendedMessage(Class<? extends BodyType> bodyType, int payloadReserve) {
        super(ExtendedClientMsgHdr.class, payloadReserve);

        try {
            body = bodyType.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            LOGGER.debug(e.getMessage(), e);
        }

        getHeader().setEMsg(body.getEMsg());
    }

    /**
     * Initializes a new instance of the {@link ExtendedMessage} class.
     * This a reply constructor.
     *
     * @param bodyType body type
     * @param message      The message that this instance is a reply for.
     */
    public ExtendedMessage(Class<? extends BodyType> bodyType, BaseMessage<ExtendedClientMsgHdr> message) {
        this(bodyType, message, DEFAULT_RESERVED_PAYLOAD);
    }

    /**
     * Initializes a new instance of the {@link ExtendedMessage} class.
     * This a reply constructor.
     *
     * @param bodyType       body type
     * @param message            The message that this instance is a reply for.
     * @param payloadReserve The number of bytes to initialize the payload capacity to.
     */
    public ExtendedMessage(Class<? extends BodyType> bodyType, BaseMessage<ExtendedClientMsgHdr> message, int payloadReserve) {
        this(bodyType, payloadReserve);

        Objects.requireNonNull(message, "Message wasn't provided");

        // our target is where the message came from
        getHeader().setTargetJobID(message.getHeader().getSourceJobID());
    }

    /**
     * Initializes a new instance of the {@link ExtendedMessage} class.
     * This a receive constructor.
     *
     * @param bodyType body type
     * @param message      The packet message to build this client message from.
     */
    public ExtendedMessage(Class<? extends BodyType> bodyType, PacketMessage message) {
        this(bodyType);

        Objects.requireNonNull(message, "Message wasn't provided");

        if (message.isProto()) {
            LOGGER.debug("ClientMsg<" + bodyType.getName() + "> used for proto message!");
        }

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
        return getHeader().getSessionID();
    }

    @Override
    public void setSessionID(int sessionID) {
        getHeader().setSessionID(sessionID);
    }

    @Override
    public SteamID getSteamID() {
        return getHeader().getSteamID();
    }

    @Override
    public void setSteamID(SteamID steamID) {
        Objects.requireNonNull(steamID, "Steam ID wasn't provided");

        getHeader().setSteamID(steamID);
    }

    @Override
    public JobID getTargetJobID() {
        return new JobID(getHeader().getTargetJobID());
    }

    @Override
    public void setTargetJobID(JobID jobID) {
        Objects.requireNonNull(jobID, "Job ID wasn't provided");

        getHeader().setTargetJobID(jobID.getValue());
    }

    @Override
    public JobID getSourceJobID() {
        return new JobID(getHeader().getSourceJobID());
    }

    @Override
    public void setSourceJobID(JobID jobID) {
        Objects.requireNonNull(jobID, "Job ID wasn't provided");

        getHeader().setSourceJobID(jobID.getValue());
    }

    @Override
    public byte[] serialize() {
        try(ByteArrayOutputStream baos = new ByteArrayOutputStream(0)) {
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
        Objects.requireNonNull(data, "Data byte array wasn't provided");

        try (MemoryStream ms = new MemoryStream(data)) {
            getHeader().deserialize(ms);
            body.deserialize(ms);

            payload.write(data, (int) ms.getPosition(), ms.available());
            payload.seek(0, SeekOrigin.BEGIN);
        } catch (IOException e) {
            LOGGER.debug(e.getMessage(), e);
        }
    }

    /**
     * @return the body structure of this message.
     */
    public BodyType getBody() {
        return body;
    }
}
