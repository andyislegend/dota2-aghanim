package com.avenga.steamclient.steam.steamuser.response.handler;

import com.avenga.steamclient.base.ClientMessageProtobuf;
import com.avenga.steamclient.base.Message;
import com.avenga.steamclient.enums.EMsg;
import com.avenga.steamclient.exception.MissedResponseException;
import com.avenga.steamclient.generated.MsgClientLogOnResponse;
import com.avenga.steamclient.protobufs.steamclient.SteammessagesClientserverLogin.CMsgClientLogonResponse;
import com.avenga.steamclient.steam.client.SteamClient;
import com.avenga.steamclient.steam.steamuser.UserLogOnResponse;

public class UserLogOnResponseHandler {

    public UserLogOnResponse handle(SteamClient steamClient) {
        var response = steamClient.getResponse(EMsg.ClientLogOnResponse, 5000);

        if (response.isEmpty()) {
            steamClient.disconnect();
            throw new MissedResponseException("Response for use log on request wasn't received");
        }

        var packetMessage = response.get();
        if (packetMessage.isProto()) {
            ClientMessageProtobuf<CMsgClientLogonResponse.Builder> logonResp = new ClientMessageProtobuf<>(CMsgClientLogonResponse.class, packetMessage);

            return new UserLogOnResponse(logonResp.getBody());
        } else {
            Message<MsgClientLogOnResponse> logonResp = new Message<>(MsgClientLogOnResponse.class, packetMessage);

            return new UserLogOnResponse(logonResp.getBody());
        }
    }
}
