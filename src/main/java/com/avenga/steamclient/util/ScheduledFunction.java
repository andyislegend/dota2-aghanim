package com.avenga.steamclient.util;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ScheduledFunction {

    private ScheduledExecutorService scheduledExecutorService;
    private long delay;
    private Runnable function;
    private Future<?> future;

    public ScheduledFunction(Runnable function, long delay) {
        this.delay = delay;
        this.function = function;
    }

    public void start() {
        if (future == null) {
            scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
            this.future = scheduledExecutorService.schedule(function, delay, TimeUnit.MILLISECONDS);
        }
    }

    public void stop() {
        if (future != null) {
            this.future.cancel(true);
            this.scheduledExecutorService.shutdown();
            this.future = null;
        }
    }

    public void setDelay(long delay) {
        this.delay = delay;
    }
}
