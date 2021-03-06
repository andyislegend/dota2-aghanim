package com.avenga.steamclient.base;

import com.avenga.steamclient.generated.MsgGCHdr;
import com.avenga.steamclient.model.JobID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Objects;

/**
 * Represents a packet message with extended header information.
 */
public class GCPacketClientMessage implements GCPacketMessage {
    private static final Logger LOGGER = LoggerFactory.getLogger(GCPacketClientMessage.class);
    private int msgType;
    private JobID targetJobID;
    private JobID sourceJobID;
    private byte[] payload;

    /**
     * Initializes a new instance of the {@link GCPacketClientMessage} class.
     *
     * @param eMsg The network message type for this packet message.
     * @param data The data.
     */
    public GCPacketClientMessage(int eMsg, byte[] data) {
        Objects.requireNonNull(data, "data wasn't provided");

        msgType = eMsg;
        payload = data;

        MsgGCHdr gcHdr = new MsgGCHdr();

        // we need to pull out the job ids, so we deserialize the protobuf header
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data)) {
            gcHdr.deserialize(bais);
        } catch (IOException ex) {
            LOGGER.debug(ex.getMessage(), ex);
        }

        targetJobID = new JobID(gcHdr.getTargetJobID());
        sourceJobID = new JobID(gcHdr.getSourceJobID());
    }

    @Override
    public boolean isProto() {
        return false;
    }

    @Override
    public int getMessageType() {
        return msgType;
    }

    @Override
    public JobID getTargetJobID() {
        return targetJobID;
    }

    @Override
    public JobID getSourceJobID() {
        return sourceJobID;
    }

    @Override
    public byte[] getData() {
        return payload;
    }
}
