package com.avenga.steamclient.network;

import com.avenga.steamclient.enums.ProtocolType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class WebSocketConnection extends Connection implements WebSocketCMClient.WSListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketConnection.class);

    private AtomicReference<WebSocketCMClient> client = new AtomicReference<>(null);

    private AtomicBoolean userInitiated = new AtomicBoolean();

    private Proxy connectionProxy;

    private boolean isConnectionFailure;

    public WebSocketConnection(Proxy proxy) {
        this.connectionProxy = proxy;
    }

    @Override
    public void connect(InetSocketAddress endPoint, int timeout) {
        LOGGER.debug("Connecting to " + endPoint + "...");
        WebSocketCMClient newClient = new WebSocketCMClient(getUri(endPoint), timeout, this);
        WebSocketCMClient oldClient = client.getAndSet(newClient);
        if (oldClient != null) {
            LOGGER.debug("Attempted to connect while already connected. Closing old connection...");
            oldClient.close();
        }

        initConnnectionProxy(newClient);
        newClient.connect();
    }

    @Override
    public void disconnect() {
        disconnectCore(true);
    }

    @Override
    public void send(byte[] data) {
        try {
            client.get().send(data);
        } catch (Exception e) {
            LOGGER.debug("Exception while sending data", e);
            disconnectCore(false);
        }
    }

    @Override
    public InetAddress getLocalIP() {
        return client.get().getLocalSocketAddress().getAddress();
    }

    @Override
    public InetSocketAddress getCurrentEndPoint() {
        return client.get().getRemoteSocketAddress();
    }

    @Override
    public ProtocolType getProtocolTypes() {
        return ProtocolType.WEB_SOCKET;
    }

    private void disconnectCore(boolean userInitiated) {
        WebSocketCMClient oldClient = client.getAndSet(null);

        if (oldClient != null) {
            oldClient.close();
            this.userInitiated.set(userInitiated);
        }
    }

    private static URI getUri(InetSocketAddress address) {
        return URI.create("wss://" + address.getHostString() + ":" + address.getPort() + "/cmsocket/");
    }

    @Override
    public void onData(byte[] data) {
        if (data != null && data.length > 0) {
            onNetMsgReceived(new NetMsgEventArgs(data, getCurrentEndPoint()));
        }
    }

    @Override
    public void onClose(boolean remote) {
        onDisconnected(userInitiated.get() && !remote, isConnectionFailure);
    }

    @Override
    public void onError(Exception ex) {
        LOGGER.debug("error in websocket", ex);
        if (ex instanceof IOException) {
            this.isConnectionFailure = true;
        }
    }

    @Override
    public void onOpen() {
        LOGGER.debug("Connected to " + getCurrentEndPoint());
        onConnected();
    }

    private void initConnnectionProxy(WebSocketCMClient client) {
        if (Objects.nonNull(connectionProxy)) {
            client.setProxy(connectionProxy);
        }
    }
}
