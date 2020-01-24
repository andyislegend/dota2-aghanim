package com.avenga.steamclient.base;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;

/**
 * This is the abstract base class for all available client messages.
 * It's used to maintain packet payloads and provide a header for all client messages.
 */
public abstract class BaseMessage<HdrType extends SteamSerializable> extends AbstractMessage implements ClientMessage {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseMessage.class);

    private HdrType header;

    /**
     * Initializes a new instance of the {@link BaseMessage} class.
     *
     * @param clazz the type of the header
     */
    public BaseMessage(Class<HdrType> clazz) {
        this(clazz, DEFAULT_PAYLOAD_RESERVE);
    }

    /**
     * Initializes a new instance of the {@link BaseMessage} class.
     *
     * @param clazz          the type of the header
     * @param payloadReserve The number of bytes to initialize the payload capacity to.
     */
    public BaseMessage(Class<HdrType> clazz, int payloadReserve) {
        super(payloadReserve);
        try {
            header = clazz.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            LOGGER.debug(e.getMessage(), e);
        }
    }

    /**
     * @return the header for this message type.
     */
    public HdrType getHeader() {
        return header;
    }
}
