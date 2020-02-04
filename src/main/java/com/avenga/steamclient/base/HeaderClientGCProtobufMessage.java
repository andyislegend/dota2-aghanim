package com.avenga.steamclient.base;

import com.avenga.steamclient.generated.MsgGCHdrProtoBuf;
import com.avenga.steamclient.model.JobID;
import com.avenga.steamclient.protobufs.steamclient.SteammessagesBase;

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

    /**
     * Shorthand accessor for the protobuf header.
     *
     * @return the protobuf header
     */
    public SteammessagesBase.CMsgProtoBufHeader.Builder getProtoHeader() {
        return getHeader().getProto();
    }
}
