package com.avenga.steamclient.model.steam.gamecoordinator;

import com.avenga.steamclient.base.GCPacketClientMessage;
import com.avenga.steamclient.base.GCPacketClientMessageProtobuf;
import com.avenga.steamclient.base.GCPacketMessage;
import com.avenga.steamclient.protobufs.steamclient.SteammessagesClientserver2.CMsgGCClient;
import com.avenga.steamclient.util.MessageUtil;
import com.avenga.steamclient.util.SteamEnumUtils;

public class GCMessage {
    private int eMsg;
    private int appID;
    private GCPacketMessage message;

    public GCMessage(CMsgGCClient.Builder gcMsg) {
        eMsg = gcMsg.getMsgtype();
        appID = gcMsg.getAppid();
        message = getPacketGCMsg(gcMsg.getMsgtype(), gcMsg.getPayload().toByteArray());
    }

    /**
     * @return the game coordinator message type
     */
    public int geteMsg() {
        return MessageUtil.getGCMsg(eMsg);
    }

    /**
     * @return the AppID of the game coordinator the message is from
     */
    public int getApplicationID() {
        return appID;
    }

    /**
     * @return <b>true</b> if this instance is protobuf'd; otherwise, <b>false</b>
     */
    public boolean isProto() {
        return MessageUtil.isProtoBuf(eMsg);
    }

    /**
     * @return the actual message
     */
    public GCPacketMessage getMessage() {
        return message;
    }

    public String getMessageType() {
        return SteamEnumUtils.getEnumName(geteMsg(), appID).orElse("");
    }

    private static GCPacketMessage getPacketGCMsg(int eMsg, byte[] data) {
        int realEMsg = MessageUtil.getGCMsg(eMsg);

        if (MessageUtil.isProtoBuf(eMsg)) {
            return new GCPacketClientMessageProtobuf(realEMsg, data);
        } else {
            return new GCPacketClientMessage(realEMsg, data);
        }
    }
}
