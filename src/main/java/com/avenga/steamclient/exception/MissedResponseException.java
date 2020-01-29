package com.avenga.steamclient.exception;

public class MissedResponseException extends RuntimeException {

    public MissedResponseException(String message) {
        super(message);
    }

    public MissedResponseException(String message, Throwable cause) {
        super(message, cause);
    }
}
