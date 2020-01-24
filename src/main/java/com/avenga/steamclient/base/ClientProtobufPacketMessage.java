package com.avenga.steamclient.base;

import com.avenga.steamclient.enums.EMsg;
import com.avenga.steamclient.generated.MsgHdrProtoBuf;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Represents a protobuf backed packet message.
 */
public class ClientProtobufPacketMessage implements PacketMessage {

    private EMsg messageType;

    private long targetJobID;

    private long sourceJobID;

    private byte[] payload;

    /**
     * Initializes a new instance of the {@link ClientProtobufPacketMessage} class.
     *
     * @param eMsg The network message type for this packet message.
     * @param data The data.
     * @throws IOException exception while deserializing the data
     */
    public ClientProtobufPacketMessage(EMsg eMsg, byte[] data) throws IOException {
        this.messageType = eMsg;
        this.payload = data;

        MsgHdrProtoBuf protobufHeader = new MsgHdrProtoBuf();

        try (ByteArrayInputStream stream = new ByteArrayInputStream(data)) {
            protobufHeader.deserialize(stream);
        }

        targetJobID = protobufHeader.getProto().getJobidTarget();
        sourceJobID = protobufHeader.getProto().getJobidSource();
    }

    @Override
    public boolean isProto() {
        return true;
    }

    @Override
    public EMsg getMessageType() {
        return messageType;
    }

    @Override
    public long getTargetJobID() {
        return targetJobID;
    }

    @Override
    public long getSourceJobID() {
        return sourceJobID;
    }

    @Override
    public byte[] getData() {
        return payload;
    }
}
