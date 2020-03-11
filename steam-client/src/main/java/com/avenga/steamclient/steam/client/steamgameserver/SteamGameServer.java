package com.avenga.steamclient.steam.client.steamgameserver;

import com.avenga.steamclient.base.ClientMessageProtobuf;
import com.avenga.steamclient.base.PacketMessage;
import com.avenga.steamclient.constant.Constant;
import com.avenga.steamclient.constant.ServiceMethodConstant;
import com.avenga.steamclient.exception.CallbackTimeoutException;
import com.avenga.steamclient.model.steam.ClientHandler;
import com.avenga.steamclient.protobufs.steamclient.SteammessagesClientserver.CMsgClientGamesPlayed;
import com.avenga.steamclient.steam.client.callback.GamePlayedClientCallbackHandler;

import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

import static com.avenga.steamclient.enums.EMsg.ClientGamesPlayedWithDataBlob;
import static com.avenga.steamclient.enums.EMsg.ServiceMethod;

public class SteamGameServer extends ClientHandler {

    /**
     * Informs Steam about games in which user currently playing.
     *
     * @param applicationIds IDs of the applications user in game.
     * @param timeout The time which callback handler will wait before cancel it, in milliseconds.
     * @throws CallbackTimeoutException if the wait timed out
     * @return CompletableFuture Callback with {@link PacketMessage} contains information of the last played game.
     */
    public CompletableFuture<PacketMessage> setClientPlayedGame(List<Integer> applicationIds) {
        validatePlayedGameParameters(applicationIds);

        var gamePlayedCallback = getClient().addCallbackToQueue(ServiceMethod.code(), getPLayedGameProperties(applicationIds));
        sendPlayedGameMessage(applicationIds);
        return gamePlayedCallback.getCallback();
    }

    /**
     * Informs Steam about games in which user currently playing.
     *
     * @param applicationIds IDs of the applications user in game.
     * @param timeout The time which callback handler will wait before cancel it, in milliseconds.
     * @throws CallbackTimeoutException if the wait timed out
     */
    public void setClientPlayedGame(List<Integer> applicationIds, long timeout) throws CallbackTimeoutException {
        validatePlayedGameParameters(applicationIds);

        var gamePlayedCallback = getClient().addCallbackToQueue(ServiceMethod.code(), getPLayedGameProperties(applicationIds));
        sendPlayedGameMessage(applicationIds);
        GamePlayedClientCallbackHandler.handle(gamePlayedCallback, timeout, getClient());
    }

    private void validatePlayedGameParameters(List<Integer> applicationIds) {
        Objects.requireNonNull(applicationIds, "List of the application ids wasn't provided");
        if (applicationIds.size() > Constant.MAX_PLAYED_GAMES) {
            throw new IllegalArgumentException("Steam only allow " + Constant.MAX_PLAYED_GAMES + " games to be in played status at one time");
        }
    }

    private void sendPlayedGameMessage(List<Integer> applicationIds) {
        var gamePlayedMessage = new ClientMessageProtobuf<CMsgClientGamesPlayed.Builder>(CMsgClientGamesPlayed.class, ClientGamesPlayedWithDataBlob);
        applicationIds.forEach(applicationId -> {
            var gamePlayed = CMsgClientGamesPlayed.GamePlayed.newBuilder()
                    .setGameId(applicationId)
                    .build();
            gamePlayedMessage.getBody().addGamesPlayed(gamePlayed);
        });
        getClient().send(gamePlayedMessage);
    }

    private Properties getPLayedGameProperties(List<Integer> applicationIds) {
        return new Properties() {{
            put(ServiceMethodConstant.PLAYER_LAST_PLAYED_TIMES, getLastApplicationId(applicationIds));
        }};
    }

    private int getLastApplicationId(List<Integer> applicationIds) {
        var lastApplicationId = 0;
        if (!applicationIds.isEmpty()) {
            lastApplicationId = applicationIds.get(applicationIds.size() - 1);
        }

        return lastApplicationId;
    }
}
