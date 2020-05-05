package com.avenga.steamclient.steam.client.task;

import java.util.concurrent.CancellationException;
import java.util.function.Predicate;

@FunctionalInterface
public interface ResetCallback {

    /**
     * Resets task actions in case {@link CancellationException} was triggered by
     * {@link TaskHandlerJob#cancelCurrentTask()} or {@link TaskHandlerJob#cancelCurrentTask(Predicate)}.
     */
    void reset();
}
