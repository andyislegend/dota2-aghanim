package com.avenga.steamclient.steam.handler;

import com.avenga.steamclient.base.ClientMessageProtobuf;
import com.avenga.steamclient.base.PacketMessage;
import com.avenga.steamclient.enums.EResult;
import com.avenga.steamclient.enums.ServerQuality;
import com.avenga.steamclient.protobufs.steamclient.SteammessagesClientserverLogin.CMsgClientLoggedOff;
import com.avenga.steamclient.steam.CMClient;

public class LoggedOffClientPacketHandler implements ClientPacketHandler {

    @Override
    public void handle(PacketMessage packetMessage, CMClient cmClient) {
        cmClient.setSessionID(null);
        cmClient.setSteamID(null);
        cmClient.setCellID(null);

        cmClient.getHeartBeatFunction().stop();

        if (packetMessage.isProto()) {
            ClientMessageProtobuf<CMsgClientLoggedOff.Builder> logoffMsg = new ClientMessageProtobuf<>(CMsgClientLoggedOff.class, packetMessage);
            EResult logoffResult = EResult.from(logoffMsg.getBody().getEresult());

            if (logoffResult == EResult.TryAnotherCM || logoffResult == EResult.ServiceUnavailable) {
                cmClient.getServers().tryMark(cmClient.getConnection().getCurrentEndPoint(),
                        cmClient.getConnection().getProtocolTypes(), ServerQuality.BAD);
            }
        }
    }
}
