package com.avenga.steamclient.steam.coordinator;

import com.avenga.steamclient.base.ClientGCMessage;
import com.avenga.steamclient.steam.client.SteamClient;
import com.google.protobuf.ProtocolMessageEnum;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractGameCoordinator {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractGameCoordinator.class);

    @Getter
    protected final SteamClient client;

    public AbstractGameCoordinator(SteamClient client) {
        this.client = client;
    }

    public abstract void send(ClientGCMessage msg, int appId, ProtocolMessageEnum messageEnum);
}
