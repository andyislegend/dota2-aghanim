package com.avenga.steamclient.steam.asyncclient.steamuser.callback;

import com.avenga.steamclient.enums.EResult;
import com.avenga.steamclient.model.JobID;
import com.avenga.steamclient.protobufs.steamclient.SteammessagesClientserverLogin.CMsgClientRequestWebAPIAuthenticateUserNonceResponse;
import com.avenga.steamclient.steam.asyncclient.callbackmanager.BaseCallbackMessage;
import lombok.Getter;

/**
 * This callback is received when requesting a new WebAPI authentication user nonce.
 */
@Getter
public class WebAPIUserNonceCallback extends BaseCallbackMessage {
    private EResult result;
    private String nonce;

    public WebAPIUserNonceCallback(JobID jobID, CMsgClientRequestWebAPIAuthenticateUserNonceResponse.Builder body) {
        setJobID(jobID);

        result = EResult.from(body.getEresult());
        nonce = body.getWebapiAuthenticateUserNonce();
    }
}
