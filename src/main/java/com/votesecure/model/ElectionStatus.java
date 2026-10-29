package com.votesecure.model;

/** The time-based status of an election. */
public enum ElectionStatus {

    NOT_STARTED("Not started"),
    OPEN("Voting open"),
    CLOSED("Closed");

    private final String displayName;

    ElectionStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isOpen() {
        return this == OPEN;
    }

    public boolean isClosed() {
        return this == CLOSED;
    }
}
