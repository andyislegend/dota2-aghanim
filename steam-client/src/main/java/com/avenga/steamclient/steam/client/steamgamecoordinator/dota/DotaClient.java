package com.avenga.steamclient.steam.client.steamgamecoordinator.dota;

import com.avenga.steamclient.base.ClientGCProtobufMessage;
import com.avenga.steamclient.enums.SteamGame;
import com.avenga.steamclient.exception.CallbackTimeoutException;
import com.avenga.steamclient.model.steam.gamecoordinator.ClientGCHandler;
import com.avenga.steamclient.model.steam.gamecoordinator.dota.account.DotaProfileCard;
import com.avenga.steamclient.model.steam.gamecoordinator.dota.match.DotaMatchDetails;
import com.avenga.steamclient.protobufs.dota.DotaGCMessagesClient.CMsgClientToGCGetProfileCard;
import com.avenga.steamclient.protobufs.dota.DotaGCMessagesClient.CMsgGCMatchDetailsRequest;
import com.avenga.steamclient.protobufs.dota.GCSdkGCMessages.ESourceEngine;
import com.avenga.steamclient.steam.client.SteamClient;
import com.avenga.steamclient.steam.client.steamgamecoordinator.dota.callback.MatchDetailsCallbackHandler;
import com.avenga.steamclient.steam.client.steamgamecoordinator.dota.callback.ProfileCardCallbackHandler;
import lombok.Getter;
import lombok.Setter;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.avenga.steamclient.protobufs.dota.DotaGCMessagesId.EDOTAGCMsg.*;

public class DotaClient extends ClientGCHandler {

    @Getter
    @Setter
    /**
     * ID of the DOTA2 game registered in the Steam Network.
     */
    private int applicationId;

    public DotaClient() {
        this.applicationId = SteamGame.Dota2.getApplicationId();
    }

    /**
     * Send Hello message to the Game Coordinator server to initiate session.
     * Callback could be canceled during execution of the auto reconnect logic.
     *
     * @param timeout The time which callback handler will wait before cancel it, in milliseconds.
     * @throws CallbackTimeoutException if the wait timed out
     */
    public void sendClientHello(long timeout) throws CallbackTimeoutException {
        gameCoordinator.sendClientHello(ESourceEngine.k_ESE_Source2, applicationId, timeout);
    }

    /**
     * Gets DOTA 2 match details.
     * Callback could be canceled during execution of the auto reconnect logic.
     * <p>
     * The {@link SteamClient} should already have been connected at this point.
     *
     * @param matchId Id of the DOTA 2 match.
     * @return CompletableFuture Callback with {@link DotaMatchDetails} details of the DOTA 2 match.
     */
    public CompletableFuture<DotaMatchDetails> getMatchDetails(long matchId) {
        var matchDetailsCallback = getClient().addGCCallbackToQueue(k_EMsgGCMatchDetailsResponse.getNumber(), applicationId);
        sendMatchDetailsRequest(matchId);
        return matchDetailsCallback.getCallback()
                .thenApply(MatchDetailsCallbackHandler::getMessage);
    }

    /**
     * Gets DOTA 2 match details.
     * Result will be returned if callback will be finished in time and won't be cancled during auto reconnect logic,
     * otherwise callback after specified timeout will be removed fron queue.
     * <p>
     * The {@link SteamClient} should already have been connected at this point.
     *
     * @param matchId Id of the DOTA 2 match.
     * @param timeout The time which callback handler will wait before cancel it, in milliseconds.
     * @return details of the DOTA 2 match.
     * @throws CallbackTimeoutException if the wait timed out
     */
    public Optional<DotaMatchDetails> getMatchDetails(long matchId, long timeout) throws CallbackTimeoutException {
        var matchDetailsCallback = getClient().addGCCallbackToQueue(k_EMsgGCMatchDetailsResponse.getNumber(), applicationId);
        sendMatchDetailsRequest(matchId);
        return MatchDetailsCallbackHandler.handle(matchDetailsCallback, timeout, getClient());
    }

    /**
     * Gets DOTA 2 user account profile card.
     * Callback could be canceled during execution of the auto reconnect logic.
     * <p>
     * The {@link SteamClient} should already have been connected at this point.
     *
     * @param accountId Id of the DOTA 2 user account.
     * @return CompletableFuture Callback with {@link DotaProfileCard} user account profile card.
     */
    public CompletableFuture<DotaProfileCard> getAccountProfileCard(int accountId) {
        var profileCardCallback = getClient().addGCCallbackToQueue(k_EMsgClientToGCGetProfileCardResponse.getNumber(), applicationId);
        sendProfileCardRequest(accountId);
        return profileCardCallback.getCallback()
                .thenApply(ProfileCardCallbackHandler::getMessage);
    }

    /**
     * Gets DOTA 2 user account profile card.
     * Result will be returned if callback will be finished in time and won't be cancled during auto reconnect logic,
     * otherwise callback after specified timeout will be removed fron queue.
     * <p>
     * The {@link SteamClient} should already have been connected at this point.
     *
     * @param accountId Id of the DOTA 2 user account.
     * @param timeout   The time which callback handler will wait before cancel it, in milliseconds.
     * @return user account profile card.
     * @throws CallbackTimeoutException if the wait timed out
     */
    public Optional<DotaProfileCard> getAccountProfileCard(int accountId, long timeout) throws CallbackTimeoutException {
        var profileCardCallback = getClient().addGCCallbackToQueue(k_EMsgClientToGCGetProfileCardResponse.getNumber(), applicationId);
        sendProfileCardRequest(accountId);
        return ProfileCardCallbackHandler.handle(profileCardCallback, timeout, getClient());
    }

    private void sendMatchDetailsRequest(long matchId) {
        var matchRequestMessage = new ClientGCProtobufMessage<CMsgGCMatchDetailsRequest.Builder>(CMsgGCMatchDetailsRequest.class,
                k_EMsgGCMatchDetailsRequest.getNumber());
        matchRequestMessage.getBody().setMatchId(matchId);
        matchRequestMessage.getHeader().getProto().setJobidSource(getClient().getNextJobID().getValue());
        gameCoordinator.send(matchRequestMessage, applicationId, k_EMsgGCMatchDetailsRequest);
    }

    private void sendProfileCardRequest(int accountId) {
        var profileCardMessage = new ClientGCProtobufMessage<CMsgClientToGCGetProfileCard.Builder>(CMsgClientToGCGetProfileCard.class,
                k_EMsgClientToGCGetProfileCard.getNumber());
        profileCardMessage.getBody().setAccountId(accountId);
        profileCardMessage.getHeader().getProto().setJobidSource(getClient().getNextJobID().getValue());
        gameCoordinator.send(profileCardMessage, applicationId, k_EMsgClientToGCGetProfileCard);
    }
}
