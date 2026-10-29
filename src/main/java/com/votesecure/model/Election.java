package com.votesecure.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * One election and its voting window.
 *
 * <p>All dates use the computer's local time zone. Voting is permitted when
 * {@code start <= now < end}; the end instant itself is already closed.</p>
 */
public final class Election {

    private final String electionId;
    private final String electionName;
    private final LocalDateTime startDateTime;
    private final LocalDateTime endDateTime;

    public Election(String electionId, String electionName,
                    LocalDateTime startDateTime, LocalDateTime endDateTime) {
        this.electionId = requireText(electionId, "Election ID");
        this.electionName = requireText(electionName, "Election name");
        this.startDateTime = Objects.requireNonNull(startDateTime, "Start date/time is required.");
        this.endDateTime = Objects.requireNonNull(endDateTime, "End date/time is required.");
        if (!endDateTime.isAfter(startDateTime)) {
            throw new IllegalArgumentException("The election end time must be after the start time.");
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return value.trim();
    }

    public String getElectionId() {
        return electionId;
    }

    public String getElectionName() {
        return electionName;
    }

    public LocalDateTime getStartDateTime() {
        return startDateTime;
    }

    public LocalDateTime getEndDateTime() {
        return endDateTime;
    }

    public ElectionStatus statusAt(LocalDateTime moment) {
        Objects.requireNonNull(moment, "Moment is required.");
        if (moment.isBefore(startDateTime)) {
            return ElectionStatus.NOT_STARTED;
        }
        if (moment.isBefore(endDateTime)) {
            return ElectionStatus.OPEN;
        }
        return ElectionStatus.CLOSED;
    }

    public Election withWindow(String newName, LocalDateTime newStart, LocalDateTime newEnd) {
        return new Election(electionId, newName, newStart, newEnd);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Election election)) {
            return false;
        }
        return electionId.equals(election.electionId);
    }

    @Override
    public int hashCode() {
        return electionId.hashCode();
    }

    @Override
    public String toString() {
        return electionName + " [" + startDateTime + " -> " + endDateTime + "]";
    }
}
