package com.avenga.steamclient.steam.client.task;

import java.util.concurrent.ExecutorService;

public interface CompletableTask {

    /**
     * @return name of the task.
     */
    String getName();

    /**
     * @return sequence number of the task.
     */
    long getSequence();

    /**
     * @return is task was completed.
     */
    boolean isComplete();

    /**
     * Executes logic of the task on provided executor service pool.
     *
     * @param executor executor service on which task will be performed.
     */
    void execute(ExecutorService executor);

    /**
     * Cancels current execution of the task
     */
    void cancel();
}
