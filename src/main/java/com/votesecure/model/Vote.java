package com.votesecure.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * A single recorded ballot. This object is used internally by the services and
 * is not displayed on voter-facing screens because it links a voter to a choice.
 */
public final class Vote {

    private final long voteId;
    private final String electionId;
    private final String voterId;
    private final String candidateId;
    private final LocalDateTime votedAt;

    public Vote(long voteId, String electionId, String voterId,
                String candidateId, LocalDateTime votedAt) {
        if (voteId <= 0) {
            throw new IllegalArgumentException("Vote ID must be positive.");
        }
        this.electionId = Objects.requireNonNull(electionId, "Election ID is required.");
        this.voterId = Objects.requireNonNull(voterId, "Voter ID is required.");
        this.candidateId = Objects.requireNonNull(candidateId, "Candidate ID is required.");
        this.votedAt = Objects.requireNonNull(votedAt, "Vote timestamp is required.");
        this.voteId = voteId;
    }

    public long getVoteId() {
        return voteId;
    }

    public String getElectionId() {
        return electionId;
    }

    public String getVoterId() {
        return voterId;
    }

    public String getCandidateId() {
        return candidateId;
    }

    public LocalDateTime getVotedAt() {
        return votedAt;
    }

    @Override
    public String toString() {
        return "Vote #" + voteId + " at " + votedAt;
    }
}
