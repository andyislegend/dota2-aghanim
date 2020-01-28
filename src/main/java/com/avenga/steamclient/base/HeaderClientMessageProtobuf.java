package com.avenga.steamclient.base;

import com.avenga.steamclient.enums.EMsg;
import com.avenga.steamclient.generated.MsgHdrProtoBuf;
import com.avenga.steamclient.model.JobID;
import com.avenga.steamclient.model.SteamID;
import com.avenga.steamclient.protobufs.steamclient.SteammessagesBase.CMsgProtoBufHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Represents a protobuf backed client message. Only contains the header information.
 */
public class HeaderClientMessageProtobuf extends BaseMessage<MsgHdrProtoBuf> {

    private static final Logger LOGGER = LoggerFactory.getLogger(HeaderClientMessageProtobuf.class);

    /**
     * Initializes a new instance of the{@link HeaderClientMessageProtobuf} class.
     * This is a recieve constructor.
     *
     * @param packetMessage The packet message to build this client message from.
     */
    public HeaderClientMessageProtobuf(PacketMessage packetMessage) {
        this(packetMessage.getMessageType());

        if (!packetMessage.isProto()) {
            LOGGER.debug("ClientMsgProtobuf used for non-proto message!");
        }

        deserialize(packetMessage.getData());
    }

    private HeaderClientMessageProtobuf() {
        this(DEFAULT_PAYLOAD_RESERVE);
    }

    HeaderClientMessageProtobuf(int payloadReserve) {
        super(MsgHdrProtoBuf.class, payloadReserve);
    }

    private HeaderClientMessageProtobuf(EMsg eMsg) {
        this(eMsg, DEFAULT_PAYLOAD_RESERVE);
    }

    private HeaderClientMessageProtobuf(EMsg eMsg, int payloadReserve) {
        super(MsgHdrProtoBuf.class, payloadReserve);
        getHeader().setEMsg(eMsg);
    }

    public CMsgProtoBufHeader.Builder getProtoHeader() {
        return getHeader().getProto();
    }

    @Override
    public boolean isProto() {
        return true;
    }

    @Override
    public EMsg getMsgType() {
        return getHeader().getMsg();
    }

    @Override
    public int getSessionID() {
        return getProtoHeader().getClientSessionid();
    }

    @Override
    public void setSessionID(int sessionID) {
        getProtoHeader().setClientSessionid(sessionID);
    }

    @Override
    public SteamID getSteamID() {
        return new SteamID(getProtoHeader().getSteamid());
    }

    @Override
    public void setSteamID(SteamID steamID) {
        if (steamID == null) {
            throw new IllegalArgumentException("steamID is null");
        }
        getProtoHeader().setSteamid(steamID.convertToUInt64());
    }

    @Override
    public JobID getTargetJobID() {
        return new JobID(getProtoHeader().getJobidTarget());
    }

    @Override
    public void setTargetJobID(JobID jobID) {
        if (jobID == null) {
            throw new IllegalArgumentException("jobID is null");
        }
        getProtoHeader().setJobidTarget(jobID.getValue());
    }

    @Override
    public JobID getSourceJobID() {
        return new JobID(getProtoHeader().getJobidSource());
    }

    @Override
    public void setSourceJobID(JobID jobID) {
        if (jobID == null) {
            throw new IllegalArgumentException("jobID is null");
        }
        getProtoHeader().setJobidSource(jobID.getValue());
    }

    @Override
    public byte[] serialize() {
        throw new UnsupportedOperationException("ClientMsgProtobuf is for reading only. Use ClientMsgProtobuf<T> for serializing messages.");
    }

    @Override
    public void deserialize(byte[] data) {
        try {
            getHeader().deserialize(new ByteArrayInputStream(data));
        } catch (IOException e) {
            LOGGER.debug(e.getMessage(), e);
        }
    }
}
