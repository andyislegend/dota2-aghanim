package com.avenga.steamclient.steam.asyncclient.steamuser.callback;

import com.avenga.steamclient.model.JobID;
import com.avenga.steamclient.model.steam.user.UserOneTimePassword;
import com.avenga.steamclient.protobufs.steamclient.SteammessagesClientserver2.CMsgClientUpdateMachineAuth;
import com.avenga.steamclient.steam.asyncclient.callbackmanager.BaseCallbackMessage;
import lombok.Getter;

/**
 * This callback is received when the backend wants the client to update it's local machine authentication data.
 */
@Getter
public class UpdateMachineAuthCallback extends BaseCallbackMessage {
    private byte[] data;
    private int bytesToWrite;
    private int offset;
    private String fileName;
    private UserOneTimePassword userOneTimePassword;

    public UpdateMachineAuthCallback(JobID jobID, CMsgClientUpdateMachineAuth.Builder message) {
        setJobID(jobID);

        data = message.getBytes().toByteArray();
        bytesToWrite = message.getCubtowrite();
        offset = message.getOffset();
        fileName = message.getFilename();

        userOneTimePassword = UserOneTimePassword.builder()
                .type(message.getOtpType())
                .identifier(message.getOtpIdentifier())
                .sharedSecret(message.getOtpSharedsecret().toByteArray())
                .timeDrift(message.getOtpTimedrift())
                .build();
    }
}
