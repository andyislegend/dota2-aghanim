package com.avenga.steamclient.steam.dota.impl;

import com.avenga.steamclient.base.ClientGCProtobufMessage;
import com.avenga.steamclient.protobufs.dota.DotaGCMessagesClient.CMsgClientToGCGetProfileCard;
import com.avenga.steamclient.protobufs.dota.DotaGCMessagesClient.CMsgGCMatchDetailsRequest;
import com.avenga.steamclient.protobufs.dota.DotaGCMessagesClient.CMsgGCMatchDetailsResponse;
import com.avenga.steamclient.protobufs.dota.DotaGCMessagesCommon.CMsgDOTAProfileCard;
import com.avenga.steamclient.steam.coordinator.AbstractGameCoordinator;
import com.avenga.steamclient.steam.coordinator.callback.MatchDetailsCallbackHandler;
import com.avenga.steamclient.steam.coordinator.callback.ProfileCardCallbackHandler;
import com.avenga.steamclient.steam.dota.AbstractDotaClient;

import static com.avenga.steamclient.protobufs.dota.DotaGCMessagesId.EDOTAGCMsg.*;

public class DotaClient extends AbstractDotaClient {

    private static final int DEFAULT_APPLICATION_ID = 570;

    private int applicationId;

    public DotaClient(AbstractGameCoordinator gameCoordinator) {
        super(gameCoordinator);
        this.applicationId = DEFAULT_APPLICATION_ID;
    }

    public DotaClient(AbstractGameCoordinator gameCoordinator, int applicationId) {
        super(gameCoordinator);
        this.applicationId = applicationId;
    }

    @Override
    public CMsgGCMatchDetailsResponse getMatchDetails(long matchId) {
        var matchDetailsCallback = gameCoordinator.addCallback(k_EMsgGCMatchDetailsResponse.getNumber());
        var matchRequestMessage = new ClientGCProtobufMessage<CMsgGCMatchDetailsRequest.Builder>(CMsgGCMatchDetailsRequest.class, k_EMsgGCMatchDetailsRequest.getNumber());
        matchRequestMessage.getBody().setMatchId(matchId);
        gameCoordinator.send(matchRequestMessage, applicationId, k_EMsgGCMatchDetailsRequest.getNumber());
        return MatchDetailsCallbackHandler.handle(matchDetailsCallback).getBody().build();
    }

    @Override
    public CMsgDOTAProfileCard getAccountProfileCard(int accountId) {
        var profileCardCallback = gameCoordinator.addCallback(k_EMsgClientToGCGetProfileCardResponse.getNumber());
        var profileCardMessage = new ClientGCProtobufMessage<CMsgClientToGCGetProfileCard.Builder>(CMsgClientToGCGetProfileCard.class, k_EMsgClientToGCGetProfileCard.getNumber());
        profileCardMessage.getBody().setAccountId(accountId);
        gameCoordinator.send(profileCardMessage, applicationId, k_EMsgClientToGCGetProfileCard.getNumber());
        return ProfileCardCallbackHandler.handle(profileCardCallback).getBody().build();
    }

    @Override
    public int getApplicationId() {
        return applicationId;
    }
}
