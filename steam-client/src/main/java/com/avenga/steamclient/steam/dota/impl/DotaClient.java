package com.avenga.steamclient.steam.dota.impl;

import com.avenga.steamclient.base.ClientGCProtobufMessage;
import com.avenga.steamclient.enums.SteamGame;
import com.avenga.steamclient.exception.CallbackTimeoutException;
import com.avenga.steamclient.model.steam.gamecoordinator.dota.account.DotaProfileCard;
import com.avenga.steamclient.model.steam.gamecoordinator.dota.match.DotaMatchDetails;
import com.avenga.steamclient.protobufs.dota.DotaGCMessagesClient.CMsgClientToGCGetProfileCard;
import com.avenga.steamclient.protobufs.dota.DotaGCMessagesClient.CMsgGCMatchDetailsRequest;
import com.avenga.steamclient.steam.client.SteamClient;
import com.avenga.steamclient.steam.coordinator.AbstractGameCoordinator;
import com.avenga.steamclient.steam.coordinator.callback.MatchDetailsCallbackHandler;
import com.avenga.steamclient.steam.coordinator.callback.ProfileCardCallbackHandler;
import com.avenga.steamclient.steam.dota.AbstractDotaClient;

import java.util.concurrent.CompletableFuture;

import static com.avenga.steamclient.protobufs.dota.DotaGCMessagesId.EDOTAGCMsg.*;

public class DotaClient extends AbstractDotaClient {

    private static final int DEFAULT_APPLICATION_ID = SteamGame.Dota2.getApplicationId();
    private static final long DEFAULT_CALLBACK_WAIT_TIMEOUT = 20000;

    public DotaClient(AbstractGameCoordinator gameCoordinator) throws CallbackTimeoutException {
        super(gameCoordinator, DEFAULT_APPLICATION_ID, DEFAULT_CALLBACK_WAIT_TIMEOUT);
    }

    public DotaClient(AbstractGameCoordinator gameCoordinator, long callbackWaitTimeout) throws CallbackTimeoutException {
        super(gameCoordinator, DEFAULT_APPLICATION_ID, callbackWaitTimeout);
    }

    public DotaClient(AbstractGameCoordinator gameCoordinator, int applicationId) throws CallbackTimeoutException {
        super(gameCoordinator, applicationId, DEFAULT_CALLBACK_WAIT_TIMEOUT);
    }

    public DotaClient(AbstractGameCoordinator gameCoordinator, int applicationId, long callbackWaitTimeout) throws CallbackTimeoutException {
        super(gameCoordinator, applicationId, callbackWaitTimeout);
    }

    /**
     * Gets DOTA 2 match details.
     * The {@link SteamClient} should already have been connected at this point.
     *
     * @param matchId Id of the DOTA 2 match.
     * @return CompletableFuture Callback with {@link DotaMatchDetails} details of the DOTA 2 match.
     */
    @Override
    public CompletableFuture<DotaMatchDetails> getMatchDetails(long matchId) {
        var matchDetailsCallback = gameCoordinator.addCallback(k_EMsgGCMatchDetailsResponse.getNumber());
        sendMatchDetailsRequest(matchId);
        return matchDetailsCallback.getCallback()
                .thenApply(MatchDetailsCallbackHandler::getMessage);
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
    public DotaMatchDetails getMatchDetails(long matchId, long timeout) throws CallbackTimeoutException {
        var matchDetailsCallback = gameCoordinator.addCallback(k_EMsgGCMatchDetailsResponse.getNumber());
        sendMatchDetailsRequest(matchId);
        return MatchDetailsCallbackHandler.handle(matchDetailsCallback, timeout);
    }

    /**
     * Gets DOTA 2 user account profile card.
     * The {@link SteamClient} should already have been connected at this point.
     *
     * @param accountId Id of the DOTA 2 user account.
     * @return CompletableFuture Callback with {@link DotaProfileCard} user account profile card.
     */
    @Override
    public CompletableFuture<DotaProfileCard> getAccountProfileCard(int accountId) {
        var profileCardCallback = gameCoordinator.addCallback(k_EMsgClientToGCGetProfileCardResponse.getNumber());
        sendProfileCardRequest(accountId);
        return profileCardCallback.getCallback()
                .thenApply(ProfileCardCallbackHandler::getMessage);
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
    public DotaProfileCard getAccountProfileCard(int accountId, long timeout) throws CallbackTimeoutException {
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
