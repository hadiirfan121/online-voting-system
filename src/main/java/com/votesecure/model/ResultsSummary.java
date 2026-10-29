package com.votesecure.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Immutable snapshot of the election result. All counts are read from the
 * database; no value in this object is adjusted independently of saved votes.
 */
public final class ResultsSummary {

    private final Election election;
    private final ElectionStatus status;
    private final LocalDateTime generatedAt;
    private final int eligibleVoters;
    private final int votesCast;
    private final double turnoutPercentage;
    private final List<CandidateResult> candidateResults;
    private final List<String> winnerNames;
    private final boolean tie;

    public ResultsSummary(Election election,
                          ElectionStatus status,
                          LocalDateTime generatedAt,
                          int eligibleVoters,
                          int votesCast,
                          double turnoutPercentage,
                          List<CandidateResult> candidateResults,
                          List<String> winnerNames,
                          boolean tie) {
        this.election = Objects.requireNonNull(election, "Election is required.");
        this.status = Objects.requireNonNull(status, "Status is required.");
        this.generatedAt = Objects.requireNonNull(generatedAt, "Generated date is required.");
        this.eligibleVoters = Math.max(0, eligibleVoters);
        this.votesCast = Math.max(0, votesCast);
        this.turnoutPercentage = turnoutPercentage;
        this.candidateResults = List.copyOf(candidateResults);
        this.winnerNames = List.copyOf(winnerNames);
        this.tie = tie;
    }

    public Election getElection() {
        return election;
    }

    public ElectionStatus getStatus() {
        return status;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public int getEligibleVoters() {
        return eligibleVoters;
    }

    public int getVotesCast() {
        return votesCast;
    }

    public double getTurnoutPercentage() {
        return turnoutPercentage;
    }

    public List<CandidateResult> getCandidateResults() {
        return candidateResults;
    }

    public List<String> getWinnerNames() {
        return winnerNames;
    }

    public boolean isTie() {
        return tie;
    }

    public boolean hasVotes() {
        return votesCast > 0;
    }

    /** A plain-language outcome suitable for tables, labels and reports. */
    public String getOutcomeText() {
        if (!hasVotes()) {
            return "No votes recorded";
        }
        if (tie) {
            return "Tie between " + String.join(", ", winnerNames);
        }
        if (winnerNames.isEmpty()) {
            return "No clear winner";
        }
        return "Winner: " + winnerNames.get(0);
    }

    public String getOutcomeLabel() {
        if (!hasVotes()) {
            return "No result yet";
        }
        return tie ? "Tie" : "Winner";
    }
}
