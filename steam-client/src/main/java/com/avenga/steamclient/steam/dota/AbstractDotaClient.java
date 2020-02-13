package com.avenga.steamclient.steam.dota;

import com.avenga.steamclient.base.ClientGCProtobufMessage;
import com.avenga.steamclient.base.ClientMessageProtobuf;
import com.avenga.steamclient.exception.CallbackTimeoutException;
import com.avenga.steamclient.protobufs.dota.DotaGCMessagesClient.CMsgGCMatchDetailsResponse;
import com.avenga.steamclient.protobufs.dota.DotaGCMessagesCommon.CMsgDOTAProfileCard;
import com.avenga.steamclient.protobufs.dota.GCSdkGCMessages.CMsgClientHello;
import com.avenga.steamclient.protobufs.dota.GCSdkGCMessages.ESourceEngine;
import com.avenga.steamclient.protobufs.steamclient.SteammessagesClientserver.CMsgClientGamesPlayed;
import com.avenga.steamclient.steam.client.callback.GamePlayedClientCallbackHandler;
import com.avenga.steamclient.steam.coordinator.AbstractGameCoordinator;
import com.avenga.steamclient.steam.coordinator.callback.GCSessionCallbackHandler;

import static com.avenga.steamclient.enums.EMsg.ClientGameConnectTokens;
import static com.avenga.steamclient.enums.EMsg.ClientGamesPlayed;
import static com.avenga.steamclient.protobufs.tf.GCSystemMessages.EGCBaseClientMsg.k_EMsgGCClientHello;
import static com.avenga.steamclient.protobufs.tf.GCSystemMessages.EGCBaseClientMsg.k_EMsgGCClientWelcome;

public abstract class AbstractDotaClient {

    protected final AbstractGameCoordinator gameCoordinator;

    protected long callbackWaitTimeout = 20000;
    protected int applicationId;

    public AbstractDotaClient(AbstractGameCoordinator gameCoordinator, int applicationId) throws CallbackTimeoutException {
        this.gameCoordinator = gameCoordinator;
        this.applicationId = applicationId;
        setClientPlayedGame();
        initGCSession();
    }

    protected void setClientPlayedGame() throws CallbackTimeoutException {
        var client = gameCoordinator.getClient();
        var gamePlayedCallback = client.addCallbackToQueue(ClientGameConnectTokens.code());
        var gamePlayedMessage = new ClientMessageProtobuf<CMsgClientGamesPlayed.Builder>(CMsgClientGamesPlayed.class, ClientGamesPlayed);
        var gamePlayed = CMsgClientGamesPlayed.GamePlayed.newBuilder()
                .setGameId(applicationId)
                .build();
        gamePlayedMessage.getBody().addGamesPlayed(gamePlayed);
        client.send(gamePlayedMessage);
        GamePlayedClientCallbackHandler.handle(gamePlayedCallback, callbackWaitTimeout);
    }

    protected void initGCSession() throws CallbackTimeoutException {
        var gcSessionCallback = gameCoordinator.addCallback(k_EMsgGCClientWelcome.getNumber());
        var clientHelloMessage = new ClientGCProtobufMessage<CMsgClientHello.Builder>(CMsgClientHello.class, k_EMsgGCClientHello.getNumber());
        clientHelloMessage.getBody().setEngine(ESourceEngine.k_ESE_Source2);
        gameCoordinator.send(clientHelloMessage, applicationId, k_EMsgGCClientHello);
        GCSessionCallbackHandler.handle(gcSessionCallback, callbackWaitTimeout).getBody().build();
    }

    /**
     * Set time which init callback handlers will wait for packet message from Steam Network server
     *
     * @param callbackWaitTimeout of the callback handler
     */
    public void setCallbackWaitTimeout(long callbackWaitTimeout) {
        this.callbackWaitTimeout = callbackWaitTimeout;
    }

    public abstract CMsgGCMatchDetailsResponse getMatchDetails(long matchId);

    public abstract CMsgGCMatchDetailsResponse getMatchDetails(long matchId, long timeout) throws CallbackTimeoutException;

    public abstract CMsgDOTAProfileCard getAccountProfileCard(int accountId);

    public abstract CMsgDOTAProfileCard getAccountProfileCard(int accountId, long timeout) throws CallbackTimeoutException;
}
