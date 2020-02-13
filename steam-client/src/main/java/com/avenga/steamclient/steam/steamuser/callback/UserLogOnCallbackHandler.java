package com.avenga.steamclient.steam.steamuser.callback;

import com.avenga.steamclient.base.ClientMessageProtobuf;
import com.avenga.steamclient.base.Message;
import com.avenga.steamclient.base.PacketMessage;
import com.avenga.steamclient.enums.EMsg;
import com.avenga.steamclient.exception.CallbackTimeoutException;
import com.avenga.steamclient.generated.MsgClientLogOnResponse;
import com.avenga.steamclient.model.steam.SteamMessageCallback;
import com.avenga.steamclient.protobufs.steamclient.SteammessagesClientserverLogin.CMsgClientLogonResponse;
import com.avenga.steamclient.steam.client.callback.AbstractCallbackHandler;
import com.avenga.steamclient.steam.steamuser.UserLogOnResponse;

public class UserLogOnCallbackHandler extends AbstractCallbackHandler<PacketMessage> {

    public static final int CALLBACK_MESSAGE_CODE = EMsg.ClientLogOnResponse.code();

    public static UserLogOnResponse handle(SteamMessageCallback<PacketMessage> callback) {
        PacketMessage packetMessage = waitAndGetPacketMessage(callback, "UserLogOn");

        return getMessage(packetMessage);
    }

    public static UserLogOnResponse handle(SteamMessageCallback<PacketMessage> callback, long timeout) throws CallbackTimeoutException {
        PacketMessage packetMessage = waitAndGetPacketMessage(callback, timeout, "UserLogOn");

        return getMessage(packetMessage);
    }

    private static UserLogOnResponse getMessage(PacketMessage packetMessage) {
        return packetMessage.isProto() ? getProtoLogOnResponse(packetMessage) : getLogOnResponse(packetMessage);
    }

    private static UserLogOnResponse getProtoLogOnResponse(PacketMessage packetMessage) {
        ClientMessageProtobuf<CMsgClientLogonResponse.Builder> logonResp = new ClientMessageProtobuf<>(
                CMsgClientLogonResponse.class, packetMessage);

        return new UserLogOnResponse(logonResp.getBody());
    }

    private static UserLogOnResponse getLogOnResponse(PacketMessage packetMessage) {
        Message<MsgClientLogOnResponse> logonResp = new Message<>(MsgClientLogOnResponse.class, packetMessage);

        return new UserLogOnResponse(logonResp.getBody());
    }
}
