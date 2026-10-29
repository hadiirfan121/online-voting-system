package com.votesecure.service;

import com.votesecure.exception.AuthenticationException;
import com.votesecure.exception.DataAccessException;
import com.votesecure.exception.DuplicateVoteException;
import com.votesecure.exception.ElectionWindowException;
import com.votesecure.exception.ValidationException;
import com.votesecure.exception.VoteSecureException;
import com.votesecure.model.Candidate;
import com.votesecure.model.Election;
import com.votesecure.model.ElectionStatus;
import com.votesecure.model.ResultsSummary;
import com.votesecure.model.Vote;
import com.votesecure.model.Voter;
import com.votesecure.repository.CandidateRepository;
import com.votesecure.repository.VoteRepository;
import com.votesecure.repository.VoterRepository;
import com.votesecure.util.DateTimeUtils;

import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Coordinates the complete voting workflow: registration/eligibility checks,
 * the election window, duplicate protection, the transactional save, result
 * summaries and report export.
 *
 * <p>{@code castVote} is synchronized within this JVM and the SQLite table also
 * has a UNIQUE(election_id, voter_id) constraint, so a double-click or a second
 * window cannot create two ballots.</p>
 */
public final class VotingManager {

    private static final Logger LOGGER = Logger.getLogger(VotingManager.class.getName());

    private final VoterRepository voters;
    private final CandidateRepository candidates;
    private final VoteRepository votes;
    private final ElectionService electionService;
    private final AuthenticationService authenticationService;
    private final ResultsService resultsService;
    private final ReportExporter reportExporter;
    private final Clock clock;

    public VotingManager(VoterRepository voters,
                         CandidateRepository candidates,
                         VoteRepository votes,
                         ElectionService electionService,
                         AuthenticationService authenticationService,
                         ResultsService resultsService,
                         ReportExporter reportExporter,
                         Clock clock) {
        this.voters = voters;
        this.candidates = candidates;
        this.votes = votes;
        this.electionService = electionService;
        this.authenticationService = authenticationService;
        this.resultsService = resultsService;
        this.reportExporter = reportExporter;
        this.clock = clock;
    }

    public Voter authenticateVoter(String voterId, String pin) throws AuthenticationException {
        return authenticationService.authenticateVoter(voterId, pin);
    }

    public synchronized Vote castVote(String voterId, String candidateId) throws VoteSecureException {
        String normalizedVoterId = voterId == null ? "" : voterId.trim().toUpperCase(Locale.ROOT);
        String normalizedCandidateId = candidateId == null ? "" : candidateId.trim().toUpperCase(Locale.ROOT);
        if (normalizedVoterId.isEmpty()) {
            throw new ValidationException("A signed-in voter is required.");
        }
        if (normalizedCandidateId.isEmpty()) {
            throw new ValidationException("Please select a candidate before submitting.");
        }

        Election election = electionService.getCurrentElection();
        Voter voter = voters.findById(normalizedVoterId)
                .orElseThrow(() -> new ValidationException(
                        "This voter is not registered for the election."));
        if (!voter.isEligible()) {
            throw new ValidationException("This voter is not eligible to vote in this election.");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        ElectionStatus status = election.statusAt(now);
        if (status == ElectionStatus.NOT_STARTED) {
            throw new ElectionWindowException("Voting has not started yet. It opens on "
                    + DateTimeUtils.format(election.getStartDateTime()) + ".");
        }
        if (status == ElectionStatus.CLOSED) {
            throw new ElectionWindowException("Voting has closed. The election ended on "
                    + DateTimeUtils.format(election.getEndDateTime()) + ".");
        }

        if (voter.hasVoted() || votes.hasVoted(election.getElectionId(), normalizedVoterId)) {
            throw new DuplicateVoteException(
                    "This voter has already cast a vote. Each voter may vote only once.");
        }

        Optional<Candidate> selected = candidates.findById(normalizedCandidateId);
        if (selected.isEmpty()) {
            throw new ValidationException("The selected candidate does not exist.");
        }
        if (!selected.get().getElectionId().equals(election.getElectionId())) {
            throw new ValidationException("The selected candidate is not part of the current election.");
        }

        long voteId = votes.castVote(
                election.getElectionId(), normalizedVoterId, normalizedCandidateId, now);
        LOGGER.fine("A ballot was recorded for the current election.");
        return new Vote(voteId, election.getElectionId(), normalizedVoterId, normalizedCandidateId, now);
    }

    public ResultsSummary calculateResults() throws DataAccessException {
        return resultsService.buildSummary(electionService.getCurrentElection());
    }

    public void exportTextReport(ResultsSummary summary, Path destination, String notice)
            throws VoteSecureException {
        reportExporter.exportText(summary, destination, notice);
    }

    public void exportCsvReport(ResultsSummary summary, Path destination, String notice)
            throws VoteSecureException {
        reportExporter.exportCsv(summary, destination, notice);
    }
}
