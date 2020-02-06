package com.avenga.steamclient.base;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the abstract base class for all available game coordinator messages.
 * It's used to maintain packet payloads and provide a header for all gc messages.
 *
 * @param <HdrType> The header type for this gc message.
 */
public abstract class GCBaseMessage<HdrType extends SteamSerializable> extends AbstractMessage implements ClientGCMessage {
    private static final Logger LOGGER = LoggerFactory.getLogger(GCBaseMessage.class);
    private static final int DEFAULT_PAYLOAD_RESERVE = 0;
    private HdrType header;

    /**
     * Initializes a new instance of the {@link GCBaseMessage} class.
     *
     * @param clazz the type of the header
     */
    public GCBaseMessage(Class<HdrType> clazz) {
        this(clazz, DEFAULT_PAYLOAD_RESERVE);
    }

    /**
     * Initializes a new instance of the {@link GCBaseMessage} class.
     *
     * @param clazz          the type of the header
     * @param payloadReserve The number of bytes to initialize the payload capacity to.
     */
    public GCBaseMessage(Class<HdrType> clazz, int payloadReserve) {
        super(payloadReserve);
        try {
            header = clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
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
