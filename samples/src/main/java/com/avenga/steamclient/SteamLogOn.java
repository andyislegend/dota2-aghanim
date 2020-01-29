package com.avenga.steamclient;

import com.avenga.steamclient.steam.client.SteamClient;
import com.avenga.steamclient.steam.steamuser.LogOnDetails;
import com.avenga.steamclient.steam.steamuser.SteamUser;
import com.avenga.steamclient.steam.steamuser.response.handler.UserLogOnResponseHandler;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class SteamLogOn {

    public static void main(String[] args) throws InterruptedException {
        Logger.getRootLogger().setLevel(Level.DEBUG);
        BasicConfigurator.configure();
        SteamClient steamClient = new SteamClient();
        steamClient.connect();

        while (!steamClient.isConnected()) {
            Thread.sleep(1000);
        }

        LogOnDetails details = new LogOnDetails();
        details.setUsername(args[0]);
        details.setPassword(args[1]);

        SteamUser steamUser = new SteamUser(steamClient);
        steamUser.logOn(details);

        UserLogOnResponseHandler userLogOnResponseHandler = new UserLogOnResponseHandler();
        var response = userLogOnResponseHandler.handle(steamClient);
        System.out.println("Result of response: " + response.getResult().name());
        steamUser.logOff();
    }
}
