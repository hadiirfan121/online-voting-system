package com.votesecure.service;

import com.votesecure.model.CandidateResult;
import com.votesecure.model.ElectionStatus;
import com.votesecure.model.ResultsSummary;
import com.votesecure.support.TestEnvironment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResultsServiceTest {

    private TestEnvironment env;

    @BeforeEach
    void setUp() throws Exception {
        env = new TestEnvironment();
        env.addCandidate("C001");
        env.addCandidate("C002");
        env.addCandidate("C003");
    }

    @AfterEach
    void tearDown() {
        env.close();
    }

    @Test
    @DisplayName("Zero votes produce safe zero counts and percentages")
    void zeroVotesAreHandledSafely() throws Exception {
        ResultsSummary summary = env.results();
        assertEquals(0, summary.getVotesCast());
        assertEquals(0, summary.getEligibleVoters());
        assertEquals(0.0, summary.getTurnoutPercentage(), 0.0001);
        assertFalse(summary.hasVotes());
        assertFalse(summary.isTie());
        assertTrue(summary.getWinnerNames().isEmpty());
        assertEquals("No votes recorded", summary.getOutcomeText());
        assertEquals("No result yet", summary.getOutcomeLabel());
        for (CandidateResult result : summary.getCandidateResults()) {
            assertEquals(0, result.getVotes());
            assertEquals(0.0, result.getPercentageOfValidVotes(), 0.0001);
        }
    }

    @Test
    @DisplayName("Candidate vote counts and percentages are calculated from saved ballots")
    void candidateCountsAndPercentages() throws Exception {
        env.registerVoter("V001");
        env.registerVoter("V002");
        env.registerVoter("V003");
        env.context().getVotingManager().castVote("V001", "C001");
        env.context().getVotingManager().castVote("V002", "C001");
        env.context().getVotingManager().castVote("V003", "C002");

        ResultsSummary summary = env.results();
        assertEquals(3, summary.getVotesCast());
        assertEquals(3, summary.getEligibleVoters());
        assertEquals(100.0, summary.getTurnoutPercentage(), 0.0001);
        assertEquals("C001", summary.getCandidateResults().get(0).getCandidate().getCandidateId());
        assertEquals(2, summary.getCandidateResults().get(0).getVotes());
        assertEquals(66.6667, summary.getCandidateResults().get(0).getPercentageOfValidVotes(), 0.001);
        assertEquals(1, summary.getCandidateResults().get(1).getVotes());
        assertEquals(33.3333, summary.getCandidateResults().get(1).getPercentageOfValidVotes(), 0.001);
        assertEquals(0, summary.getCandidateResults().get(2).getVotes());
    }

    @Test
    @DisplayName("The candidate with the highest count is the winner")
    void winnerIsDeterminedCorrectly() throws Exception {
        env.registerVoter("V011");
        env.registerVoter("V012");
        env.registerVoter("V013");
        env.registerVoter("V014");
        env.context().getVotingManager().castVote("V011", "C002");
        env.context().getVotingManager().castVote("V012", "C002");
        env.context().getVotingManager().castVote("V013", "C002");
        env.context().getVotingManager().castVote("V014", "C003");

        ResultsSummary summary = env.results();
        assertFalse(summary.isTie());
        assertEquals(1, summary.getWinnerNames().size());
        assertEquals("Candidate Beta", summary.getWinnerNames().get(0));
        assertEquals("Winner: Candidate Beta", summary.getOutcomeText());
        assertEquals("Winner", summary.getOutcomeLabel());
    }

    @Test
    @DisplayName("Equal highest counts are reported as a tie, not an invented winner")
    void tieIsDeterminedCorrectly() throws Exception {
        env.registerVoter("V021");
        env.registerVoter("V022");
        env.context().getVotingManager().castVote("V021", "C001");
        env.context().getVotingManager().castVote("V022", "C003");

        ResultsSummary summary = env.results();
        assertTrue(summary.isTie());
        assertEquals(2, summary.getWinnerNames().size());
        assertEquals("Tie", summary.getOutcomeLabel());
        assertTrue(summary.getOutcomeText().startsWith("Tie between "));
        assertEquals(50.0, summary.getCandidateResults().get(0).getPercentageOfValidVotes(), 0.0001);
        assertEquals(50.0, summary.getCandidateResults().get(1).getPercentageOfValidVotes(), 0.0001);
    }

    @Test
    @DisplayName("Turnout uses the eligible-voter count, not every registered record")
    void turnoutPercentageIsCorrect() throws Exception {
        env.registerVoter("V031");
        env.registerVoter("V032");
        env.registerVoter("V033");
        env.registerVoter("V034", "Ineligible Voter", "2468", false);
        env.context().getVotingManager().castVote("V031", "C001");
        env.context().getVotingManager().castVote("V032", "C002");

        ResultsSummary summary = env.results();
        assertEquals(3, summary.getEligibleVoters());
        assertEquals(2, summary.getVotesCast());
        assertEquals(66.6667, summary.getTurnoutPercentage(), 0.001);
    }

    @Test
    @DisplayName("Results are sorted from highest to lowest vote count")
    void resultsAreSortedByVotes() throws Exception {
        env.registerVoter("V041");
        env.registerVoter("V042");
        env.registerVoter("V043");
        env.context().getVotingManager().castVote("V041", "C003");
        env.context().getVotingManager().castVote("V042", "C003");
        env.context().getVotingManager().castVote("V043", "C001");

        ResultsSummary summary = env.results();
        assertEquals("C003", summary.getCandidateResults().get(0).getCandidate().getCandidateId());
        assertEquals("C001", summary.getCandidateResults().get(1).getCandidate().getCandidateId());
        assertEquals("C002", summary.getCandidateResults().get(2).getCandidate().getCandidateId());
    }

    @Test
    @DisplayName("The result snapshot reports the election status at generation time")
    void resultStatusUsesInjectedClock() throws Exception {
        env.setNow(TestEnvironment.BASE_END.plusMinutes(5));
        ResultsSummary summary = env.results();
        assertEquals(ElectionStatus.CLOSED, summary.getStatus());
        assertEquals(TestEnvironment.BASE_END.plusMinutes(5), summary.getGeneratedAt());
    }
}
