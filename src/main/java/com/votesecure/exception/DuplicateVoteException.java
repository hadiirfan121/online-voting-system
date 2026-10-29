package com.votesecure.exception;

/** Raised when a voter already has a recorded vote in the same election. */
public class DuplicateVoteException extends VoteSecureException {

    private static final long serialVersionUID = 1L;

    public DuplicateVoteException(String message) {
        super(message);
    }

    public DuplicateVoteException(String message, Throwable cause) {
        super(message, cause);
    }
}
