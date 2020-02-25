package com.avenga.steamclient.steam.asyncclient.callbackmanager;

import com.avenga.steamclient.model.JobID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class BaseCallbackMessage implements CallbackMessage {
    private JobID jobID = JobID.INVALID;
}
