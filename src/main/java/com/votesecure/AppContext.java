package com.votesecure;

import com.votesecure.database.DatabaseManager;
import com.votesecure.repository.AdminRepository;
import com.votesecure.repository.CandidateRepository;
import com.votesecure.repository.ElectionRepository;
import com.votesecure.repository.JdbcAdminRepository;
import com.votesecure.repository.JdbcCandidateRepository;
import com.votesecure.repository.JdbcElectionRepository;
import com.votesecure.repository.JdbcVoteRepository;
import com.votesecure.repository.JdbcVoterRepository;
import com.votesecure.repository.VoteRepository;
import com.votesecure.repository.VoterRepository;
import com.votesecure.service.AdminService;
import com.votesecure.service.AuthenticationService;
import com.votesecure.service.ElectionService;
import com.votesecure.service.ReportExporter;
import com.votesecure.service.ResultsService;
import com.votesecure.service.VotingManager;
import com.votesecure.util.PasswordHasher;

import java.time.Clock;

/**
 * Wires repositories and services together once, then hands them to the
 * Swing screens. This is composition: services do not create their own
 * database or collaborators.
 */
public final class AppContext {

    private final DatabaseManager database;
    private final Clock clock;
    private final VoterRepository voterRepository;
    private final CandidateRepository candidateRepository;
    private final ElectionRepository electionRepository;
    private final VoteRepository voteRepository;
    private final AdminRepository adminRepository;
    private final PasswordHasher passwordHasher;
    private final AuthenticationService authenticationService;
    private final ElectionService electionService;
    private final ResultsService resultsService;
    private final ReportExporter reportExporter;
    private final VotingManager votingManager;
    private final AdminService adminService;

    private AppContext(DatabaseManager database, Clock clock) {
        this.database = database;
        this.clock = clock;
        this.voterRepository = new JdbcVoterRepository(database);
        this.candidateRepository = new JdbcCandidateRepository(database);
        this.electionRepository = new JdbcElectionRepository(database);
        this.voteRepository = new JdbcVoteRepository(database);
        this.adminRepository = new JdbcAdminRepository(database);
        this.passwordHasher = new PasswordHasher();
        this.authenticationService = new AuthenticationService(
                voterRepository, adminRepository, passwordHasher);
        this.electionService = new ElectionService(
                electionRepository, voteRepository, clock);
        this.resultsService = new ResultsService(
                candidateRepository, voteRepository, voterRepository, clock);
        this.reportExporter = new ReportExporter();
        this.votingManager = new VotingManager(
                voterRepository,
                candidateRepository,
                voteRepository,
                electionService,
                authenticationService,
                resultsService,
                reportExporter,
                clock);
        this.adminService = new AdminService(
                voterRepository,
                candidateRepository,
                adminRepository,
                voteRepository,
                electionService,
                passwordHasher,
                clock);
    }

    public static AppContext create(DatabaseManager database, Clock clock) {
        return new AppContext(database, clock);
    }

    public DatabaseManager getDatabase() {
        return database;
    }

    public Clock getClock() {
        return clock;
    }

    public VoterRepository getVoterRepository() {
        return voterRepository;
    }

    public CandidateRepository getCandidateRepository() {
        return candidateRepository;
    }

    public ElectionRepository getElectionRepository() {
        return electionRepository;
    }

    public VoteRepository getVoteRepository() {
        return voteRepository;
    }

    public AdminRepository getAdminRepository() {
        return adminRepository;
    }

    public PasswordHasher getPasswordHasher() {
        return passwordHasher;
    }

    public AuthenticationService getAuthenticationService() {
        return authenticationService;
    }

    public ElectionService getElectionService() {
        return electionService;
    }

    public ResultsService getResultsService() {
        return resultsService;
    }

    public ReportExporter getReportExporter() {
        return reportExporter;
    }

    public VotingManager getVotingManager() {
        return votingManager;
    }

    public AdminService getAdminService() {
        return adminService;
    }
}
