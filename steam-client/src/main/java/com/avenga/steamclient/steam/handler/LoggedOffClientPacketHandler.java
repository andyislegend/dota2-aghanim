package com.avenga.steamclient.steam.handler;

import com.avenga.steamclient.base.ClientMessageProtobuf;
import com.avenga.steamclient.base.ExtendedMessage;
import com.avenga.steamclient.base.PacketMessage;
import com.avenga.steamclient.enums.EResult;
import com.avenga.steamclient.enums.ServerQuality;
import com.avenga.steamclient.generated.MsgClientLoggedOff;
import com.avenga.steamclient.protobufs.steamclient.SteammessagesClientserverLogin.CMsgClientLoggedOff;
import com.avenga.steamclient.steam.CMClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class LoggedOffClientPacketHandler implements ClientPacketHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggedOffClientPacketHandler.class);

    @Override
    public void handle(PacketMessage packetMessage, CMClient cmClient) {
        cmClient.setSessionID(null);
        cmClient.setSteamID(null);
        cmClient.setCellID(null);

        cmClient.getHeartBeatFunction().stop();

        if (packetMessage.isProto()) {
            ClientMessageProtobuf<CMsgClientLoggedOff.Builder> loggedOffMessage = new ClientMessageProtobuf<>(CMsgClientLoggedOff.class, packetMessage);
            EResult logoffResult = EResult.from(loggedOffMessage.getBody().getEresult());

            if (LOGGER.isDebugEnabled() && Objects.nonNull(logoffResult)) {
                LOGGER.debug("Logged Off proto response: {}", logoffResult.name());
            }

            if (logoffResult == EResult.TryAnotherCM || logoffResult == EResult.ServiceUnavailable) {
                cmClient.getServers().tryMark(cmClient.getConnection().getCurrentEndPoint(),
                        cmClient.getConnection().getProtocolTypes(), ServerQuality.BAD);
            }
        } else {
            ExtendedMessage<MsgClientLoggedOff> loggedOffMessage = new ExtendedMessage<>(MsgClientLoggedOff.class, packetMessage);
            EResult logoffResult = loggedOffMessage.getBody().getResult();

            if (LOGGER.isDebugEnabled() && Objects.nonNull(logoffResult)) {
                LOGGER.debug("Logged Off response: {}", logoffResult.name());
            }
        }
    }
}
