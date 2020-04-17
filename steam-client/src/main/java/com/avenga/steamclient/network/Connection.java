package com.avenga.steamclient.network;

import com.avenga.steamclient.enums.ProtocolType;
import com.avenga.steamclient.event.Event;
import com.avenga.steamclient.event.EventArgs;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public abstract class Connection {

    private static final int DEFAULT_TIMEOUT = 5000;

    /**
     * Occurs when a net message is received over the network.
     */
    Event<NetMsgEventArgs> netMsgReceived = new Event<>();

    /**
     * Occurs when the physical connection is established.
     */
    Event<EventArgs> connected = new Event<>();

    /**
     * Occurs when the physical connection is broken.
     */
    Event<DisconnectedEventArgs> disconnected = new Event<>();

    void onNetMsgReceived(NetMsgEventArgs e) {
        if (netMsgReceived != null) {
            netMsgReceived.handleEvent(this, e);
        }
    }

    void onConnected() {
        if (connected != null) {
            connected.handleEvent(this, null);
        }
    }

    void onDisconnected(boolean userInitiated) {
        if (disconnected != null) {
            disconnected.handleEvent(this, new DisconnectedEventArgs(userInitiated));
        }
    }

    void onDisconnected(boolean userInitiated, boolean isConnectionFailure) {
        if (disconnected != null) {
            disconnected.handleEvent(this, new DisconnectedEventArgs(userInitiated, isConnectionFailure));
        }
    }

    /**
     * Connects to the specified end point.
     *
     * @param endPoint The end point to connect to.
     * @param timeout  Timeout in milliseconds
     */
    public abstract void connect(InetSocketAddress endPoint, int timeout);

    /**
     * Connects to the specified end point.
     *
     * @param endPoint The end point to connect to.
     */
    public final void connect(InetSocketAddress endPoint) {
        connect(endPoint, DEFAULT_TIMEOUT);
    }

    /**
     * Disconnects this instance.
     */
    public abstract void disconnect();

    /**
     * Sends the specified data packet.
     *
     * @param data The data packet to send.
     */
    public abstract void send(byte[] data);

    /**
     * Gets the local IP.
     *
     * @return The local IP.
     */
    public abstract InetAddress getLocalIP();

    public abstract InetSocketAddress getCurrentEndPoint();

    /**
     * @return The type of communication protocol that this connection uses.
     */
    public abstract ProtocolType getProtocolTypes();

    public Event<NetMsgEventArgs> getNetMsgReceived() {
        return netMsgReceived;
    }

    public Event<EventArgs> getConnected() {
        return connected;
    }

    public Event<DisconnectedEventArgs> getDisconnected() {
        return disconnected;
    }
}
