package com.avenga.steamclient.steam.client.steamuser.callback;

import com.avenga.steamclient.base.ClientMessageProtobuf;
import com.avenga.steamclient.base.Message;
import com.avenga.steamclient.base.PacketMessage;
import com.avenga.steamclient.enums.EMsg;
import com.avenga.steamclient.exception.CallbackTimeoutException;
import com.avenga.steamclient.generated.MsgClientLogOnResponse;
import com.avenga.steamclient.model.steam.SteamMessageCallback;
import com.avenga.steamclient.protobufs.steamclient.SteammessagesClientserverLogin.CMsgClientLogonResponse;
import com.avenga.steamclient.steam.client.SteamClient;
import com.avenga.steamclient.steam.client.callback.AbstractCallbackHandler;
import com.avenga.steamclient.steam.client.steamuser.UserLogOnResponse;

public class UserLogOnCallbackHandler extends AbstractCallbackHandler<PacketMessage> {

    public static final int CALLBACK_MESSAGE_CODE = EMsg.ClientLogOnResponse.code();

    public static UserLogOnResponse handle(SteamMessageCallback<PacketMessage> callback, long timeout, SteamClient client) throws CallbackTimeoutException {
        PacketMessage packetMessage = waitAndGetMessageOrRemoveAfterTimeout(callback, timeout, "UserLogOn", client);

        return getMessage(packetMessage);
    }

    public static UserLogOnResponse getMessage(PacketMessage packetMessage) {
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
