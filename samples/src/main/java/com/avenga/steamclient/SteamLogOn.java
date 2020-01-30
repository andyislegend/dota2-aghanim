package com.avenga.steamclient;

import com.avenga.steamclient.steam.client.SteamClient;
import com.avenga.steamclient.steam.client.callback.ConnectedClientCallbackHandler;
import com.avenga.steamclient.steam.steamuser.LogOnDetails;
import com.avenga.steamclient.steam.steamuser.SteamUser;
import com.avenga.steamclient.steam.steamuser.callback.UserLogOnCallbackHandler;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class SteamLogOn {

    public static void main(String[] args) {
        Logger.getRootLogger().setLevel(Level.DEBUG);
        BasicConfigurator.configure();
        SteamClient steamClient = new SteamClient();

        var connectedCallback = steamClient.addCallbackToQueue(ConnectedClientCallbackHandler.CALLBACK_MESSAGE_CODE);
        steamClient.connect();
        ConnectedClientCallbackHandler.handle(connectedCallback);
        System.out.println("Connected!");

        var userLogOnCallback = steamClient.addCallbackToQueue(UserLogOnCallbackHandler.CALLBACK_MESSAGE_CODE);
        LogOnDetails details = new LogOnDetails();
        details.setUsername(args[0]);
        details.setPassword(args[1]);

        SteamUser steamUser = new SteamUser(steamClient);
        steamUser.logOn(details);

        var response = UserLogOnCallbackHandler.handle(userLogOnCallback);
        System.out.println("Result of response: " + response.getResult().name());
        steamUser.logOff();
    }
}
