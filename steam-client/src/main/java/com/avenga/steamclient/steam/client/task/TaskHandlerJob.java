package com.avenga.steamclient.steam.client.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

public class TaskHandlerJob {
    private static final Logger LOGGER = LoggerFactory.getLogger(TaskHandlerJob.class);
    private static final long DEFAULT_TASK_CHECK_PERIOD = 100L;

    private final ScheduledExecutorService scheduledExecutor;
    private final ExecutorService executor;
    private final BlockingQueue<CompletableTask> taskQueue;
    private AtomicReference<CompletableTask> currentTask = new AtomicReference<>();
    private String clientName;

    public TaskHandlerJob(String clientName, long taskCheckPeriod) {
        this.scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
        this.executor = Executors.newSingleThreadExecutor();
        this.taskQueue = new LinkedBlockingQueue<>();
        this.clientName = clientName;
        var checkPeriodInterval = taskCheckPeriod > 0 ? taskCheckPeriod : DEFAULT_TASK_CHECK_PERIOD;

        scheduledExecutor.scheduleAtFixedRate(() -> {
            try {
                var oldTask = currentTask.get();
                currentTask.compareAndSet(oldTask, taskQueue.poll());
                if (Objects.nonNull(currentTask.get())) {
                    LOGGER.debug("{}: Start executing task: {}", clientName, currentTask.get().getName());
                    currentTask.get().execute(executor);
                    LOGGER.debug("{}: Stop executing task: {}", clientName, currentTask.get().getName());
                }
            } catch (Exception e) {
                LOGGER.debug("{}: Exception during executing task {}: {}", clientName, currentTask.get().getName(), e.toString());
            }
        }, checkPeriodInterval, checkPeriodInterval, TimeUnit.MILLISECONDS);
    }

    public void registerTask(CompletableTask task) {
        LOGGER.debug("{}: Register task: {}", clientName, task.getName());
        taskQueue.offer(task);
    }

    public void removeTask(CompletableTask task) {
        taskQueue.remove(task);
    }

    public synchronized void cancelCurrentTask() {
        if (Objects.nonNull(currentTask.get()) && !currentTask.get().isComplete()) {
            currentTask.get().cancel();
        }
    }

    public synchronized void cancelCurrentTask(Predicate<CompletableTask> cancelCondition) {
        if (Objects.nonNull(currentTask.get()) && !currentTask.get().isComplete() && cancelCondition.test(currentTask.get())) {
            currentTask.get().cancel();
        }
    }

    public void stop() {
        LOGGER.debug("{}: Stop task handler job.", clientName);
        taskQueue.clear();
        if (!executor.isShutdown()) {
            executor.shutdownNow();
        }
        if (!scheduledExecutor.isShutdown()) {
            scheduledExecutor.shutdownNow();
        }
    }
}
