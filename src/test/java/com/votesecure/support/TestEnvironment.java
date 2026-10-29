package com.votesecure.support;

import com.votesecure.AppContext;
import com.votesecure.database.DatabaseManager;
import com.votesecure.model.Candidate;
import com.votesecure.model.Election;
import com.votesecure.model.ResultsSummary;
import com.votesecure.model.Voter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Comparator;

/**
 * A fully isolated database and application context for one test. Every test
 * receives its own temporary SQLite file and adjustable clock; no test can
 * touch the demonstration database.
 */
public final class TestEnvironment implements AutoCloseable {

    public static final LocalDateTime BASE_NOW = LocalDateTime.of(2026, 10, 28, 10, 0);
    public static final LocalDateTime BASE_START = BASE_NOW.minusHours(1);
    public static final LocalDateTime BASE_END = BASE_NOW.plusHours(1);
    public static final String DEFAULT_PIN = "2468";

    private final Path directory;
    private final Path databaseFile;
    private final MutableClock clock;
    private final DatabaseManager database;
    private final AppContext context;

    public TestEnvironment() throws Exception {
        this(BASE_NOW, BASE_START, BASE_END);
    }

    public TestEnvironment(LocalDateTime now, LocalDateTime start, LocalDateTime end) throws Exception {
        directory = Files.createTempDirectory("votesecure-test-");
        databaseFile = directory.resolve("votesecure-test.db");
        clock = new MutableClock(now);
        database = new DatabaseManager(databaseFile, clock);
        database.createSchema();
        database.ensureDefaultAdministrator();
        context = AppContext.create(database, clock);
        context.getElectionRepository().save(
                new Election("E-TEST", "Test Election", start, end));
    }

    public Path tempDirectory() {
        return directory;
    }

    public DatabaseManager database() {
        return database;
    }

    public AppContext context() {
        return context;
    }

    public MutableClock clock() {
        return clock;
    }

    public void setNow(LocalDateTime moment) {
        clock.setTo(moment);
    }

    public LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    public Election election() throws Exception {
        return context.getElectionService().getCurrentElection();
    }

    public Voter registerVoter(String voterId) throws Exception {
        return registerVoter(voterId, "Test Voter", DEFAULT_PIN, true);
    }

    public Voter registerVoter(String voterId, String name, String pin, boolean eligible) throws Exception {
        return context.getAdminService().registerVoter(voterId, name, pin, eligible);
    }

    public Candidate addCandidate(String candidateId) throws Exception {
        return context.getAdminService().addCandidate(
                candidateId, candidateName(candidateId), "Test Party",
                "Created by an automated test.");
    }

    private static String candidateName(String candidateId) {
        String[] names = {
                "Alpha", "Beta", "Gamma", "Delta", "Epsilon", "Zeta", "Eta", "Theta",
                "Iota", "Kappa", "Lambda", "Mu", "Nu", "Xi", "Omicron", "Pi", "Rho",
                "Sigma", "Tau", "Upsilon"
        };
        String digits = candidateId.replaceAll("\\D", "");
        try {
            int number = Integer.parseInt(digits);
            if (number >= 1 && number <= names.length) {
                return "Candidate " + names[number - 1];
            }
        } catch (NumberFormatException ignored) {
            // Fall through to a generic but valid label.
        }
        return "Candidate Test";
    }

    public Election saveElection(String electionId, LocalDateTime start, LocalDateTime end) throws Exception {
        Election election = new Election(electionId, "Election " + electionId, start, end);
        context.getElectionRepository().save(election);
        return election;
    }

    public Candidate saveCandidateDirectly(String candidateId, String electionId) throws Exception {
        Candidate candidate = new Candidate(candidateId, electionId,
                "Candidate " + candidateId, "Other Party", "", now());
        context.getCandidateRepository().save(candidate);
        return candidate;
    }

    public ResultsSummary results() throws Exception {
        return context.getResultsService().buildSummary(election());
    }

    public Voter reloadVoter(String voterId) throws Exception {
        return context.getVoterRepository().findById(voterId).orElseThrow();
    }

    public AppContext newContext() {
        return AppContext.create(new DatabaseManager(databaseFile, clock), clock);
    }

    @Override
    public void close() {
        try (var paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // Best-effort cleanup of temporary test files.
                }
            });
        } catch (IOException ignored) {
            // Best-effort cleanup.
        }
    }
}
