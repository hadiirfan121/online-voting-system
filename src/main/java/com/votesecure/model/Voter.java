package com.votesecure.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * A registered voter.
 *
 * <p>The raw PIN is never stored in this object. Only a salted PBKDF2 hash and
 * its salt are kept, and neither value is shown in the user interface.</p>
 */
public final class Voter {

    private final String voterId;
    private final String name;
    private final boolean eligible;
    private final boolean hasVoted;
    private final String pinHash;
    private final String pinSalt;
    private final LocalDateTime registeredAt;

    public Voter(String voterId,
                 String name,
                 boolean eligible,
                 boolean hasVoted,
                 String pinHash,
                 String pinSalt,
                 LocalDateTime registeredAt) {
        this.voterId = requireText(voterId, "Voter ID");
        this.name = requireText(name, "Voter name");
        this.pinHash = requireText(pinHash, "PIN hash");
        this.pinSalt = requireText(pinSalt, "PIN salt");
        this.registeredAt = Objects.requireNonNull(registeredAt, "Registered date is required.");
        this.eligible = eligible;
        this.hasVoted = hasVoted;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return value.trim();
    }

    public String getVoterId() {
        return voterId;
    }

    public String getName() {
        return name;
    }

    public boolean isEligible() {
        return eligible;
    }

    public boolean hasVoted() {
        return hasVoted;
    }

    public String getPinHash() {
        return pinHash;
    }

    public String getPinSalt() {
        return pinSalt;
    }

    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }

    public Voter withHasVoted(boolean voted) {
        return new Voter(voterId, name, eligible, voted, pinHash, pinSalt, registeredAt);
    }

    public Voter withEligibility(boolean nowEligible) {
        return new Voter(voterId, name, nowEligible, hasVoted, pinHash, pinSalt, registeredAt);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Voter voter)) {
            return false;
        }
        return voterId.equals(voter.voterId);
    }

    @Override
    public int hashCode() {
        return voterId.hashCode();
    }

    @Override
    public String toString() {
        return "Voter{" + voterId + ", " + name + ", hasVoted=" + hasVoted + "}";
    }
}
