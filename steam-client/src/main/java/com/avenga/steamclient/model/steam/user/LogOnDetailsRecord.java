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
    private static final int PROXY_MAX_RATE_LIMIT_FAILURES = 5;
    private static final long UNAVAILABLE_ACCOUNT_BAN_PERIOD = 1;

    private LogOnDetails logOnDetails;
    private Instant blockedTime;
    private boolean permanentlyBlocked;
    private int rateLimitFailuers;

    public LogOnDetailsRecord(LogOnDetails logOnDetails) {
        this.logOnDetails = logOnDetails;
    }

    public void overLogOnLimitBlock(boolean isProxyEnabled) {
        if (isProxyEnabled && rateLimitFailuers < PROXY_MAX_RATE_LIMIT_FAILURES) {
            this.blockFor(RECONNECT_TIMEOUT);
            rateLimitFailuers++;
        } else {
            this.blockedTime = Instant.now().plus(LOG_ON_OVER_LIMIT_BLOCKED_TIME, ChronoUnit.HOURS);
            rateLimitFailuers++;
        }
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
        if (rateLimitFailuers > PROXY_MAX_RATE_LIMIT_FAILURES) {
            resetRateLimitFailures();
        }
        this.blockedTime = null;
    }

    public void blockPermanently() {
        this.permanentlyBlocked = true;
    }

    public void blockUnavailableAccount() {
        this.blockedTime = Instant.now().plus(UNAVAILABLE_ACCOUNT_BAN_PERIOD, ChronoUnit.DAYS);
    }

    public void resetRateLimitFailures() {
        this.rateLimitFailuers = 0;
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
