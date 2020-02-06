package com.avenga.steamclient.exception;

public class CallbackQueueException extends RuntimeException {

    public CallbackQueueException(String message) {
        super(message);
    }

    public CallbackQueueException(String message, Throwable cause) {
        super(message, cause);
    }
}
