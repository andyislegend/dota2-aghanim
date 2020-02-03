package com.avenga.steamclient;

import com.avenga.steamclient.base.ClientMessageProtobuf;
import com.avenga.steamclient.protobufs.steamclient.SteammessagesClientserver.CMsgClientGamesPlayed;
import com.avenga.steamclient.steam.client.SteamClient;
import com.avenga.steamclient.steam.client.callback.ConnectedClientCallbackHandler;
import com.avenga.steamclient.steam.client.callback.GamePlayedClientCallbackHandler;
import com.avenga.steamclient.steam.coordinator.GameCoordinator;
import com.avenga.steamclient.steam.steamuser.LogOnDetails;
import com.avenga.steamclient.steam.steamuser.SteamUser;
import com.avenga.steamclient.steam.steamuser.callback.UserLogOnCallbackHandler;
import com.avenga.steamclient.steam.wrapper.DotaClient;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import static com.avenga.steamclient.enums.EMsg.ClientGamesPlayed;
import static com.avenga.steamclient.enums.EMsg.ClientServiceCall;

public class SteamLogOn {

    public static void main(String[] args) {
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

        var dotaClient = new DotaClient(new GameCoordinator(steamClient));
        var session = dotaClient.initSession();
        var matchDetails = dotaClient.getMatchDetails(5194418928L);
        var profileCard = dotaClient.getAccountProfileCard(124801257);
        steamUser.logOff();
    }
}
