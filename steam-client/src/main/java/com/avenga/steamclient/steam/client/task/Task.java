package com.avenga.steamclient.steam.client.task;

import lombok.EqualsAndHashCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@EqualsAndHashCode
public class Task implements CompletableTask {
    private static final Logger LOGGER = LoggerFactory.getLogger(Task.class);
    private static final AtomicLong INTIAL_SEQUENCE = new AtomicLong();

    private final Runnable task;
    private final String name;
    private final long sequence;
    @EqualsAndHashCode.Exclude
    private ResetCallback resetTaskActions;
    @EqualsAndHashCode.Exclude
    private AtomicBoolean isComplete = new AtomicBoolean();
    @EqualsAndHashCode.Exclude
    private AtomicBoolean isCanceled = new AtomicBoolean();
    @EqualsAndHashCode.Exclude
    private Future<?> currentFuture;

    public Task(Runnable task, String name) {
        this.task = task;
        this.name = name;
        this.sequence = INTIAL_SEQUENCE.get();
    }

    public Task(Runnable task, ResetCallback resetTaskActions, String name) {
        this.task = task;
        this.name = name;
        this.resetTaskActions = resetTaskActions;
        this.sequence = INTIAL_SEQUENCE.get();
    }

    @Override
    public void execute(ExecutorService executor) {
        this.isComplete.set(false);
        currentFuture = executor.submit(task);
        try {
            currentFuture.get();
        } catch (InterruptedException | CancellationException | ExecutionException e) {
            if (!isCanceled.get()) {
                LOGGER.debug("Unexpected " + name +" task execution was interupted.", e);
            }
            if (Objects.nonNull(resetTaskActions)) {
                resetTaskActions.reset();
            }
        }
        this.isComplete.compareAndSet(false,true);
    }

    @Override
    public void cancel() {
        this.isCanceled.compareAndSet(false,true);
        currentFuture.cancel(true);
    }

    @Override
    public long getSequence() {
        return sequence;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isComplete() {
        return isComplete.get();
    }
}
