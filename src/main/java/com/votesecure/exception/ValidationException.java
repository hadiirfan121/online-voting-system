package com.votesecure.exception;

/** Raised when user input or domain data fails a business validation rule. */
public class ValidationException extends VoteSecureException {

    private static final long serialVersionUID = 1L;

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
