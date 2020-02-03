package com.avenga.steamclient.steam.wrapper;

import com.avenga.steamclient.base.ClientGCProtobufMessage;
import com.avenga.steamclient.protobufs.dota.DotaGCMessagesClient;
import com.avenga.steamclient.protobufs.dota.DotaGCMessagesClient.CMsgGCMatchDetailsResponse;
import com.avenga.steamclient.protobufs.dota.DotaGCMessagesCommon.CMsgDOTAProfileCard;
import com.avenga.steamclient.protobufs.dota.DotaGCMessagesId;
import com.avenga.steamclient.protobufs.dota.GCSdkGCMessages;
import com.avenga.steamclient.protobufs.tf.GCSystemMessages;
import com.avenga.steamclient.steam.coordinator.GameCoordinator;
import com.avenga.steamclient.steam.coordinator.callback.GCSessionCallbackHandler;
import com.avenga.steamclient.steam.coordinator.callback.MatchDetailsCallbackHandler;
import com.avenga.steamclient.steam.coordinator.callback.ProfileCardCallbackHandler;
import lombok.RequiredArgsConstructor;

import static com.avenga.steamclient.protobufs.dota.DotaGCMessagesId.EDOTAGCMsg.*;
import static com.avenga.steamclient.protobufs.tf.GCSystemMessages.EGCBaseClientMsg.k_EMsgGCClientHello;

@RequiredArgsConstructor
public class DotaClient {
    private static final int DOTA_2_APP_ID = 570;
    private final GameCoordinator gameCoordinator;

    public GCSdkGCMessages.CMsgClientWelcome initSession() {
        var gcSessionCallback = gameCoordinator.addCallback(GCSystemMessages.EGCBaseClientMsg.k_EMsgGCClientWelcome.getNumber());
        var clientHelloMessage = new ClientGCProtobufMessage<GCSdkGCMessages.CMsgClientHello.Builder>(GCSdkGCMessages.CMsgClientHello.class, k_EMsgGCClientHello.getNumber());
        clientHelloMessage.getBody().setEngine(GCSdkGCMessages.ESourceEngine.k_ESE_Source2);
        gameCoordinator.send(clientHelloMessage, DOTA_2_APP_ID, k_EMsgGCClientHello.getNumber());
        return GCSessionCallbackHandler.handle(gcSessionCallback).getBody().build();
    }

    public CMsgGCMatchDetailsResponse getMatchDetails(long matchId) {
        var matchDetailsCallback = gameCoordinator.addCallback(k_EMsgGCMatchDetailsResponse.getNumber());
        var matchRequestMessage = new ClientGCProtobufMessage<DotaGCMessagesClient.CMsgGCMatchDetailsRequest.Builder>(DotaGCMessagesClient.CMsgGCMatchDetailsRequest.class, DotaGCMessagesId.EDOTAGCMsg.k_EMsgGCMatchDetailsRequest.getNumber());
        matchRequestMessage.getBody().setMatchId(matchId);
        gameCoordinator.send(matchRequestMessage, DOTA_2_APP_ID, DotaGCMessagesId.EDOTAGCMsg.k_EMsgGCMatchDetailsRequest.getNumber());
        return MatchDetailsCallbackHandler.handle(matchDetailsCallback).getBody().build();
    }

    public CMsgDOTAProfileCard getAccountProfileCard(int accountId) {
        var profileCardCallback = gameCoordinator.addCallback(k_EMsgClientToGCGetProfileCardResponse.getNumber());
        var profileCardMessage = new ClientGCProtobufMessage<DotaGCMessagesClient.CMsgClientToGCGetProfileCard.Builder>(DotaGCMessagesClient.CMsgClientToGCGetProfileCard.class, k_EMsgClientToGCGetProfileCard.getNumber());
        profileCardMessage.getBody().setAccountId(accountId);
        gameCoordinator.send(profileCardMessage, DOTA_2_APP_ID, k_EMsgClientToGCGetProfileCard.getNumber());
        return ProfileCardCallbackHandler.handle(profileCardCallback).getBody().build();
    }
}
