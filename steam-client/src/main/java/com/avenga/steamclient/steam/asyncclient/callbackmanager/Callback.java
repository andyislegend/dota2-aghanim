package com.avenga.steamclient.steam.asyncclient.callbackmanager;

import com.avenga.steamclient.model.JobID;

import java.io.Closeable;
import java.util.function.Consumer;

public class Callback<TCall extends CallbackMessage> extends BaseCallback implements Closeable {

    CallbackManager manager;

    private JobID jobID;

    private Consumer<TCall> onRun;

    private Class<? extends TCall> callbackType;

    public Callback(Class<? extends TCall> callbackType, Consumer<TCall> function) {
        this(callbackType, function, null);
    }

    public Callback(Class<? extends TCall> callbackType, Consumer<TCall> function, CallbackManager manager) {
        this(callbackType, function, manager, JobID.INVALID);
    }

    public Callback(Class<? extends TCall> callbackType, Consumer<TCall> function, CallbackManager manager, JobID jobID) {
        this.jobID = jobID;
        this.onRun = function;
        this.callbackType = callbackType;

        attachTo(manager);
    }

    void attachTo(CallbackManager manager) {
        if (manager == null) {
            return;
        }

        this.manager = manager;
        manager.register(this);
    }

    @Override
    Class getCallbackType() {
        return callbackType;
    }

    @Override
    void run(Object genericCallback) {
        if (callbackType.isAssignableFrom(genericCallback.getClass())) {
            TCall callback = (TCall) genericCallback;

            if ((callback.getJobID().equals(jobID) || jobID.equals(JobID.INVALID)) && onRun != null) {
                onRun.accept(callback);
            }
        }
    }

    @Override
    public void close() {
        if (manager != null) {
            manager.unregister(this);
        }
    }
}
