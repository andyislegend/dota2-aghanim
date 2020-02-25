package com.avenga.steamclient.model.discovery;

import com.avenga.steamclient.enums.ProtocolType;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class ServerInfo {

    private ServerRecord record;
    private ProtocolType protocol;
    private Instant lastBadConnection;

    public ServerInfo(ServerRecord record, ProtocolType protocol) {
        this.record = record;
        this.protocol = protocol;
    }
}
