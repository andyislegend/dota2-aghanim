package com.avenga.steamclient;

import com.avenga.steamclient.steam.client.SteamClient;
import com.avenga.steamclient.steam.coordinator.impl.GameCoordinator;
import com.avenga.steamclient.steam.dota.impl.DotaClient;
import com.avenga.steamclient.steam.steamuser.LogOnDetails;
import com.avenga.steamclient.steam.steamuser.SteamUser;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class DotaClientSample {

    public static void main(String[] args) {
        Logger.getRootLogger().setLevel(Level.DEBUG);
        BasicConfigurator.configure();
        var steamClient = new SteamClient();

        steamClient.connect();
        System.out.println("Connected!");

        var details = new LogOnDetails();
        details.setUsername(args[0]);
        details.setPassword(args[1]);

        var steamUser = new SteamUser(steamClient);
        var logOnResponse = steamUser.logOn(details);
        System.out.println("Result of logOn response: " + logOnResponse.getResult().name());

        var dotaClient = new DotaClient(new GameCoordinator(steamClient));
        var matchDetails = dotaClient.getMatchDetails(5194418928L);
        var profileCard = dotaClient.getAccountProfileCard(124801257);
        steamUser.logOff();
    }
}
