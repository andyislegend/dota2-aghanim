package com.avenga.steamclient.exception;

public class CallbackCompletionException extends RuntimeException {

    public CallbackCompletionException(String message) {
        super(message);
    }

    public CallbackCompletionException(String message, Throwable cause) {
        super(message, cause);
    }
}
