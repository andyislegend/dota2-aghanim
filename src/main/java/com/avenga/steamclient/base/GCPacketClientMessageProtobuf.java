package com.avenga.steamclient.base;

import com.avenga.steamclient.generated.MsgGCHdrProtoBuf;
import com.avenga.steamclient.model.JobID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Objects;

/**
 * Represents a protobuf backed packet message.
 */
public class GCPacketClientMessageProtobuf implements GCPacketMessage {
    private static final Logger LOGGER = LoggerFactory.getLogger(GCPacketClientMessageProtobuf.class);
    private int msgType;
    private JobID targetJobID;
    private JobID sourceJobID;
    private byte[] payload;

    /**
     * Initializes a new instance of the {@link GCPacketClientMessageProtobuf} class.
     *
     * @param eMsg The network message type for this packet message.
     * @param data The data.
     */
    public GCPacketClientMessageProtobuf(int eMsg, byte[] data) {
        Objects.requireNonNull(data, "data wasn't provided");

        msgType = eMsg;
        payload = data;

        MsgGCHdrProtoBuf protobufHeader = new MsgGCHdrProtoBuf();

        // we need to pull out the job ids, so we deserialize the protobuf header
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data)) {
            protobufHeader.deserialize(bais);
        } catch (IOException ex) {
            LOGGER.debug(ex.getMessage(), ex);
        }

        targetJobID = new JobID(protobufHeader.getProto().getJobidTarget());
        sourceJobID = new JobID(protobufHeader.getProto().getJobidSource());
    }

    @Override
    public boolean isProto() {
        return true;
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
