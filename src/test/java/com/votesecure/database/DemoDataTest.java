package com.votesecure.database;

import com.votesecure.AppContext;
import com.votesecure.model.Election;
import com.votesecure.model.ElectionStatus;
import com.votesecure.model.Voter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DemoDataTest {

    private Path directory;
    private DatabaseManager database;
    private AppContext context;
    private Clock clock;

    @BeforeEach
    void setUp() throws Exception {
        directory = Files.createTempDirectory("votesecure-demo-test-");
        ZoneId zone = ZoneId.systemDefault();
        LocalDateTime moment = LocalDateTime.of(2026, 10, 28, 10, 0);
        clock = Clock.fixed(moment.atZone(zone).toInstant(), zone);
        database = new DatabaseManager(directory.resolve("demo.db"), clock);
        database.initialize();
        context = AppContext.create(database, clock);
    }

    @AfterEach
    void tearDown() throws Exception {
        try (var paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (Exception ignored) {
                    // Best-effort cleanup.
                }
            });
        }
    }

    @Test
    @DisplayName("First launch creates the database, administrator, election and demo records")
    void firstLaunchCreatesCompleteDemoData() throws Exception {
        assertTrue(Files.exists(database.getDatabaseFile()));
        assertEquals(10, context.getVoterRepository().countAll());
        assertEquals(1, context.getElectionRepository().findAll().size());
        assertEquals(3, context.getCandidateRepository().countByElection("E2026"));
        assertEquals(0, context.getVoteRepository().countAll());
        assertEquals(10, context.getVoterRepository().countEligible());
    }

    @Test
    @DisplayName("The demonstration election is open immediately and has a valid window")
    void demoElectionIsOpen() throws Exception {
        Election election = context.getElectionService().getCurrentElection();
        assertEquals("Student Council Election 2026", election.getElectionName());
        assertTrue(election.getStartDateTime().isBefore(election.getEndDateTime()));
        assertEquals(ElectionStatus.OPEN, context.getElectionService().getStatus(election));
        assertTrue(context.getElectionService().isVotingOpen(election));
    }

    @Test
    @DisplayName("The demonstration administrator and voter credentials work")
    void demoCredentialsWork() throws Exception {
        context.getAuthenticationService().authenticateAdministrator("admin", "Admin@123");
        Voter voter = context.getAuthenticationService().authenticateVoter("V001", "1234");
        assertEquals("Ananya Rao", voter.getName());
        assertEquals(10, context.getVoterRepository().findAll().size());
    }

    @Test
    @DisplayName("The demonstration administrator password is stored as a salted hash")
    void demoPasswordIsHashed() throws Exception {
        String sql = "SELECT password_hash, password_salt FROM admins WHERE username = 'admin'";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rows = statement.executeQuery()) {
            assertTrue(rows.next());
            String hash = rows.getString("password_hash");
            String salt = rows.getString("password_salt");
            assertNotEquals("Admin@123", hash);
            assertFalse(hash.contains("Admin@123"));
            assertFalse(salt.isBlank());
        }
    }
}
