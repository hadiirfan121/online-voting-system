package com.votesecure.service;

import com.votesecure.exception.DataAccessException;
import com.votesecure.model.Candidate;
import com.votesecure.model.CandidateResult;
import com.votesecure.model.Election;
import com.votesecure.model.ElectionStatus;
import com.votesecure.model.ResultsSummary;
import com.votesecure.repository.CandidateRepository;
import com.votesecure.repository.VoteRepository;
import com.votesecure.repository.VoterRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Builds election results directly from saved ballots. Percentages guard
 * against division by zero, and ties are reported instead of guessing a winner.
 */
public final class ResultsService {

    private final CandidateRepository candidates;
    private final VoteRepository votes;
    private final VoterRepository voters;
    private final Clock clock;

    public ResultsService(CandidateRepository candidates,
                          VoteRepository votes,
                          VoterRepository voters,
                          Clock clock) {
        this.candidates = candidates;
        this.votes = votes;
        this.voters = voters;
        this.clock = clock;
    }

    public ResultsSummary buildSummary(Election election) throws DataAccessException {
        String electionId = election.getElectionId();
        int totalVotes = votes.countByElection(electionId);
        int eligibleVoters = voters.countEligible();
        List<Candidate> candidateList = candidates.findByElection(electionId);

        List<CandidateResult> results = new ArrayList<>();
        int highestVoteCount = 0;
        for (Candidate candidate : candidateList) {
            int candidateVotes = votes.countByCandidate(candidate.getCandidateId());
            double percentage = totalVotes == 0 ? 0.0 : (candidateVotes * 100.0) / totalVotes;
            results.add(new CandidateResult(candidate, candidateVotes, percentage));
            highestVoteCount = Math.max(highestVoteCount, candidateVotes);
        }

        List<String> winnerNames = new ArrayList<>();
        if (totalVotes > 0 && highestVoteCount > 0) {
            for (CandidateResult result : results) {
                if (result.getVotes() == highestVoteCount) {
                    winnerNames.add(result.getCandidate().getName());
                }
            }
        }
        boolean tie = winnerNames.size() > 1;

        results.sort(Comparator
                .comparingInt(CandidateResult::getVotes)
                .reversed()
                .thenComparing(result -> result.getCandidate().getCandidateId()));

        double turnout = eligibleVoters == 0 ? 0.0 : (totalVotes * 100.0) / eligibleVoters;
        LocalDateTime generatedAt = LocalDateTime.now(clock);
        ElectionStatus status = election.statusAt(generatedAt);

        return new ResultsSummary(
                election,
                status,
                generatedAt,
                eligibleVoters,
                totalVotes,
                turnout,
                results,
                winnerNames,
                tie);
    }
}
