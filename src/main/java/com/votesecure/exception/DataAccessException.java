package com.votesecure.exception;

/**
 * Raised when the database cannot be opened or an SQL operation fails.
 * The cause is logged for diagnostics but is not shown to ordinary users.
 */
public class DataAccessException extends VoteSecureException {

    private static final long serialVersionUID = 1L;

    public DataAccessException(String message) {
        super(message);
    }

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
