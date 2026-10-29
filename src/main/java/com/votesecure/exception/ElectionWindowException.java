package com.votesecure.exception;

/** Raised when a vote is attempted before the start time or at/after the end time. */
public class ElectionWindowException extends VoteSecureException {

    private static final long serialVersionUID = 1L;

    public ElectionWindowException(String message) {
        super(message);
    }
}
