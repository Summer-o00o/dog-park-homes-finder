package com.dogparkhomes.api.exception;

/**
 * Thrown when the user's search input is invalid (e.g. no location could be extracted).
 * Mapped to HTTP 400 with a user-facing message.
 */
public class InvalidSearchQueryException extends RuntimeException {

    public InvalidSearchQueryException(String message) {
        super(message);
    }
}
