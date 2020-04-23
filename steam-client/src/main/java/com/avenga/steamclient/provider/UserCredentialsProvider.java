package com.avenga.steamclient.provider;

import com.avenga.steamclient.model.steam.user.LogOnDetailsRecord;
import com.avenga.steamclient.steam.client.steamuser.LogOnDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class UserCredentialsProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserCredentialsProvider.class);
    private static final int THREAD_SLEEP_TIME = 1;
    private static final long BANNED_TIME_CHECK_PERIOD = 5;

    private final ConcurrentLinkedQueue<LogOnDetailsRecord> credentialRecords;
    private final List<LogOnDetailsRecord> bannedCredentialRecords;
    private ScheduledExecutorService executor;

    public UserCredentialsProvider(List<LogOnDetails> logOnDetails) {
        this.credentialRecords = new ConcurrentLinkedQueue<>();
        this.bannedCredentialRecords = new LinkedList<>();

        logOnDetails.stream().map(LogOnDetailsRecord::new).forEach(this.credentialRecords::add);
        startResetBannedCredentialJob();
    }

    public synchronized LogOnDetailsRecord getNext() {
        LogOnDetailsRecord detailsRecord = null;
        while (detailsRecord == null) {
            detailsRecord = credentialRecords.poll();
            if (detailsRecord == null) {
                LOGGER.info("Waiting for user credentials: {} second(s)", THREAD_SLEEP_TIME);
                try {
                    TimeUnit.SECONDS.sleep(THREAD_SLEEP_TIME);
                } catch (InterruptedException e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
        }
        return detailsRecord;
    }

    public void returnKey(LogOnDetailsRecord detailsRecord) {
        if (detailsRecord.isBlocked() || detailsRecord.isPermanentlyBlocked()) {
            LOGGER.debug("User {} was banned until: {}", detailsRecord.getLogOnDetails().getUsername(), detailsRecord.getBlockedTime());
            bannedCredentialRecords.add(detailsRecord);
        } else {
            credentialRecords.add(detailsRecord);
        }
    }

    public void returnBlockedKey(LogOnDetailsRecord detailsRecord) {
        LOGGER.debug("User {} was banned until: {}", detailsRecord.getLogOnDetails().getUsername(), detailsRecord.getBlockedTime());
        credentialRecords.remove(detailsRecord);
        bannedCredentialRecords.add(detailsRecord);
    }

    public void stopResetBannedCredentialJob() {
        executor.shutdown();
    }

    public void startResetBannedCredentialJob() {
        if (Objects.nonNull(executor) && !executor.isShutdown()) {
            return;
        }
        executor = Executors.newSingleThreadScheduledExecutor();
        resetBannedUserCredentials();
    }

    private void resetBannedUserCredentials() {
        var scheduledFuture = executor.scheduleAtFixedRate(() -> {
            try {
                var currentTime = Instant.now();
                var iterator = bannedCredentialRecords.listIterator();
                while (iterator.hasNext()) {
                    var record = iterator.next();
                    if (Objects.nonNull(record.getBlockedTime()) && currentTime.isAfter(record.getBlockedTime())) {
                        record.resetBlockedTime();
                        credentialRecords.add(record);
                        iterator.remove();
                        LOGGER.debug("User {} was removed from banned list", record.getLogOnDetails().getUsername());
                    }
                }
            } catch (Exception e) {
                LOGGER.debug("Exception in user credentials schedular task: {}", e.getMessage(), e);
            }
        }, THREAD_SLEEP_TIME, BANNED_TIME_CHECK_PERIOD, TimeUnit.SECONDS);
    }
}
