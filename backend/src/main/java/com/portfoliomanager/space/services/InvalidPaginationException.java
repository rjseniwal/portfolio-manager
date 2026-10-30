package com.portfoliomanager.space.services;

public class InvalidPaginationException extends IllegalArgumentException {
    public InvalidPaginationException(String message) {
        super(message);
    }
}
