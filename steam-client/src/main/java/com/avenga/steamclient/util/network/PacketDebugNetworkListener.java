package com.avenga.steamclient.util.network;

import com.avenga.steamclient.enums.EMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Dump any network messages sent to and received from the Steam server that the client is connected to.
 * These messages are dumped to file, and can be analyzed further with NetHookAnalyzer2, a hex editor, or your own purpose-built tools.
 * @see <a href="https://github.com/SteamRE/SteamKit/tree/master/Resources/NetHookAnalyzer2">NetHookAnalyzer2</a>
 *
 * Be careful with this, sensitive data may be written to the disk (such as your Steam password).
 */
public class PacketDebugNetworkListener implements DebugNetworkListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(PacketDebugNetworkListener.class);

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd_MMM_yyyy_HH_mm_ss_SSS").withZone(ZoneOffset.UTC);
    private static final String NETWORK_LOGGER_FOLDER = "netlogs";
    private static final String RECEIVED_PACKET_FOLDER = "in";
    private static final String SENT_PACKET_FOLDER = "out";

    private AtomicLong messageNumber = new AtomicLong();

    private File logDirectory;

    public PacketDebugNetworkListener() {
        this(NETWORK_LOGGER_FOLDER);
    }

    public PacketDebugNetworkListener(String path) {
        File dir = new File(path);
        LOGGER.debug("Creating network listener folder: " + dir.getAbsolutePath());
        dir.mkdir();

        logDirectory = new File(dir, FORMATTER.format(Instant.now()));
        logDirectory.mkdir();
    }

    @Override
    public void onPacketMessageReceived(EMsg msgType, byte[] data) {
        try {
            Files.write(Paths.get(new File(logDirectory, getFile(RECEIVED_PACKET_FOLDER, msgType)).getAbsolutePath()), data);
        } catch (IOException e) {
            LOGGER.debug(e.getMessage(), e);
        }
    }

    @Override
    public void onPacketMessageSent(EMsg msgType, byte[] data) {
        try {
            Files.write(Paths.get(new File(logDirectory, getFile(SENT_PACKET_FOLDER, msgType)).getAbsolutePath()), data);
        } catch (IOException e) {
            LOGGER.debug(e.getMessage(), e);
        }
    }

    private String getFile(String direction, EMsg msgType) {
        return String.format("%d_%s_%d_k_EMsg%s.bin", messageNumber.getAndIncrement(), direction, msgType.code(), msgType.toString());
    }
}
