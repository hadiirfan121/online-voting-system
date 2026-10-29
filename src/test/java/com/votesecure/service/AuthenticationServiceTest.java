package com.votesecure.service;

import com.votesecure.exception.AuthenticationException;
import com.votesecure.model.Administrator;
import com.votesecure.model.Voter;
import com.votesecure.support.TestEnvironment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthenticationServiceTest {

    private TestEnvironment env;

    @BeforeEach
    void setUp() throws Exception {
        env = new TestEnvironment();
    }

    @AfterEach
    void tearDown() {
        env.close();
    }

    @Test
    @DisplayName("A registered voter with the correct PIN signs in")
    void successfulVoterAuthentication() throws Exception {
        env.registerVoter("V100", "Test Voter", "4826", true);
        Voter voter = env.context().getVotingManager().authenticateVoter("V100", "4826");
        assertEquals("V100", voter.getVoterId());
        assertEquals("Test Voter", voter.getName());
        assertTrue(voter.isEligible());
        assertFalse(voter.hasVoted());
    }

    @Test
    @DisplayName("Voter sign-in is case-insensitive for the voter ID")
    void voterIdIsNormalised() throws Exception {
        env.registerVoter("V101", "Case Voter", "4826", true);
        Voter voter = env.context().getVotingManager().authenticateVoter(" v101 ", "4826");
        assertEquals("V101", voter.getVoterId());
    }

    @Test
    @DisplayName("A wrong voter PIN is rejected without revealing which field was wrong")
    void failedVoterAuthentication() throws Exception {
        env.registerVoter("V102", "Wrong Pin", "4826", true);
        AuthenticationException error = assertThrows(AuthenticationException.class,
                () -> env.context().getVotingManager().authenticateVoter("V102", "9999"));
        assertEquals("Invalid voter ID or PIN.", error.getMessage());
    }

    @Test
    @DisplayName("An unregistered voter ID is rejected")
    void unknownVoterIsRejected() {
        AuthenticationException error = assertThrows(AuthenticationException.class,
                () -> env.context().getVotingManager().authenticateVoter("V999", "4826"));
        assertEquals("Invalid voter ID or PIN.", error.getMessage());
    }

    @Test
    @DisplayName("Missing credentials are rejected")
    void blankCredentialsAreRejected() {
        assertThrows(AuthenticationException.class,
                () -> env.context().getVotingManager().authenticateVoter("", ""));
    }

    @Test
    @DisplayName("The demonstration administrator can sign in")
    void successfulAdministratorAuthentication() throws Exception {
        Administrator admin = env.context().getAuthenticationService()
                .authenticateAdministrator("admin", "Admin@123");
        assertEquals("admin", admin.getUsername());
    }

    @Test
    @DisplayName("A wrong administrator password is rejected")
    void failedAdministratorAuthentication() {
        assertThrows(AuthenticationException.class,
                () -> env.context().getAuthenticationService()
                        .authenticateAdministrator("admin", "wrong-password"));
    }

    @Test
    @DisplayName("The voter PIN is stored only as a salted hash")
    void pinIsNeverStoredInPlainText() throws Exception {
        env.registerVoter("V103", "Hash Voter", "4826", true);
        Voter stored = env.reloadVoter("V103");
        assertNotEquals("4826", stored.getPinHash());
        assertFalse(stored.getPinHash().contains("4826"));
        assertFalse(stored.getPinSalt().isBlank());
        assertNotEquals(stored.getPinSalt(), stored.getPinHash());
        assertFalse(stored.toString().contains(stored.getPinHash()));
        assertFalse(stored.toString().contains(stored.getPinSalt()));
    }
}
