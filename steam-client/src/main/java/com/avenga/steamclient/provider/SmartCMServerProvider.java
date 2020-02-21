package com.avenga.steamclient.provider;

import com.avenga.steamclient.enums.ProtocolType;
import com.avenga.steamclient.enums.ServerQuality;
import com.avenga.steamclient.model.configuration.SteamConfiguration;
import com.avenga.steamclient.model.discovery.ServerInfo;
import com.avenga.steamclient.model.discovery.ServerRecord;
import com.avenga.steamclient.steam.webapi.SteamDirectoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.*;

/**
 * Smart list of CM servers.
 */
public class SmartCMServerProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(SmartCMServerProvider.class);

    private final SteamConfiguration configuration;

    private List<ServerInfo> servers = Collections.synchronizedList(new ArrayList<>());
    private Long badConnectionMemoryTimeSpan;

    public SmartCMServerProvider(SteamConfiguration configuration) {
        Objects.requireNonNull(configuration, "Steam configuration wasn't provided");

        this.configuration = configuration;
    }

    private void startFetchingServers() throws IOException {
        if (!servers.isEmpty()) {
            return;
        }

        resolveServerList();
    }

    private void resolveServerList() throws IOException {
        LOGGER.debug("Resolving server list");

        List<ServerRecord> endPoints = configuration.getServerListProvider().fetchServerList();
        if (endPoints == null) {
            endPoints = new ArrayList<>();
        }

        if (endPoints.isEmpty() && configuration.isAllowDirectoryFetch()) {
            LOGGER.debug("Server list provider had no entries, will query SteamDirectory");
            endPoints = SteamDirectoryService.getServers(configuration);
        }

        LOGGER.debug("Resolved " + endPoints.size() + " servers");
        replaceList(endPoints);
    }

    /**
     * Resets the scores of all servers which has a last bad connection more than {@link SmartCMServerProvider#badConnectionMemoryTimeSpan} ago.
     */
    public void resetOldScores() {
        if (badConnectionMemoryTimeSpan == null) {
            return;
        }

        final long cutoff = System.currentTimeMillis() - badConnectionMemoryTimeSpan;

        for (ServerInfo serverInfo : servers) {
            if (serverInfo.getLastBadConnection() != null && serverInfo.getLastBadConnection().toEpochMilli() < cutoff) {
                serverInfo.setLastBadConnection(null);
            }
        }
    }

    /**
     * Replace the list with a new list of servers provided to us by the Steam servers.
     *
     * @param endPoints The {@link ServerRecord ServerRecords} to use for this {@link SmartCMServerProvider}.
     */
    public void replaceList(List<ServerRecord> endPoints) {
        Objects.requireNonNull(endPoints, "Server record endpoints wasn't provided");

        servers.clear();
        endPoints.forEach(this::addCore);
        configuration.getServerListProvider().updateServerList(endPoints);
    }

    private void addCore(ServerRecord endPoint) {
        endPoint.getProtocolTypes().forEach(protocol -> servers.add(new ServerInfo(endPoint, protocol)));
    }

    public boolean tryMark(InetSocketAddress endPoint, ProtocolType protocolTypes, ServerQuality quality) {
        return tryMark(endPoint, EnumSet.of(protocolTypes), quality);
    }

    public boolean tryMark(InetSocketAddress endPoint, EnumSet<ProtocolType> protocolTypes, ServerQuality quality) {
        List<ServerInfo> serverInfos = new ArrayList<>();
        servers.stream()
                .filter(serverInfo -> serverInfo.getRecord().getEndpoint().equals(endPoint)
                        && protocolTypes.contains(serverInfo.getProtocol()))
                .forEach(serverInfos::add);

        serverInfos.forEach(serverInfo -> {
            LOGGER.debug("Marking " + serverInfo.getRecord().getEndpoint() + " - " + serverInfo.getProtocol() + " as " + quality);
            markServerCore(serverInfo, quality);
        });

        return serverInfos.size() > 0;
    }

    private void markServerCore(ServerInfo serverInfo, ServerQuality quality) {
        switch (quality) {
            case GOOD:
                serverInfo.setLastBadConnection(null);
                break;
            case BAD:
                serverInfo.setLastBadConnection(Instant.now());
                break;
        }
    }

    /**
     * Perform the actual score lookup of the server list and return the candidate.
     *
     * @param supportedProtocolTypes The minimum supported {@link ProtocolType} of the server to return.
     * @return An {@link ServerRecord}, or null if the list is empty.
     */
    private ServerRecord getNextServerCandidateInternal(EnumSet<ProtocolType> supportedProtocolTypes) {
        resetOldScores();

        List<ServerInfo> serverInfos = new ArrayList<>();
        servers.stream()
                .filter(serverInfo -> supportedProtocolTypes.contains(serverInfo.getProtocol()))
                .forEach(serverInfos::add);

        serverInfos.sort((o1, o2) -> {
            if (o1.getLastBadConnection() == null && o2.getLastBadConnection() == null) {
                return 1;
            }

            if (o1.getLastBadConnection() == null) {
                return -1;
            }

            if (o2.getLastBadConnection() == null) {
                return 1;
            }

            if (o2.getLastBadConnection().equals(o1.getLastBadConnection())) {
                return 0;
            }

            return o1.getLastBadConnection().isBefore(o2.getLastBadConnection()) ? -1 : 1;
        });

        if (serverInfos.isEmpty()) {
            return null;
        }

        ServerInfo result = serverInfos.get(0);

        return new ServerRecord(result.getRecord().getEndpoint(), result.getProtocol());
    }

    /**
     * Get the next server in the list.
     *
     * @param supportedProtocolTypes The minimum supported {@link ProtocolType} of the server to return.
     * @return An {@link ServerRecord}, or null if the list is empty.
     */
    public ServerRecord getNextServerCandidate(EnumSet<ProtocolType> supportedProtocolTypes) {
        try {
            startFetchingServers();
        } catch (IOException e) {
            return null;
        }

        return getNextServerCandidateInternal(supportedProtocolTypes);
    }
}
