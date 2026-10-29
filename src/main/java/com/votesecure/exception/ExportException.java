package com.votesecure.exception;

/** Raised when a result report cannot be written to the chosen location. */
public class ExportException extends VoteSecureException {

    private static final long serialVersionUID = 1L;

    public ExportException(String message) {
        super(message);
    }

    public ExportException(String message, Throwable cause) {
        super(message, cause);
    }
}
