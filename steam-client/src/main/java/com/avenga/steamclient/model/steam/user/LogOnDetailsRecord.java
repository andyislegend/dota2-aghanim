package com.avenga.steamclient.model.steam.user;

import com.avenga.steamclient.steam.client.steamuser.LogOnDetails;
import lombok.Getter;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Objects;

@Getter
public class LogOnDetailsRecord {
    public static final long RECONNECT_TIMEOUT = 5000;
    private static final long LOG_ON_OVER_LIMIT_BLOCKED_TIME = 6;

    private LogOnDetails logOnDetails;
    private Instant blockedTime;
    private boolean permanentlyBlocked;

    public LogOnDetailsRecord(LogOnDetails logOnDetails) {
        this.logOnDetails = logOnDetails;
    }

    public void overLogOnLimitBlock() {
        this.blockedTime = Instant.now().plus(LOG_ON_OVER_LIMIT_BLOCKED_TIME, ChronoUnit.HOURS);
    }

    public void blockFor(long milliseconds) {
        this.blockedTime = Instant.now().plusMillis(milliseconds);
    }

    public void blockFor(long time, TemporalUnit timeUnit) {
        this.blockedTime = Instant.now().plus(time, timeUnit);
    }

    public boolean isBlocked() {
        return Objects.nonNull(this.blockedTime);
    }

    public void resetBlockedTime() {
        this.blockedTime = null;
    }

    public void blockPermanently() {
        this.permanentlyBlocked = true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LogOnDetailsRecord provided = (LogOnDetailsRecord) o;

        if (!logOnDetails.equals(provided.logOnDetails)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return logOnDetails.hashCode();
    }
}
