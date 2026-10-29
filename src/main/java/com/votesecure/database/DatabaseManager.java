package com.votesecure.database;

import com.votesecure.exception.DataAccessException;
import com.votesecure.util.DateTimeUtils;
import com.votesecure.util.PasswordHasher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Owns the SQLite database file, creates the schema and seeds the clearly
 * labelled demonstration data on first launch.
 */
public final class DatabaseManager {

    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());

    public static final String DEMO_ADMIN_USERNAME = "admin";
    public static final String DEMO_ADMIN_PASSWORD = "Admin@123";
    public static final String DEMO_VOTER_PIN = "1234";

    private static final String[] SCHEMA = {
            """
            CREATE TABLE IF NOT EXISTS elections (
                election_id     TEXT PRIMARY KEY,
                election_name   TEXT NOT NULL,
                start_datetime  TEXT NOT NULL,
                end_datetime    TEXT NOT NULL,
                created_at      TEXT NOT NULL
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS voters (
                voter_id    TEXT PRIMARY KEY,
                name        TEXT NOT NULL,
                pin_hash    TEXT NOT NULL,
                pin_salt    TEXT NOT NULL,
                eligible    INTEGER NOT NULL DEFAULT 1 CHECK (eligible IN (0, 1)),
                has_voted   INTEGER NOT NULL DEFAULT 0 CHECK (has_voted IN (0, 1)),
                registered_at TEXT NOT NULL
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS candidates (
                candidate_id TEXT PRIMARY KEY,
                election_id  TEXT NOT NULL,
                name         TEXT NOT NULL,
                party        TEXT NOT NULL,
                description  TEXT NOT NULL DEFAULT '',
                created_at   TEXT NOT NULL,
                FOREIGN KEY (election_id) REFERENCES elections(election_id) ON DELETE CASCADE
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS votes (
                vote_id      INTEGER PRIMARY KEY AUTOINCREMENT,
                election_id  TEXT NOT NULL,
                voter_id     TEXT NOT NULL,
                candidate_id TEXT NOT NULL,
                voted_at     TEXT NOT NULL,
                FOREIGN KEY (election_id) REFERENCES elections(election_id) ON DELETE RESTRICT,
                FOREIGN KEY (voter_id) REFERENCES voters(voter_id) ON DELETE RESTRICT,
                FOREIGN KEY (candidate_id) REFERENCES candidates(candidate_id) ON DELETE RESTRICT,
                CONSTRAINT one_vote_per_voter_per_election UNIQUE (election_id, voter_id)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS admins (
                username             TEXT PRIMARY KEY,
                password_hash        TEXT NOT NULL,
                password_salt        TEXT NOT NULL,
                must_change_password INTEGER NOT NULL DEFAULT 0 CHECK (must_change_password IN (0, 1)),
                created_at           TEXT NOT NULL
            )
            """,
            "CREATE INDEX IF NOT EXISTS idx_candidates_election ON candidates(election_id)",
            "CREATE INDEX IF NOT EXISTS idx_votes_election ON votes(election_id)",
            "CREATE INDEX IF NOT EXISTS idx_votes_candidate ON votes(candidate_id)",
            "PRAGMA user_version = 1"
    };

    private final Path databaseFile;
    private final Clock clock;
    private final PasswordHasher passwordHasher;

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException("The SQLite JDBC driver is missing from the classpath.", ex);
        }
    }

    public DatabaseManager(Path databaseFile) {
        this(databaseFile, Clock.systemDefaultZone());
    }

    public DatabaseManager(Path databaseFile, Clock clock) {
        if (databaseFile == null) {
            throw new IllegalArgumentException("Database file is required.");
        }
        this.databaseFile = databaseFile.toAbsolutePath().normalize();
        this.clock = clock;
        this.passwordHasher = new PasswordHasher();
    }

    /** Chooses the normal application-data database, or a path from -Dvotesecure.db. */
    public static DatabaseManager createDefault(Clock clock) {
        String override = System.getProperty("votesecure.db");
        Path path;
        if (override != null && !override.isBlank()) {
            path = Path.of(override);
        } else {
            path = Path.of(System.getProperty("user.home"), ".votesecure", "votesecure.db");
        }
        return new DatabaseManager(path, clock);
    }

    public Path getDatabaseFile() {
        return databaseFile;
    }

    public Path getApplicationDataDirectory() {
        Path parent = databaseFile.getParent();
        return parent == null ? Path.of(".").toAbsolutePath() : parent;
    }

    /**
     * Opens a connection with foreign-key enforcement and a busy timeout so a
     * second window waits briefly instead of corrupting a concurrent vote.
     */
    public Connection getConnection() throws DataAccessException {
        try {
            Path parent = databaseFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile);
            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA foreign_keys = ON");
                statement.execute("PRAGMA busy_timeout = 5000");
            }
            return connection;
        } catch (SQLException | IOException ex) {
            LOGGER.log(Level.SEVERE, "Could not open the SQLite database.", ex);
            throw new DataAccessException("The application database could not be opened.", ex);
        }
    }

    /** Creates missing tables and demo records. Safe to call on every launch. */
    public void initialize() throws DataAccessException {
        createSchema();
        ensureDefaultAdministrator();
        seedDemonstrationDataIfEmpty();
    }

    public void createSchema() throws DataAccessException {
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA journal_mode = WAL");
            for (String sql : SCHEMA) {
                statement.execute(sql);
            }
            LOGGER.info(() -> "Database schema is ready at " + databaseFile);
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Could not create the database schema.", ex);
            throw new DataAccessException("The application database could not be prepared.", ex);
        }
    }

    /** Creates the demonstration administrator only when it does not exist. */
    public void ensureDefaultAdministrator() throws DataAccessException {
        String checkSql = "SELECT COUNT(*) FROM admins WHERE username = ?";
        try (Connection connection = getConnection();
             PreparedStatement check = connection.prepareStatement(checkSql)) {
            check.setString(1, DEMO_ADMIN_USERNAME);
            try (ResultSet rows = check.executeQuery()) {
                if (rows.next() && rows.getInt(1) > 0) {
                    return;
                }
            }
            String salt = passwordHasher.newSalt();
            String hash = passwordHasher.hash(DEMO_ADMIN_PASSWORD, salt);
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO admins (username, password_hash, password_salt, must_change_password, created_at) "
                            + "VALUES (?, ?, ?, 0, ?)")) {
                insert.setString(1, DEMO_ADMIN_USERNAME);
                insert.setString(2, hash);
                insert.setString(3, salt);
                insert.setString(4, DateTimeUtils.toStorage(LocalDateTime.now(clock)));
                insert.executeUpdate();
            }
            LOGGER.info("Demo administrator account created.");
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Could not create the demo administrator.", ex);
            throw new DataAccessException("The administrator account could not be prepared.", ex);
        }
    }

    /**
     * Populates an empty database with one open demonstration election,
     * three candidates, ten voters and a demo administrator.
     */
    public void seedDemonstrationDataIfEmpty() throws DataAccessException {
        try (Connection connection = getConnection()) {
            try (PreparedStatement check = connection.prepareStatement("SELECT COUNT(*) FROM elections");
                 ResultSet rows = check.executeQuery()) {
                if (rows.next() && rows.getInt(1) > 0) {
                    return;
                }
            }

            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                LocalDateTime now = LocalDateTime.now(clock).withNano(0);
                LocalDateTime start = now.minusMinutes(30);
                LocalDateTime end = now.plusDays(30);

                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO elections (election_id, election_name, start_datetime, end_datetime, created_at) "
                                + "VALUES (?, ?, ?, ?, ?)")) {
                    insert.setString(1, "E2026");
                    insert.setString(2, "Student Council Election 2026");
                    insert.setString(3, DateTimeUtils.toStorage(start));
                    insert.setString(4, DateTimeUtils.toStorage(end));
                    insert.setString(5, DateTimeUtils.toStorage(now));
                    insert.executeUpdate();
                }

                insertCandidate(connection, "C001", "Aarav Sharma", "Progress Party",
                        "Focuses on better library hours, study spaces and campus Wi-Fi.");
                insertCandidate(connection, "C002", "Meera Iyer", "Unity Party",
                        "Promotes inclusive clubs, mental-health support and student welfare.");
                insertCandidate(connection, "C003", "Rohan Verma", "Reform Party",
                        "Campaigns for transparent budgets, sports facilities and green campus projects.");

                List<String> voterNames = List.of(
                        "Ananya Rao", "Vikram Singh", "Priya Nair", "Arjun Mehta", "Sara Khan",
                        "Dev Patel", "Neha Kulkarni", "Kabir Joshi", "Isha Verma", "Aditya Menon");
                for (int i = 0; i < voterNames.size(); i++) {
                    String voterId = String.format("V%03d", i + 1);
                    String salt = passwordHasher.newSalt();
                    String hash = passwordHasher.hash(DEMO_VOTER_PIN, salt);
                    try (PreparedStatement insert = connection.prepareStatement(
                            "INSERT INTO voters (voter_id, name, pin_hash, pin_salt, eligible, has_voted, registered_at) "
                                    + "VALUES (?, ?, ?, ?, 1, 0, ?)")) {
                        insert.setString(1, voterId);
                        insert.setString(2, voterNames.get(i));
                        insert.setString(3, hash);
                        insert.setString(4, salt);
                        insert.setString(5, DateTimeUtils.toStorage(now));
                        insert.executeUpdate();
                    }
                }

                connection.commit();
                connection.setAutoCommit(originalAutoCommit);
                LOGGER.info("Demonstration election, candidates and voters created.");
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                if (!connection.getAutoCommit()) {
                    connection.setAutoCommit(true);
                }
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Could not seed demonstration data.", ex);
            throw new DataAccessException("Demonstration data could not be created.", ex);
        }
    }

    private void insertCandidate(Connection connection, String id, String name,
                                 String party, String description) throws SQLException {
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO candidates (candidate_id, election_id, name, party, description, created_at) "
                        + "VALUES (?, 'E2026', ?, ?, ?, ?)")) {
            insert.setString(1, id);
            insert.setString(2, name);
            insert.setString(3, party);
            insert.setString(4, description);
            insert.setString(5, DateTimeUtils.toStorage(LocalDateTime.now(clock)));
            insert.executeUpdate();
        }
    }
}
