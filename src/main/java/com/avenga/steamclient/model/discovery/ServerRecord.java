package com.avenga.steamclient.model.discovery;

import com.avenga.steamclient.enums.ProtocolType;

import java.net.InetSocketAddress;
import java.util.EnumSet;
import java.util.Objects;

/**
 * Represents the information needed to connect to a CM server
 */
public class ServerRecord {

    private InetSocketAddress endpoint;
    private EnumSet<ProtocolType> protocolTypes;

    public ServerRecord(InetSocketAddress endpoint, ProtocolType protocolType) {
        Objects.requireNonNull(endpoint, "endpoint wasn't provided");

        this.endpoint = endpoint;
        this.protocolTypes = EnumSet.of(protocolType);
    }

    public ServerRecord(InetSocketAddress socketEndpoint) {
        this.endpoint = socketEndpoint;
        this.protocolTypes = EnumSet.of(ProtocolType.TCP, ProtocolType.UDP);
    }

    public ServerRecord(String webSocketAddress) {
        Objects.requireNonNull(webSocketAddress, "webSocketAddress wasn't provided");

        final int defaultPort = 443;
        String[] split = webSocketAddress.split(":");
        InetSocketAddress endpoint;

        if (split.length > 1) {
            endpoint = new InetSocketAddress(split[0], Integer.parseInt(split[1]));
        } else {
            endpoint = new InetSocketAddress(webSocketAddress, defaultPort);
        }

        this.endpoint = endpoint;
        this.protocolTypes = EnumSet.of(ProtocolType.WEB_SOCKET);
    }

    public String getHost() {
        return this.endpoint.getHostString();
    }

    public int getPort() {
        return endpoint.getPort();
    }

    public InetSocketAddress getEndpoint() {
        return endpoint;
    }

    public EnumSet<ProtocolType> getProtocolTypes() {
        return protocolTypes;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof ServerRecord)) {
            return false;
        }

        ServerRecord o = (ServerRecord) obj;

        return endpoint.equals(o.endpoint) && protocolTypes.equals(o.protocolTypes);
    }

    @Override
    public int hashCode() {
        return endpoint.hashCode() ^ protocolTypes.hashCode();
    }
}
