package com.votesecure.model;

import java.time.LocalDateTime;
import java.util.Objects;

/** An administrator account. Only the salted password hash is retained. */
public final class Administrator {

    private final String username;
    private final String passwordHash;
    private final String passwordSalt;
    private final boolean mustChangePassword;
    private final LocalDateTime createdAt;

    public Administrator(String username, String passwordHash, String passwordSalt,
                         boolean mustChangePassword, LocalDateTime createdAt) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Administrator username is required.");
        }
        if (passwordHash == null || passwordHash.isBlank() || passwordSalt == null || passwordSalt.isBlank()) {
            throw new IllegalArgumentException("Administrator password hash and salt are required.");
        }
        this.username = username.trim();
        this.passwordHash = passwordHash;
        this.passwordSalt = passwordSalt;
        this.mustChangePassword = mustChangePassword;
        this.createdAt = Objects.requireNonNull(createdAt, "Created date is required.");
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getPasswordSalt() {
        return passwordSalt;
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return "Administrator{" + username + "}";
    }
}
