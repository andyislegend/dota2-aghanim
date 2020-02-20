package com.avenga.steamclient.steam.asyncclient.steamgameserver.callback;

import com.avenga.steamclient.enums.EAuthSessionResponse;
import com.avenga.steamclient.model.GameID;
import com.avenga.steamclient.model.SteamID;
import com.avenga.steamclient.protobufs.steamclient.SteammessagesClientserver.CMsgClientTicketAuthComplete;
import com.avenga.steamclient.steam.asyncclient.callbackmanager.BaseCallbackMessage;

/**
 * This callback is fired when ticket authentication has completed.
 */
public class TicketAuthCallback extends BaseCallbackMessage {
    private SteamID steamID;
    private GameID gameID;
    private int state;
    private EAuthSessionResponse authSessionResponse;
    private int ticketCrc;
    private int ticketSequence;

    public TicketAuthCallback(CMsgClientTicketAuthComplete.Builder tickAuth) {
        steamID = new SteamID(tickAuth.getSteamId());
        gameID = new GameID(tickAuth.getGameId());
        state = tickAuth.getEstate();
        authSessionResponse = EAuthSessionResponse.from(tickAuth.getEauthSessionResponse());
        ticketCrc = tickAuth.getTicketCrc();
        ticketSequence = tickAuth.getTicketSequence();
    }

    /**
     * @return the SteamID the ticket auth completed for
     */
    public SteamID getSteamID() {
        return steamID;
    }

    /**
     * @return the GameID the ticket was for
     */
    public GameID getGameID() {
        return gameID;
    }

    /**
     * @return the authentication state
     */
    public int getState() {
        return state;
    }

    /**
     * @return the auth session response
     */
    public EAuthSessionResponse getAuthSessionResponse() {
        return authSessionResponse;
    }

    /**
     * @return the ticket CRC
     */
    public int getTicketCrc() {
        return ticketCrc;
    }

    /**
     * @return the ticket sequence
     */
    public int getTicketSequence() {
        return ticketSequence;
    }
}
