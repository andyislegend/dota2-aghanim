package com.avenga.steamclient;

import com.avenga.steamclient.steam.client.SteamClient;
import com.avenga.steamclient.steam.client.callback.ConnectedClientCallbackHandler;
import com.avenga.steamclient.steam.coordinator.impl.GameCoordinator;
import com.avenga.steamclient.steam.dota.impl.DotaClient;
import com.avenga.steamclient.steam.steamuser.LogOnDetails;
import com.avenga.steamclient.steam.steamuser.SteamUser;
import com.avenga.steamclient.steam.steamuser.callback.UserLogOnCallbackHandler;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class DotaClientSample {

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

        var dotaClient = new DotaClient(new GameCoordinator(steamClient));
        var matchDetails = dotaClient.getMatchDetails(5194418928L);
        var profileCard = dotaClient.getAccountProfileCard(124801257);
        steamUser.logOff();
    }
}
