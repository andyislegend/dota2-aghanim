package com.avenga.steamclient.base;

import com.avenga.steamclient.enums.EMsg;
import com.avenga.steamclient.generated.ExtendedClientMsgHdr;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Represents a packet message with extended header information.
 */
public class ClientPacketMessage implements PacketMessage {

    private EMsg messageType;

    private long targetJobID;

    private long sourceJobID;

    private byte[] payload;

    /**
     * Initializes a new instance of the {@link ClientPacketMessage} class.
     *
     * @param eMsg The network message type for this packet message.
     * @param data The data.
     * @throws IOException exception while deserializing the data
     */
    public ClientPacketMessage(EMsg eMsg, byte[] data) throws IOException {
        this.messageType = eMsg;
        this.payload = data;

        ExtendedClientMsgHdr extendedHdr = new ExtendedClientMsgHdr();

        try (ByteArrayInputStream stream = new ByteArrayInputStream(data)) {
            extendedHdr.deserialize(stream);
        }

        targetJobID = extendedHdr.getTargetJobID();
        sourceJobID = extendedHdr.getSourceJobID();
    }

    @Override
    public boolean isProto() {
        return false;
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
