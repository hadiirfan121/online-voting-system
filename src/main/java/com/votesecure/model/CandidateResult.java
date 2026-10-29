package com.votesecure.model;

import java.util.Objects;

/** The vote tally for one candidate in a results summary. */
public final class CandidateResult {

    private final Candidate candidate;
    private final int votes;
    private final double percentageOfValidVotes;

    public CandidateResult(Candidate candidate, int votes, double percentageOfValidVotes) {
        this.candidate = Objects.requireNonNull(candidate, "Candidate is required.");
        if (votes < 0) {
            throw new IllegalArgumentException("Vote count cannot be negative.");
        }
        this.votes = votes;
        this.percentageOfValidVotes = percentageOfValidVotes;
    }

    public Candidate getCandidate() {
        return candidate;
    }

    public int getVotes() {
        return votes;
    }

    public double getPercentageOfValidVotes() {
        return percentageOfValidVotes;
    }

    @Override
    public String toString() {
        return candidate.getName() + ": " + votes;
    }
}
