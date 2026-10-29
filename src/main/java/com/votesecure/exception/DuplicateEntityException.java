package com.votesecure.exception;

/** Raised when a voter ID or candidate ID already exists. */
public class DuplicateEntityException extends ValidationException {

    private static final long serialVersionUID = 1L;

    public DuplicateEntityException(String message) {
        super(message);
    }

    public DuplicateEntityException(String message, Throwable cause) {
        super(message, cause);
    }
}
