package com.votesecure.exception;

/** Raised for invalid voter or administrator credentials. */
public class AuthenticationException extends VoteSecureException {

    private static final long serialVersionUID = 1L;

    public AuthenticationException(String message) {
        super(message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
