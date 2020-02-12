package com.avenga.steamclient.exception;

public class CallbackTimeoutException extends Exception {

    public CallbackTimeoutException(String message) {
        super(message);
    }

    public CallbackTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
