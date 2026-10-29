package com.votesecure.exception;

/**
 * Base class for every checked application exception.
 * Having one common parent lets the user interface display a friendly message
 * without leaking database or stack-trace details.
 */
public class VoteSecureException extends Exception {

    private static final long serialVersionUID = 1L;

    public VoteSecureException(String message) {
        super(message);
    }

    public VoteSecureException(String message, Throwable cause) {
        super(message, cause);
    }
}
