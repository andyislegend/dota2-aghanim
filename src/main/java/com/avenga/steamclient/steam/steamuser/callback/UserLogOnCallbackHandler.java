package com.avenga.steamclient.steam.steamuser.callback;

import com.avenga.steamclient.base.ClientMessageProtobuf;
import com.avenga.steamclient.base.Message;
import com.avenga.steamclient.base.PacketMessage;
import com.avenga.steamclient.enums.EMsg;
import com.avenga.steamclient.exception.CallbackCompletionException;
import com.avenga.steamclient.generated.MsgClientLogOnResponse;
import com.avenga.steamclient.model.steam.SteamMessageCallback;
import com.avenga.steamclient.protobufs.steamclient.SteammessagesClientserverLogin.CMsgClientLogonResponse;
import com.avenga.steamclient.steam.steamuser.UserLogOnResponse;

import java.util.concurrent.ExecutionException;

import static com.avenga.steamclient.constant.Constant.CALLBACK_EXCEPTION_MESSAGE_FORMAT;

public class UserLogOnCallbackHandler {

    public static final int CALLBACK_MESSAGE_CODE = EMsg.ClientLogOnResponse.code();

    public static UserLogOnResponse handle(SteamMessageCallback<PacketMessage> steamMessageCallback) {
        PacketMessage steamPacketMessage;
        try {
            steamPacketMessage = steamMessageCallback.getCallback().get();
        } catch (final InterruptedException | ExecutionException e) {
            throw new CallbackCompletionException(String.format(CALLBACK_EXCEPTION_MESSAGE_FORMAT, "UserLogOn", e.getMessage()) , e);
        }

        if (steamPacketMessage.isProto()) {
            ClientMessageProtobuf<CMsgClientLogonResponse.Builder> logonResp = new ClientMessageProtobuf<>(
                    CMsgClientLogonResponse.class, steamPacketMessage);

            return new UserLogOnResponse(logonResp.getBody());
        } else {
            Message<MsgClientLogOnResponse> logonResp = new Message<>(MsgClientLogOnResponse.class, steamPacketMessage);

            return new UserLogOnResponse(logonResp.getBody());
        }
    }
}
