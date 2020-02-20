package com.avenga.steamclient.steam.asyncclient.callbackmanager;

import com.avenga.steamclient.model.JobID;

public interface CallbackMessage {
    JobID getJobID();

    void setJobID(JobID jobID);
}
