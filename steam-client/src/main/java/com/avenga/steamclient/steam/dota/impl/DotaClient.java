package com.avenga.steamclient.steam.dota.impl;

import com.avenga.steamclient.base.ClientGCProtobufMessage;
import com.avenga.steamclient.exception.CallbackTimeoutException;
import com.avenga.steamclient.protobufs.dota.DotaGCMessagesClient.CMsgClientToGCGetProfileCard;
import com.avenga.steamclient.protobufs.dota.DotaGCMessagesClient.CMsgGCMatchDetailsRequest;
import com.avenga.steamclient.protobufs.dota.DotaGCMessagesClient.CMsgGCMatchDetailsResponse;
import com.avenga.steamclient.protobufs.dota.DotaGCMessagesCommon.CMsgDOTAProfileCard;
import com.avenga.steamclient.steam.client.SteamClient;
import com.avenga.steamclient.steam.coordinator.AbstractGameCoordinator;
import com.avenga.steamclient.steam.coordinator.callback.MatchDetailsCallbackHandler;
import com.avenga.steamclient.steam.coordinator.callback.ProfileCardCallbackHandler;
import com.avenga.steamclient.steam.dota.AbstractDotaClient;

import static com.avenga.steamclient.protobufs.dota.DotaGCMessagesId.EDOTAGCMsg.*;

public class DotaClient extends AbstractDotaClient {

    private static final int DEFAULT_APPLICATION_ID = 570;

    public DotaClient(AbstractGameCoordinator gameCoordinator) {
        super(gameCoordinator, DEFAULT_APPLICATION_ID);
    }

    public DotaClient(AbstractGameCoordinator gameCoordinator, int applicationId) {
        super(gameCoordinator, applicationId);
    }

    /**
     * Gets DOTA 2 match details.
     * The {@link SteamClient} should already have been connected at this point.
     *
     * @param matchId Id of the DOTA 2 match.
     * @return details of the DOTA 2 match.
     */
    @Override
    public CMsgGCMatchDetailsResponse getMatchDetails(long matchId) {
        var matchDetailsCallback = gameCoordinator.addCallback(k_EMsgGCMatchDetailsResponse.getNumber());
        sendMatchDetailsRequest(matchId);
        return MatchDetailsCallbackHandler.handle(matchDetailsCallback);
    }

    /**
     * Gets DOTA 2 match details.
     * Result will be returned if callback will be finished in time, otherwise callback after specified timeout will be canceled.
     * The {@link SteamClient} should already have been connected at this point.
     *
     * @param matchId Id of the DOTA 2 match.
     * @param timeout The time which callback handler will wait before cancel it, in milliseconds.
     *
     * @throws CallbackTimeoutException if the wait timed out
     * @return details of the DOTA 2 match.
     */
    @Override
    public CMsgGCMatchDetailsResponse getMatchDetails(long matchId, long timeout) throws CallbackTimeoutException {
        var matchDetailsCallback = gameCoordinator.addCallback(k_EMsgGCMatchDetailsResponse.getNumber());
        sendMatchDetailsRequest(matchId);
        return MatchDetailsCallbackHandler.handle(matchDetailsCallback, timeout);
    }

    /**
     * Gets DOTA 2 user account profile card.
     * The {@link SteamClient} should already have been connected at this point.
     *
     * @param accountId Id of the DOTA 2 user account.
     * @return user account profile card.
     */
    @Override
    public CMsgDOTAProfileCard getAccountProfileCard(int accountId) {
        var profileCardCallback = gameCoordinator.addCallback(k_EMsgClientToGCGetProfileCardResponse.getNumber());
        sendProfileCardRequest(accountId);
        return ProfileCardCallbackHandler.handle(profileCardCallback);
    }

    /**
     * Gets DOTA 2 user account profile card.
     * Result will be returned if callback will be finished in time, otherwise callback after specified timeout will be canceled.
     * The {@link SteamClient} should already have been connected at this point.
     *
     * @param accountId Id of the DOTA 2 user account.
     * @param timeout The time which callback handler will wait before cancel it, in milliseconds.
     *
     * @throws CallbackTimeoutException if the wait timed out
     * @return user account profile card.
     */
    @Override
    public CMsgDOTAProfileCard getAccountProfileCard(int accountId, long timeout) throws CallbackTimeoutException {
        var profileCardCallback = gameCoordinator.addCallback(k_EMsgClientToGCGetProfileCardResponse.getNumber());
        sendProfileCardRequest(accountId);
        return ProfileCardCallbackHandler.handle(profileCardCallback, timeout);
    }

    private void sendMatchDetailsRequest(long matchId) {
        var matchRequestMessage = new ClientGCProtobufMessage<CMsgGCMatchDetailsRequest.Builder>(CMsgGCMatchDetailsRequest.class,
                k_EMsgGCMatchDetailsRequest.getNumber());
        matchRequestMessage.getBody().setMatchId(matchId);
        gameCoordinator.send(matchRequestMessage, applicationId, k_EMsgGCMatchDetailsRequest);
    }

    private void sendProfileCardRequest(int accountId) {
        var profileCardMessage = new ClientGCProtobufMessage<CMsgClientToGCGetProfileCard.Builder>(CMsgClientToGCGetProfileCard.class,
                k_EMsgClientToGCGetProfileCard.getNumber());
        profileCardMessage.getBody().setAccountId(accountId);
        gameCoordinator.send(profileCardMessage, applicationId, k_EMsgClientToGCGetProfileCard);
    }
}
