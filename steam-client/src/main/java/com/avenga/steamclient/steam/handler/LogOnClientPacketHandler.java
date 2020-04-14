package com.avenga.steamclient.steam.handler;

import com.avenga.steamclient.base.ClientMessageProtobuf;
import com.avenga.steamclient.base.PacketMessage;
import com.avenga.steamclient.enums.EResult;
import com.avenga.steamclient.enums.ServerQuality;
import com.avenga.steamclient.model.SteamID;
import com.avenga.steamclient.protobufs.steamclient.SteammessagesClientserverLogin.CMsgClientLogonResponse;
import com.avenga.steamclient.steam.CMClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogOnClientPacketHandler implements ClientPacketHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogOnClientPacketHandler.class);

    @Override
    public void handle(PacketMessage packetMessage, CMClient cmClient) {
        if (!packetMessage.isProto()) {
            // a non proto ClientLogonResponse can come in as a result of connecting but never sending a ClientLogon
            // in this case, it always fails, so we don't need to do anything special here
            LOGGER.debug("Got non-proto logon response, this is indicative of no logon attempt after connecting.");
            return;
        }

        ClientMessageProtobuf<CMsgClientLogonResponse.Builder> logonResp = new ClientMessageProtobuf<>(CMsgClientLogonResponse.class, packetMessage);
        var logonResult = logonResp.getBody().getEresult();

        if (logonResult == EResult.OK.code()) {
            cmClient.setSessionID(logonResp.getProtoHeader().getClientSessionid());
            cmClient.setSteamID(new SteamID(logonResp.getProtoHeader().getSteamid()));

            cmClient.setCellID(logonResp.getBody().getCellId());

            // restart heartbeat
            cmClient.getHeartBeatFunction().stop();
            cmClient.getHeartBeatFunction().setDelay(logonResp.getBody().getOutOfGameHeartbeatSeconds() * 1000L);
            cmClient.getHeartBeatFunction().start();
        } else if (logonResult == EResult.TryAnotherCM.code() || logonResult == EResult.ServiceUnavailable.code()){
            cmClient.getServers().tryMark(cmClient.getConnection().getCurrentEndPoint(),
                    cmClient.getConnection().getProtocolTypes(), ServerQuality.BAD);
        }
    }
}
