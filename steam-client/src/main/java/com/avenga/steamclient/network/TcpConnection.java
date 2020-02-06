package com.avenga.steamclient.network;

import com.avenga.steamclient.enums.ProtocolType;
import com.avenga.steamclient.util.stream.BinaryReader;
import com.avenga.steamclient.util.stream.BinaryWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

public class TcpConnection extends Connection {

    private static final Logger logger = LoggerFactory.getLogger(TcpConnection.class);

    private static final int MAGIC = 0x31305456; // "VT01"

    private Socket socket;

    private InetSocketAddress currentEndPoint;

    private BinaryWriter netWriter;

    private BinaryReader netReader;

    private Thread netThread;

    private NetLoop netLoop;

    private final Object netLock = new Object();

    private void shutdown() {
        try {
            if (socket.isConnected()) {
                socket.shutdownInput();
                socket.shutdownOutput();
            }
        } catch (IOException e) {
            logger.debug(e.getMessage(), e);
        }
    }

    private void connectionCompleted(boolean success) {
        if (!success) {
            logger.debug("Timed out while connecting to " + currentEndPoint);
            release(false);
            return;
        }

        logger.debug("Connected to " + currentEndPoint);

        try {
            synchronized (netLock) {
                netReader = new BinaryReader(socket.getInputStream());
                netWriter = new BinaryWriter(socket.getOutputStream());

                netLoop = new NetLoop();
                netThread = new Thread(netLoop, "TcpConnection Thread");

                currentEndPoint = new InetSocketAddress(socket.getInetAddress(), socket.getPort());
            }

            netThread.start();

            onConnected();
        } catch (IOException e) {
            logger.debug("Exception while setting up connection to " + currentEndPoint, e);
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

        onDisconnected(userRequestedDisconnect);
    }

    @Override
    public void connect(InetSocketAddress endPoint, int timeout) {
        synchronized (netLock) {
            currentEndPoint = endPoint;
            try {
                logger.debug("Connecting to " + currentEndPoint + "...");
                socket = new Socket();
                socket.connect(endPoint, timeout);

                connectionCompleted(true);
            } catch (IOException e) {
                logger.debug("Socket exception while completing connection request to " + currentEndPoint, e);
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
                logger.debug("Attempting to send client data when not connected.");
                return;
            }

            try {
                netWriter.writeInt(data.length);
                netWriter.writeInt(MAGIC);
                netWriter.write(data);
            } catch (IOException e) {
                logger.debug("Socket exception while writing data.", e);

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
                    logger.debug("Thread interrupted", e);
                }

                if (cancelRequested) {
                    break;
                }

                boolean canRead;

                try {
                    canRead = netReader.available() > 0;
                } catch (IOException e) {
                    logger.debug("Socket exception while polling", e);
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
                    logger.debug("Socket exception occurred while reading packet", e);
                    break;
                }
            }

            if (cancelRequested) {
                shutdown();
            }
            release(cancelRequested && userRequested);
        }
    }
}