package com.avenga.steamclient.steam.dota;

import com.avenga.steamclient.base.ClientGCProtobufMessage;
import com.avenga.steamclient.base.ClientMessageProtobuf;
import com.avenga.steamclient.exception.CallbackTimeoutException;
import com.avenga.steamclient.model.steam.gamecoordinator.dota.account.DotaProfileCard;
import com.avenga.steamclient.model.steam.gamecoordinator.dota.match.DotaMatchDetails;
import com.avenga.steamclient.protobufs.dota.GCSdkGCMessages.CMsgClientHello;
import com.avenga.steamclient.protobufs.dota.GCSdkGCMessages.ESourceEngine;
import com.avenga.steamclient.protobufs.steamclient.SteammessagesClientserver.CMsgClientGamesPlayed;
import com.avenga.steamclient.steam.client.SteamClient;
import com.avenga.steamclient.steam.client.callback.GamePlayedClientCallbackHandler;
import com.avenga.steamclient.steam.coordinator.AbstractGameCoordinator;
import com.avenga.steamclient.steam.coordinator.callback.GCSessionCallbackHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

import static com.avenga.steamclient.enums.EMsg.ClientGameConnectTokens;
import static com.avenga.steamclient.enums.EMsg.ClientGamesPlayed;
import static com.avenga.steamclient.protobufs.tf.GCSystemMessages.EGCBaseClientMsg.k_EMsgGCClientHello;
import static com.avenga.steamclient.protobufs.tf.GCSystemMessages.EGCBaseClientMsg.k_EMsgGCClientWelcome;

public abstract class AbstractDotaClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractDotaClient.class);

    protected final AbstractGameCoordinator gameCoordinator;

    /**
     * Time which init callback handlers will wait for packet message from Steam Network server
     */
    protected long callbackWaitTimeout;
    protected int applicationId;

    public AbstractDotaClient(AbstractGameCoordinator gameCoordinator, int applicationId, long callbackWaitTimeout) throws CallbackTimeoutException {
        this.gameCoordinator = gameCoordinator;
        this.applicationId = applicationId;
        this.callbackWaitTimeout = callbackWaitTimeout;
        setClientPlayedGame();
        initGCSession();
    }

    public SteamClient getClient() {
        return gameCoordinator.getClient();
    }

    protected void setClientPlayedGame() throws CallbackTimeoutException {
        var gamePlayedCallback = getClient().addCallbackToQueue(ClientGameConnectTokens.code());
        var gamePlayedMessage = new ClientMessageProtobuf<CMsgClientGamesPlayed.Builder>(CMsgClientGamesPlayed.class, ClientGamesPlayed);
        var gamePlayed = CMsgClientGamesPlayed.GamePlayed.newBuilder()
                .setGameId(applicationId)
                .build();
        gamePlayedMessage.getBody().addGamesPlayed(gamePlayed);
        getClient().send(gamePlayedMessage);
        GamePlayedClientCallbackHandler.handle(gamePlayedCallback, callbackWaitTimeout);
    }

    protected void initGCSession() throws CallbackTimeoutException {
        var gcSessionCallback = getClient().addGCCallbackToQueue(k_EMsgGCClientWelcome.getNumber(), applicationId);
        var clientHelloMessage = new ClientGCProtobufMessage<CMsgClientHello.Builder>(CMsgClientHello.class, k_EMsgGCClientHello.getNumber());
        clientHelloMessage.getBody().setEngine(ESourceEngine.k_ESE_Source2);
        gameCoordinator.send(clientHelloMessage, applicationId, k_EMsgGCClientHello);
        GCSessionCallbackHandler.handle(gcSessionCallback, callbackWaitTimeout);
    }

    public abstract CompletableFuture<DotaMatchDetails> getMatchDetails(long matchId);

    public abstract DotaMatchDetails getMatchDetails(long matchId, long timeout) throws CallbackTimeoutException;

    public abstract CompletableFuture<DotaProfileCard> getAccountProfileCard(int accountId);

    public abstract DotaProfileCard getAccountProfileCard(int accountId, long timeout) throws CallbackTimeoutException;
}
