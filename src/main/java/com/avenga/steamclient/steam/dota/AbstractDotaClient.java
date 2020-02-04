package com.avenga.steamclient.steam.dota;

import com.avenga.steamclient.base.ClientGCProtobufMessage;
import com.avenga.steamclient.base.ClientMessageProtobuf;
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
    protected static final int DOTA_2_APP_ID = 570;
    protected final AbstractGameCoordinator gameCoordinator;

    public AbstractDotaClient(AbstractGameCoordinator gameCoordinator) {
        this.gameCoordinator = gameCoordinator;
        setClientPlayedGame();
        initGCSession();
    }

    protected void setClientPlayedGame() {
        var client = gameCoordinator.getClient();
        var gamePlayedCallback = client.addCallbackToQueue(ClientGameConnectTokens.code());
        var gamePlayedMessage = new ClientMessageProtobuf<CMsgClientGamesPlayed.Builder>(CMsgClientGamesPlayed.class, ClientGamesPlayed);
        var gamePlayed = CMsgClientGamesPlayed.GamePlayed.newBuilder()
                .setGameId(DOTA_2_APP_ID)
                .build();
        gamePlayedMessage.getBody().addGamesPlayed(gamePlayed);
        client.send(gamePlayedMessage);
        GamePlayedClientCallbackHandler.handle(gamePlayedCallback);
    }

    protected void initGCSession() {
        var gcSessionCallback = gameCoordinator.addCallback(k_EMsgGCClientWelcome.getNumber());
        var clientHelloMessage = new ClientGCProtobufMessage<CMsgClientHello.Builder>(CMsgClientHello.class, k_EMsgGCClientHello.getNumber());
        clientHelloMessage.getBody().setEngine(ESourceEngine.k_ESE_Source2);
        gameCoordinator.send(clientHelloMessage, DOTA_2_APP_ID, k_EMsgGCClientHello.getNumber());
        GCSessionCallbackHandler.handle(gcSessionCallback).getBody().build();
    }

    public abstract CMsgGCMatchDetailsResponse getMatchDetails(long matchId);

    public abstract CMsgDOTAProfileCard getAccountProfileCard(int accountId);
}
