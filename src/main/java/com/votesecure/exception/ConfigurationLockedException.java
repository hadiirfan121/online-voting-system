package com.votesecure.exception;

/**
 * Raised when an administrator tries to change the election schedule or
 * candidate list after the configuration has been frozen to protect the result.
 */
public class ConfigurationLockedException extends VoteSecureException {

    private static final long serialVersionUID = 1L;

    public ConfigurationLockedException(String message) {
        super(message);
    }
}
