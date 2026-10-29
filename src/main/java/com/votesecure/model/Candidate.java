package com.votesecure.model;

import java.time.LocalDateTime;
import java.util.Objects;

/** A candidate standing in one election. */
public final class Candidate {

    private final String candidateId;
    private final String electionId;
    private final String name;
    private final String politicalParty;
    private final String description;
    private final LocalDateTime createdAt;

    public Candidate(String candidateId,
                     String electionId,
                     String name,
                     String politicalParty,
                     String description,
                     LocalDateTime createdAt) {
        this.candidateId = requireText(candidateId, "Candidate ID");
        this.electionId = requireText(electionId, "Election ID");
        this.name = requireText(name, "Candidate name");
        this.politicalParty = requireText(politicalParty, "Political party");
        this.description = description == null ? "" : description.trim();
        this.createdAt = Objects.requireNonNull(createdAt, "Created date is required.");
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return value.trim();
    }

    public String getCandidateId() {
        return candidateId;
    }

    public String getElectionId() {
        return electionId;
    }

    public String getName() {
        return name;
    }

    public String getPoliticalParty() {
        return politicalParty;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getDisplayLabel() {
        return name + " (" + politicalParty + ")";
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Candidate candidate)) {
            return false;
        }
        return candidateId.equals(candidate.candidateId);
    }

    @Override
    public int hashCode() {
        return candidateId.hashCode();
    }

    @Override
    public String toString() {
        return candidateId + " - " + name + " (" + politicalParty + ")";
    }
}
