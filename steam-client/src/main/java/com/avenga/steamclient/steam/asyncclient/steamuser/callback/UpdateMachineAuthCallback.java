package com.avenga.steamclient.steam.asyncclient.steamuser.callback;

import com.avenga.steamclient.model.JobID;
import com.avenga.steamclient.protobufs.steamclient.SteammessagesClientserver2.CMsgClientUpdateMachineAuth;
import com.avenga.steamclient.steam.asyncclient.callbackmanager.BaseCallbackMessage;
import lombok.Getter;
import lombok.Setter;

/**
 * This callback is received when the backend wants the client to update it's local machine authentication data.
 */
@Getter
public class UpdateMachineAuthCallback extends BaseCallbackMessage {
    private byte[] data;
    private int bytesToWrite;
    private int offset;
    private String fileName;
    private OTPDetails oneTimePassword;

    public UpdateMachineAuthCallback(JobID jobID, CMsgClientUpdateMachineAuth.Builder message) {
        setJobID(jobID);

        data = message.getBytes().toByteArray();
        bytesToWrite = message.getCubtowrite();
        offset = message.getOffset();
        fileName = message.getFilename();

        oneTimePassword = new OTPDetails();
        oneTimePassword.setType(message.getOtpType());
        oneTimePassword.setIdentifier(message.getOtpIdentifier());
        oneTimePassword.setSharedSecret(message.getOtpSharedsecret().toByteArray());
        oneTimePassword.setTimeDrift(message.getOtpTimedrift());
    }

    /**
     * Represents various one-time-password details.
     */
    @Getter
    @Setter
    public static class OTPDetails {
        private int type;
        private String identifier;
        private byte[] sharedSecret;
        private int timeDrift;
    }

}
