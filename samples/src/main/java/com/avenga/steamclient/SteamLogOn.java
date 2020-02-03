package com.avenga.steamclient;

import com.avenga.steamclient.base.ClientGCProtobufMessage;
import com.avenga.steamclient.base.ClientMessageProtobuf;
import com.avenga.steamclient.protobufs.dota.DotaGCMessagesClient.CMsgClientToGCGetProfileCard;
import com.avenga.steamclient.protobufs.dota.DotaGCMessagesClient.CMsgGCMatchDetailsRequest;
import com.avenga.steamclient.protobufs.dota.DotaGCMessagesId.EDOTAGCMsg;
import com.avenga.steamclient.protobufs.dota.GCSdkGCMessages.CMsgClientHello;
import com.avenga.steamclient.protobufs.dota.GCSdkGCMessages.ESourceEngine;
import com.avenga.steamclient.protobufs.steamclient.SteammessagesClientserver.CMsgClientGamesPlayed;
import com.avenga.steamclient.protobufs.tf.GCSystemMessages.EGCBaseClientMsg;
import com.avenga.steamclient.steam.client.SteamClient;
import com.avenga.steamclient.steam.client.callback.ConnectedClientCallbackHandler;
import com.avenga.steamclient.steam.client.callback.GamePlayedClientCallbackHandler;
import com.avenga.steamclient.steam.coordinator.GameCoordinator;
import com.avenga.steamclient.steam.coordinator.callback.GCSessionCallbackHandler;
import com.avenga.steamclient.steam.coordinator.callback.MatchDetailsCallbackHandler;
import com.avenga.steamclient.steam.coordinator.callback.ProfileCardCallbackHandler;
import com.avenga.steamclient.steam.steamuser.LogOnDetails;
import com.avenga.steamclient.steam.steamuser.SteamUser;
import com.avenga.steamclient.steam.steamuser.callback.UserLogOnCallbackHandler;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import static com.avenga.steamclient.enums.EMsg.ClientGamesPlayed;
import static com.avenga.steamclient.enums.EMsg.ClientServiceCall;
import static com.avenga.steamclient.protobufs.dota.DotaGCMessagesId.EDOTAGCMsg.*;
import static com.avenga.steamclient.protobufs.tf.GCSystemMessages.EGCBaseClientMsg.k_EMsgGCClientHello;

public class SteamLogOn {
    private static final int DOTA_2_APP_ID = 570;

    public static void main(String[] args) throws InterruptedException {
        Logger.getRootLogger().setLevel(Level.DEBUG);
        BasicConfigurator.configure();
        var steamClient = new SteamClient();

        var connectedCallback = steamClient.addCallbackToQueue(ConnectedClientCallbackHandler.CALLBACK_MESSAGE_CODE);
        steamClient.connect();
        ConnectedClientCallbackHandler.handle(connectedCallback);
        System.out.println("Connected!");

        var userLogOnCallback = steamClient.addCallbackToQueue(UserLogOnCallbackHandler.CALLBACK_MESSAGE_CODE);
        var details = new LogOnDetails();
        details.setUsername(args[0]);
        details.setPassword(args[1]);

        var steamUser = new SteamUser(steamClient);
        steamUser.logOn(details);

        var response = UserLogOnCallbackHandler.handle(userLogOnCallback);
        System.out.println("Result of response: " + response.getResult().name());

        var gamePlayedCallback = steamClient.addCallbackToQueue(ClientServiceCall.code());
        var gamePlayedRequest = new ClientMessageProtobuf<CMsgClientGamesPlayed.Builder>(CMsgClientGamesPlayed.class, ClientGamesPlayed);
        var gamePlayed = CMsgClientGamesPlayed.GamePlayed.newBuilder()
                .setGameId(570)
                .build();
        gamePlayedRequest.getBody().addGamesPlayed(gamePlayed);
        steamClient.send(gamePlayedRequest);

        GamePlayedClientCallbackHandler.handle(gamePlayedCallback);

        var gameCoordinator = new GameCoordinator(steamClient);

        var gcSessionCallback = gameCoordinator.addCallback(EGCBaseClientMsg.k_EMsgGCClientWelcome.getNumber());
        var clientHelloMessage = new ClientGCProtobufMessage<CMsgClientHello.Builder>(CMsgClientHello.class, k_EMsgGCClientHello.getNumber());
        clientHelloMessage.getBody().setEngine(ESourceEngine.k_ESE_Source2);
        gameCoordinator.send(clientHelloMessage, DOTA_2_APP_ID, k_EMsgGCClientHello.getNumber());
        var gcSession = GCSessionCallbackHandler.handle(gcSessionCallback).getBody().build();

        var matchDetailsCallback = gameCoordinator.addCallback(k_EMsgGCMatchDetailsResponse.getNumber());
        var matchRequestMessage = new ClientGCProtobufMessage<CMsgGCMatchDetailsRequest.Builder>(CMsgGCMatchDetailsRequest.class, EDOTAGCMsg.k_EMsgGCMatchDetailsRequest.getNumber());
        matchRequestMessage.getBody().setMatchId(5194418928L);
        gameCoordinator.send(matchRequestMessage, DOTA_2_APP_ID, EDOTAGCMsg.k_EMsgGCMatchDetailsRequest.getNumber());
        var matchDetails = MatchDetailsCallbackHandler.handle(matchDetailsCallback).getBody().build();

        var profileCardCallback = gameCoordinator.addCallback(k_EMsgClientToGCGetProfileCardResponse.getNumber());
        var profileCardMessage = new ClientGCProtobufMessage<CMsgClientToGCGetProfileCard.Builder>(CMsgClientToGCGetProfileCard.class, k_EMsgClientToGCGetProfileCard.getNumber());
        profileCardMessage.getBody().setAccountId(124801257);
        gameCoordinator.send(profileCardMessage, DOTA_2_APP_ID, k_EMsgClientToGCGetProfileCard.getNumber());
        var profileCard = ProfileCardCallbackHandler.handle(profileCardCallback).getBody().build();

        steamUser.logOff();
    }
}
