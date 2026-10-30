package com.portfoliomanager.space.services;

public class SpaceException extends IllegalArgumentException {
    public enum Reason { INVALID_NAME, INVALID_ID, NOT_FOUND }

    private final Reason reason;

    public SpaceException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}
