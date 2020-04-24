package com.avenga.steamclient.network;

import com.avenga.steamclient.enums.ProtocolType;
import com.avenga.steamclient.util.stream.BinaryReader;
import com.avenga.steamclient.util.stream.BinaryWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.util.Objects;

public class TcpConnection extends Connection {

    private static final Logger LOGGER = LoggerFactory.getLogger(TcpConnection.class);

    private static final int MAGIC = 0x31305456; // "VT01"

    private Socket socket;

    private InetSocketAddress currentEndPoint;

    private BinaryWriter netWriter;

    private BinaryReader netReader;

    private Thread netThread;

    private NetLoop netLoop;

    private Proxy connectionProxy;

    private boolean isConnectionFailure;

    private final Object netLock = new Object();

    public TcpConnection(Proxy proxy, String clientName) {
        this.clientName = clientName;
        this.connectionProxy = proxy;
    }

    private void shutdown() {
        try {
            if (socket.isConnected()) {
                socket.shutdownInput();
                socket.shutdownOutput();
            }
        } catch (IOException e) {
            LOGGER.debug("{}: Socket shutdown exception: {}", clientName, e.getMessage());
        }
    }

    private void connectionCompleted(boolean success) {
        if (!success) {
            LOGGER.debug("{}: Timed out while connecting to {}", clientName, currentEndPoint);
            release(false);
            return;
        }

        LOGGER.debug("{}: Connected to {}", clientName, currentEndPoint);

        try {
            synchronized (netLock) {
                netReader = new BinaryReader(socket.getInputStream());
                netWriter = new BinaryWriter(socket.getOutputStream());

                netLoop = new NetLoop();
                netThread = new Thread(netLoop, "TcpConnection");

                currentEndPoint = new InetSocketAddress(socket.getInetAddress(), socket.getPort());
            }

            netThread.start();

            onConnected();
        } catch (IOException e) {
            LOGGER.debug("{}: Exception while setting up connection to {}: {}", clientName, currentEndPoint, e.getMessage());
            checkAndSetConnectionFailure(e);
            release(false);
        }
    }

    private void release(boolean userRequestedDisconnect) {
        synchronized (netLock) {
            if (netWriter != null) {
                try {
                    netWriter.close();
                } catch (IOException ignored) {
                }
                netWriter = null;
            }

            if (netReader != null) {
                try {
                    netReader.close();
                } catch (IOException ignored) {
                }
                netReader = null;
            }

            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ignored) {
                }
                socket = null;
            }
        }

        onDisconnected(userRequestedDisconnect, isConnectionFailure);
    }

    @Override
    public void connect(InetSocketAddress endPoint, int timeout) {
        synchronized (netLock) {
            currentEndPoint = endPoint;
            try {
                LOGGER.debug("{}: Connecting to {} ...", clientName, currentEndPoint);
                socket = getSocket();
                socket.connect(endPoint, timeout);

                connectionCompleted(true);
            } catch (IOException e) {
                LOGGER.debug("{}: Socket exception while completing connection request to {}: {}",
                        clientName, currentEndPoint, e.getMessage());
                checkAndSetConnectionFailure(e);
                connectionCompleted(false);
            }
        }
    }

    @Override
    public void disconnect() {
        synchronized (netLock) {
            if (netLoop != null) {
                netLoop.stop(true);
            }
        }
    }

    private byte[] readPacket() throws IOException {
        int packetLen = netReader.readInt();
        int packetMagic = netReader.readInt();

        if (packetMagic != MAGIC) {
            throw new IOException("Got a packet with invalid magic!");
        }

        return netReader.readBytes(packetLen);
    }

    @Override
    public void send(byte[] data) {
        synchronized (netLock) {
            if (socket == null) {
                LOGGER.debug("{}: Attempting to send client data when not connected.", clientName);
                return;
            }

            try {
                netWriter.writeInt(data.length);
                netWriter.writeInt(MAGIC);
                netWriter.write(data);
            } catch (IOException e) {
                LOGGER.debug("{}: Socket exception while writing data {}", clientName, e.getMessage());

                // looks like the only the only way to detect a closed connection is to try and write to it
                // afaik read also throws an exception if the connection is open but there is nothing to read
                if (netLoop != null) {
                    netLoop.stop(false);
                }
            }
        }
    }

    @Override
    public InetAddress getLocalIP() {
        synchronized (netLock) {
            return socket == null ? null : socket.getLocalAddress();
        }
    }

    @Override
    public InetSocketAddress getCurrentEndPoint() {
        return currentEndPoint;
    }

    @Override
    public ProtocolType getProtocolTypes() {
        return ProtocolType.TCP;
    }

    /**
     * Nets the loop.
     */
    private class NetLoop implements Runnable {
        private static final int POLL_MS = 100;

        private volatile boolean cancelRequested = false;

        private volatile boolean userRequested = false;

        void stop(boolean userRequested) {
            this.userRequested = userRequested;
            cancelRequested = true;
        }

        @Override
        public void run() {
            while (!cancelRequested) {
                try {
                    Thread.sleep(POLL_MS);
                } catch (InterruptedException e) {
                    LOGGER.debug("{}: Thread interrupted {}", clientName, e.getMessage());
                }

                if (cancelRequested) {
                    break;
                }

                boolean canRead;

                try {
                    canRead = netReader.available() > 0;
                } catch (IOException e) {
                    LOGGER.debug("{}: Socket exception while polling {}", clientName, e.getMessage());
                    break;
                }

                if (!canRead) {
                    // nothing to read yet
                    continue;
                }

                byte[] packData;

                try {
                    packData = readPacket();

                    onNetMsgReceived(new NetMsgEventArgs(packData, currentEndPoint));
                } catch (IOException e) {
                    LOGGER.debug("{}: Socket exception occurred while reading packet {}", clientName, e.getMessage());
                    break;
                }
            }

            if (cancelRequested) {
                shutdown();
            }
            release(cancelRequested && userRequested);
        }
    }

    private Socket getSocket() {
        return Objects.isNull(connectionProxy) ? new Socket() : new Socket(connectionProxy);
    }

    private void checkAndSetConnectionFailure(Exception exception) {
        if (exception instanceof SocketException) {
            this.isConnectionFailure = true;
        }
    }
}
