package com.avenga.steamclient.steam.asyncclient.steamgamecoordinator.dota;

import com.avenga.steamclient.base.ClientGCProtobufMessage;
import com.avenga.steamclient.enums.SteamGame;
import com.avenga.steamclient.model.steam.gamecoordinator.ClientGCMessageHandler;
import com.avenga.steamclient.model.steam.gamecoordinator.GCMessage;
import com.avenga.steamclient.protobufs.dota.DotaGCMessagesClient.CMsgClientToGCGetProfileCard;
import com.avenga.steamclient.protobufs.dota.DotaGCMessagesClient.CMsgGCMatchDetailsRequest;
import com.avenga.steamclient.protobufs.dota.DotaGCMessagesClient.CMsgGCMatchDetailsResponse;
import com.avenga.steamclient.protobufs.dota.DotaGCMessagesCommon.CMsgDOTAProfileCard;
import com.avenga.steamclient.protobufs.dota.GCSdkGCMessages.CMsgClientWelcome;
import com.avenga.steamclient.protobufs.dota.GCSdkGCMessages.ESourceEngine;
import com.avenga.steamclient.steam.asyncclient.steamgamecoordinator.dota.callback.ClientWelcomeCallback;
import com.avenga.steamclient.steam.asyncclient.steamgamecoordinator.dota.callback.DotaAccountProfileCardCallback;
import com.avenga.steamclient.steam.asyncclient.steamgamecoordinator.dota.callback.DotaMatchDetailsCallback;
import com.avenga.steamclient.util.SteamEnumUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import static com.avenga.steamclient.protobufs.dota.DotaGCMessagesId.EDOTAGCMsg.*;
import static com.avenga.steamclient.protobufs.tf.GCSystemMessages.EGCBaseClientMsg.k_EMsgGCClientWelcome;

public class DotaClientAsync extends ClientGCMessageHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(DotaClientAsync.class);
    private static final int DEFAULT_APPLICATION_ID = SteamGame.Dota2.getApplicationId();

    private final Map<Integer, Consumer<GCMessage>> clientHandlers;
    private final int applicationId;

    public DotaClientAsync() {
        this(DEFAULT_APPLICATION_ID);
    }

    public DotaClientAsync(int applicationId) {
        this.applicationId = applicationId;
        this.clientHandlers = Map.of(
                k_EMsgGCClientWelcome.getNumber(), this::handleClientWelcome,
                k_EMsgGCMatchDetailsResponse.getNumber(), this::handleMatchDetails,
                k_EMsgClientToGCGetProfileCardResponse.getNumber(), this::handleProfileCard
        );
    }

    /**
     * Sends DOTA client hello message to Game Coordinator server.
     * Results are returned in a {@link ClientWelcomeCallback} callback.
     */
    public void sendClientHello() {
        gameCoordinator.sendClientHello(ESourceEngine.k_ESE_Source2, applicationId);
    }

    /**
     * Requests DOTA match details from Steam Servers.
     * Results are returned in a {@link DotaMatchDetailsCallback} callback.
     *
     * @param matchId The id of the DOTA match.
     */
    public void requestMatchDetails(long matchId) {
        var matchRequestMessage = new ClientGCProtobufMessage<CMsgGCMatchDetailsRequest.Builder>(CMsgGCMatchDetailsRequest.class,
                k_EMsgGCMatchDetailsRequest.getNumber());
        matchRequestMessage.getBody().setMatchId(matchId);
        gameCoordinator.send(matchRequestMessage, applicationId);
    }

    /**
     * Requests account profile card of the DOTA player.
     * Results are returned in a {@link DotaAccountProfileCardCallback} callback.
     *
     * @param accountId The id of the DOTA player.
     */
    public void requestAccountProfileCard(int accountId) {
        var profileCardMessage = new ClientGCProtobufMessage<CMsgClientToGCGetProfileCard.Builder>(CMsgClientToGCGetProfileCard.class,
                k_EMsgClientToGCGetProfileCard.getNumber());
        profileCardMessage.getBody().setAccountId(accountId);
        gameCoordinator.send(profileCardMessage, applicationId);
    }

    @Override
    public void handleMessage(GCMessage gcMessage) {
        Objects.requireNonNull(gcMessage, "GC packet message wasn't provided");

        Consumer<GCMessage> dispatcher = clientHandlers.get(gcMessage.geteMsg());
        if (dispatcher != null) {
            dispatcher.accept(gcMessage);
        }
    }

    @Override
    public int getApplicationId() {
        return applicationId;
    }

    private void handleClientWelcome(GCMessage gcMessage) {
        ClientGCProtobufMessage<CMsgClientWelcome.Builder> clientWelcome = new ClientGCProtobufMessage<>(CMsgClientWelcome.class, gcMessage.getMessage());
        getClient().postCallback(new ClientWelcomeCallback(clientWelcome.getBody()));
    }

    private void handleProfileCard(GCMessage gcMessage) {
        ClientGCProtobufMessage<CMsgDOTAProfileCard.Builder> profileCard = new ClientGCProtobufMessage<>(
                CMsgDOTAProfileCard.class, gcMessage.getMessage());
        getClient().postCallback(new DotaAccountProfileCardCallback(profileCard.getBody()));
    }

    private void handleMatchDetails(GCMessage gcMessage) {
        ClientGCProtobufMessage<CMsgGCMatchDetailsResponse.Builder> matchDetails = new ClientGCProtobufMessage<>(
                CMsgGCMatchDetailsResponse.class, gcMessage.getMessage());
        getClient().postCallback(new DotaMatchDetailsCallback(matchDetails.getBody()));
    }
}
