package com.avenga.steamclient.base;

import com.avenga.steamclient.generated.MsgGCHdrProtoBuf;
import com.avenga.steamclient.model.JobID;
import com.avenga.steamclient.protobufs.steamclient.SteammessagesBase;

import java.util.Objects;

public abstract class HeaderClientGCProtobufMessage extends GCBaseMessage<MsgGCHdrProtoBuf> {

    public HeaderClientGCProtobufMessage(Class<MsgGCHdrProtoBuf> clazz, int payloadReserve) {
        super(clazz, payloadReserve);
    }

    @Override
    public boolean isProto() {
        return true;
    }

    @Override
    public int getMessageType() {
        return getHeader().getMsg();
    }

    @Override
    public JobID getTargetJobID() {
        return new JobID(getProtoHeader().getJobidTarget());
    }

    @Override
    public void setTargetJobID(JobID jobID) {
        Objects.requireNonNull(jobID, "jobID wasn't provided");

        getProtoHeader().setJobidTarget(jobID.getValue());
    }

    @Override
    public JobID getSourceJobID() {
        return new JobID(getProtoHeader().getJobidSource());
    }

    @Override
    public void setSourceJobID(JobID jobID) {
        Objects.requireNonNull(jobID, "jobID wasn't provided");

        getProtoHeader().setJobidSource(jobID.getValue());
    }

    /**
     * Shorthand accessor for the protobuf header.
     *
     * @return the protobuf header
     */
    public SteammessagesBase.CMsgProtoBufHeader.Builder getProtoHeader() {
        return getHeader().getProto();
    }
}
