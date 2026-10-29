package com.votesecure.service;

import com.votesecure.AppContext;
import com.votesecure.exception.DataAccessException;
import com.votesecure.exception.DuplicateVoteException;
import com.votesecure.exception.ElectionWindowException;
import com.votesecure.exception.ValidationException;
import com.votesecure.model.Candidate;
import com.votesecure.model.Election;
import com.votesecure.model.Vote;
import com.votesecure.model.Voter;
import com.votesecure.support.TestEnvironment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VotingWorkflowTest {

    private TestEnvironment env;
    private VotingManager voting;

    @BeforeEach
    void setUp() throws Exception {
        env = new TestEnvironment();
        voting = env.context().getVotingManager();
    }

    @AfterEach
    void tearDown() {
        env.close();
    }

    @Test
    @DisplayName("A valid voter can cast one vote inside the election window")
    void successfulVoteSubmission() throws Exception {
        env.registerVoter("V001", "Ananya Rao", "4826", true);
        Candidate candidate = env.addCandidate("C001");
        Election election = env.election();

        Vote vote = voting.castVote("V001", "C001");

        assertEquals(election.getElectionId(), vote.getElectionId());
        assertEquals("V001", vote.getVoterId());
        assertEquals("C001", vote.getCandidateId());
        assertEquals(env.now(), vote.getVotedAt());
        assertTrue(vote.getVoteId() > 0);
        assertEquals(1, env.context().getVoteRepository()
                .countByElection(election.getElectionId()));
        assertTrue(env.reloadVoter("V001").hasVoted());
    }

    @Test
    @DisplayName("Voting at the exact start time is allowed")
    void votingAtStartTimeIsAllowed() throws Exception {
        env.registerVoter("V002");
        env.addCandidate("C002");
        env.setNow(TestEnvironment.BASE_START);
        voting.castVote("V002", "C002");
        assertEquals(1, env.results().getVotesCast());
    }

    @Test
    @DisplayName("A vote before the election starts is rejected")
    void rejectionBeforeElectionStarts() throws Exception {
        env.registerVoter("V003");
        env.addCandidate("C003");
        env.setNow(TestEnvironment.BASE_START.minusMinutes(1));

        ElectionWindowException error = assertThrows(ElectionWindowException.class,
                () -> voting.castVote("V003", "C003"));
        assertTrue(error.getMessage().startsWith("Voting has not started yet"));
        assertEquals(0, env.results().getVotesCast());
        assertFalse(env.reloadVoter("V003").hasVoted());
    }

    @Test
    @DisplayName("A vote at the exact end time is rejected")
    void rejectionAtElectionEndTime() throws Exception {
        env.registerVoter("V004");
        env.addCandidate("C004");
        env.setNow(TestEnvironment.BASE_END);

        ElectionWindowException error = assertThrows(ElectionWindowException.class,
                () -> voting.castVote("V004", "C004"));
        assertTrue(error.getMessage().startsWith("Voting has closed"));
        assertEquals(0, env.results().getVotesCast());
    }

    @Test
    @DisplayName("Voting one second before the end time is still allowed")
    void votingBeforeEndTimeIsAllowed() throws Exception {
        env.registerVoter("V005");
        env.addCandidate("C005");
        env.setNow(TestEnvironment.BASE_END.minusSeconds(1));
        voting.castVote("V005", "C005");
        assertEquals(1, env.results().getVotesCast());
    }

    @Test
    @DisplayName("A voter cannot cast a second vote")
    void rejectionOfRepeatVote() throws Exception {
        env.registerVoter("V006");
        env.addCandidate("C006");
        voting.castVote("V006", "C006");

        DuplicateVoteException error = assertThrows(DuplicateVoteException.class,
                () -> voting.castVote("V006", "C006"));
        assertTrue(error.getMessage().contains("already cast a vote"));
        assertEquals(1, env.results().getVotesCast());
    }

    @Test
    @DisplayName("An unregistered voter cannot vote")
    void rejectionOfUnregisteredVoter() throws Exception {
        env.addCandidate("C007");
        ValidationException error = assertThrows(ValidationException.class,
                () -> voting.castVote("V999", "C007"));
        assertTrue(error.getMessage().contains("not registered"));
        assertEquals(0, env.results().getVotesCast());
    }

    @Test
    @DisplayName("An ineligible voter is rejected even with valid credentials")
    void rejectionOfIneligibleVoter() throws Exception {
        env.registerVoter("V008", "Ineligible Voter", "4826", false);
        env.addCandidate("C008");

        ValidationException error = assertThrows(ValidationException.class,
                () -> voting.castVote("V008", "C008"));
        assertTrue(error.getMessage().contains("not eligible"));
        assertEquals(0, env.results().getVotesCast());
    }

    @Test
    @DisplayName("An unknown candidate is rejected")
    void rejectionOfInvalidCandidate() throws Exception {
        env.registerVoter("V009");
        ValidationException error = assertThrows(ValidationException.class,
                () -> voting.castVote("V009", "C999"));
        assertTrue(error.getMessage().contains("does not exist"));
        assertEquals(0, env.results().getVotesCast());
        assertFalse(env.reloadVoter("V009").hasVoted());
    }

    @Test
    @DisplayName("The database unique constraint blocks a duplicate ballot even if the flag is reset")
    void databaseRejectsDuplicateBallot() throws Exception {
        env.registerVoter("V010");
        env.addCandidate("C010");
        Election election = env.election();
        env.context().getVoteRepository()
                .castVote(election.getElectionId(), "V010", "C010", env.now());

        try (Connection connection = env.database().getConnection();
             Statement statement = connection.createStatement()) {
            // Simulate a stale client that still believes the voter has not voted.
            statement.executeUpdate("UPDATE voters SET has_voted = 0 WHERE voter_id = 'V010'");
        }

        assertThrows(DuplicateVoteException.class, () -> env.context().getVoteRepository()
                .castVote(election.getElectionId(), "V010", "C010", env.now()));
        assertEquals(1, env.context().getVoteRepository()
                .countByElection(election.getElectionId()));
    }

    @Test
    @DisplayName("Two simultaneous submissions for one voter produce exactly one ballot")
    void simultaneousSubmissionsProduceOneVote() throws Exception {
        env.registerVoter("V011");
        env.addCandidate("C011");
        AppContext secondWindow = env.newContext();
        String electionId = env.election().getElectionId();

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Boolean> first = pool.submit(() -> attemptVote(
                    env.context(), ready, start, "V011", "C011"));
            Future<Boolean> second = pool.submit(() -> attemptVote(
                    secondWindow, ready, start, "V011", "C011"));
            ready.await();
            start.countDown();
            int successes = (first.get() ? 1 : 0) + (second.get() ? 1 : 0);
            assertEquals(1, successes, "Exactly one concurrent submission should succeed");
        } finally {
            pool.shutdownNow();
        }
        assertEquals(1, env.context().getVoteRepository().countByElection(electionId));
        assertEquals(1, env.context().getVoteRepository().countAll());
    }

    private boolean attemptVote(AppContext app, CountDownLatch ready, CountDownLatch start,
                                String voterId, String candidateId) throws Exception {
        ready.countDown();
        start.await();
        try {
            app.getVotingManager().castVote(voterId, candidateId);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    @Test
    @DisplayName("A failed vote transaction is rolled back completely")
    void voteTransactionRollsBackOnFailure() throws Exception {
        env.registerVoter("V012");
        env.addCandidate("C012");
        Election election = env.election();

        try (Connection connection = env.database().getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TRIGGER fail_has_voted_update "
                    + "BEFORE UPDATE OF has_voted ON voters "
                    + "WHEN NEW.has_voted = 1 "
                    + "BEGIN SELECT RAISE(ABORT, 'forced test failure'); END");
        }

        assertThrows(DataAccessException.class, () -> voting.castVote("V012", "C012"));
        assertEquals(0, env.context().getVoteRepository().countByElection(election.getElectionId()));
        assertFalse(env.reloadVoter("V012").hasVoted());
    }

    @Test
    @DisplayName("A vote recorded at a later time still uses the injected clock")
    void voteTimestampUsesInjectedClock() throws Exception {
        env.registerVoter("V013");
        env.addCandidate("C013");
        env.clock().advance(Duration.ofMinutes(20));
        Vote vote = voting.castVote("V013", "C013");
        assertEquals(TestEnvironment.BASE_NOW.plusMinutes(20), vote.getVotedAt());
    }

    @Test
    @DisplayName("A voter marked as voted persists after the repository reconnects")
    void votedStatusIsPersisted() throws Exception {
        env.registerVoter("V014");
        env.addCandidate("C014");
        voting.castVote("V014", "C014");
        Voter reloaded = env.newContext().getVoterRepository().findById("V014").orElseThrow();
        assertTrue(reloaded.hasVoted());
    }
}
