package com.votesecure.service;

import com.votesecure.AppContext;
import com.votesecure.exception.AuthenticationException;
import com.votesecure.exception.ConfigurationLockedException;
import com.votesecure.exception.DuplicateEntityException;
import com.votesecure.exception.ValidationException;
import com.votesecure.model.Election;
import com.votesecure.model.Voter;
import com.votesecure.support.TestEnvironment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminServiceTest {

    private TestEnvironment env;
    private AdminService admin;

    @BeforeEach
    void setUp() throws Exception {
        env = new TestEnvironment();
        admin = env.context().getAdminService();
    }

    @AfterEach
    void tearDown() {
        env.close();
    }

    @Test
    @DisplayName("A voter can be registered and found by ID and name")
    void voterRegistrationAndSearch() throws Exception {
        admin.registerVoter("V100", "Ananya Rao", "4826", true);
        admin.registerVoter("V200", "Vikram Singh", "4826", true);

        List<Voter> byName = admin.searchVoters("ananya");
        assertEquals(1, byName.size());
        assertEquals("V100", byName.get(0).getVoterId());

        List<Voter> byId = admin.searchVoters("V2");
        assertEquals(1, byId.size());
        assertEquals("V200", byId.get(0).getVoterId());
        assertEquals(2, admin.getRegisteredVoterCount());
    }

    @Test
    @DisplayName("Duplicate voter IDs are rejected by the database constraint")
    void duplicateVoterIdIsRejected() throws Exception {
        admin.registerVoter("V101", "First Voter", "4826", true);
        assertThrows(DuplicateEntityException.class,
                () -> admin.registerVoter("V101", "Second Voter", "4826", true));
        assertEquals(1, admin.getRegisteredVoterCount());
    }

    @Test
    @DisplayName("Duplicate candidate IDs are rejected by the database constraint")
    void duplicateCandidateIdIsRejected() throws Exception {
        admin.addCandidate("C101", "First Candidate", "Test Party", "");
        assertThrows(DuplicateEntityException.class,
                () -> admin.addCandidate("C101", "Second Candidate", "Test Party", ""));
        assertEquals(1, admin.getCandidateCount());
    }

    @Test
    @DisplayName("Invalid registration input is rejected before it reaches the database")
    void invalidVoterInputIsRejected() {
        assertThrows(ValidationException.class,
                () -> admin.registerVoter("bad id!", "Valid Name", "4826", true));
        assertThrows(ValidationException.class,
                () -> admin.registerVoter("V102", "X", "4826", true));
        assertThrows(ValidationException.class,
                () -> admin.registerVoter("V103", "Valid Name", "12", true));
    }

    @Test
    @DisplayName("An invalid election time range is rejected")
    void invalidElectionTimeRangeIsRejected() throws Exception {
        LocalDateTime start = TestEnvironment.BASE_NOW;
        assertThrows(ValidationException.class,
                () -> admin.updateElectionSettings("Bad Election", start, start.minusMinutes(1)));
        assertThrows(ValidationException.class,
                () -> admin.updateElectionSettings("Bad Election", start, start));

        Election current = env.election();
        assertEquals(TestEnvironment.BASE_START, current.getStartDateTime());
        assertEquals(TestEnvironment.BASE_END, current.getEndDateTime());
    }

    @Test
    @DisplayName("A valid schedule change is saved while the configuration is unlocked")
    void electionSettingsCanBeUpdatedBeforeVoting() throws Exception {
        LocalDateTime start = TestEnvironment.BASE_NOW.plusMinutes(5);
        LocalDateTime end = TestEnvironment.BASE_NOW.plusDays(2);
        Election updated = admin.updateElectionSettings("Updated Election Name", start, end);

        assertEquals("Updated Election Name", updated.getElectionName());
        assertEquals(start, env.election().getStartDateTime());
        assertEquals(end, env.election().getEndDateTime());
    }

    @Test
    @DisplayName("After the first vote the schedule and candidates are frozen")
    void configurationLocksAfterFirstVote() throws Exception {
        admin.registerVoter("V104", "Locked Voter", "4826", true);
        admin.addCandidate("C104", "Locked Candidate", "Test Party", "");
        env.context().getVotingManager().castVote("V104", "C104");

        assertThrows(ConfigurationLockedException.class,
                () -> admin.addCandidate("C105", "New Candidate", "Test Party", ""));
        assertThrows(ConfigurationLockedException.class,
                () -> admin.updateElectionSettings("Changed", TestEnvironment.BASE_NOW,
                        TestEnvironment.BASE_NOW.plusDays(1)));
        assertThrows(ConfigurationLockedException.class,
                () -> admin.deleteCandidate("C104"));
        assertThrows(ConfigurationLockedException.class,
                () -> admin.updateCandidate("C104", "Changed Candidate", "Other Party", ""));
    }

    @Test
    @DisplayName("A candidate can be edited and deleted before voting begins")
    void candidateLifecycleBeforeVoting() throws Exception {
        admin.addCandidate("C106", "Original Name", "Original Party", "Original description");
        admin.updateCandidate("C106", "Updated Name", "Updated Party", "Updated description");
        assertEquals("Updated Name",
                env.context().getCandidateRepository().findById("C106").orElseThrow().getName());

        admin.deleteCandidate("C106");
        assertTrue(env.context().getCandidateRepository().findById("C106").isEmpty());
    }

    @Test
    @DisplayName("An administrator can change the demo password")
    void administratorPasswordCanBeChanged() throws Exception {
        admin.changeAdministratorPassword("admin", "Admin@123", "NewPass@456");

        assertThrows(AuthenticationException.class, () -> env.context().getAuthenticationService()
                .authenticateAdministrator("admin", "Admin@123"));
        assertNotNull(env.context().getAuthenticationService()
                .authenticateAdministrator("admin", "NewPass@456"));
    }

    @Test
    @DisplayName("Changing the administrator password requires the current password")
    void administratorPasswordChangeRequiresCurrentPassword() {
        assertThrows(AuthenticationException.class,
                () -> admin.changeAdministratorPassword("admin", "wrong", "NewPass@456"));
        assertThrows(ValidationException.class,
                () -> admin.changeAdministratorPassword("admin", "Admin@123", "weak"));
        assertThrows(ValidationException.class,
                () -> admin.changeAdministratorPassword("admin", "Admin@123", "Admin@123"));
    }

    @Test
    @DisplayName("Registered data and votes persist after a fresh database connection")
    void dataPersistsAfterReconnecting() throws Exception {
        admin.registerVoter("V107", "Persistent Voter", "4826", true);
        admin.addCandidate("C107", "Persistent Candidate", "Test Party", "");
        env.context().getVotingManager().castVote("V107", "C107");

        AppContext reopened = env.newContext();
        Voter voter = reopened.getVoterRepository().findById("V107").orElseThrow();
        assertTrue(voter.hasVoted());
        assertEquals(1, reopened.getVoteRepository().countAll());
        assertEquals("Persistent Candidate",
                reopened.getCandidateRepository().findById("C107").orElseThrow().getName());

        // A second attempt through the reopened application also finds the saved ballot.
        assertThrows(com.votesecure.exception.DuplicateVoteException.class,
                () -> reopened.getVotingManager().castVote("V107", "C107"));
        assertEquals(1, reopened.getVoteRepository().countAll());
        assertTrue(reopened.getVoterRepository().findById("V107").orElseThrow().isEligible());
    }
}
