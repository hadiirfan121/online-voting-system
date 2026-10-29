package com.votesecure.repository;

import com.votesecure.database.DatabaseManager;
import com.votesecure.exception.DataAccessException;
import com.votesecure.exception.DuplicateVoteException;
import com.votesecure.exception.ValidationException;
import com.votesecure.util.DateTimeUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

/** JDBC implementation of {@link VoteRepository}. */
public final class JdbcVoteRepository implements VoteRepository {

    private static final Logger LOGGER = Logger.getLogger(JdbcVoteRepository.class.getName());
    private final DatabaseManager database;

    public JdbcVoteRepository(DatabaseManager database) {
        this.database = database;
    }

    @Override
    public long castVote(String electionId, String voterId, String candidateId, LocalDateTime votedAt)
            throws DataAccessException, DuplicateVoteException, ValidationException {
        try (Connection connection = database.getConnection()) {
            try {
                connection.setAutoCommit(false);

                long voteId;
                String insertSql = "INSERT INTO votes (election_id, voter_id, candidate_id, voted_at) "
                        + "VALUES (?, ?, ?, ?)";
                try (PreparedStatement insert = connection.prepareStatement(
                        insertSql, Statement.RETURN_GENERATED_KEYS)) {
                    insert.setString(1, electionId);
                    insert.setString(2, voterId);
                    insert.setString(3, candidateId);
                    insert.setString(4, DateTimeUtils.toStorage(votedAt));
                    insert.executeUpdate();
                    try (ResultSet keys = insert.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("SQLite did not return a generated vote ID.");
                        }
                        voteId = keys.getLong(1);
                    }
                }

                try (PreparedStatement update = connection.prepareStatement(
                        "UPDATE voters SET has_voted = 1 WHERE voter_id = ? AND has_voted = 0")) {
                    update.setString(1, voterId);
                    if (update.executeUpdate() != 1) {
                        throw new DuplicateVoteException(
                                "This voter has already cast a vote in this election.");
                    }
                }

                connection.commit();
                return voteId;
            } catch (SQLException ex) {
                rollbackQuietly(connection);
                if (SqlErrors.isUniqueViolation(ex)) {
                    throw new DuplicateVoteException(
                            "This voter has already cast a vote in this election.", ex);
                }
                if (SqlErrors.isForeignKeyViolation(ex)) {
                    throw new ValidationException(
                            "The selected voter, candidate or election is not valid.", ex);
                }
                LOGGER.log(Level.SEVERE, "Vote transaction failed and was rolled back.", ex);
                throw new DataAccessException("The vote could not be saved.", ex);
            } catch (DuplicateVoteException ex) {
                rollbackQuietly(connection);
                throw ex;
            } catch (RuntimeException ex) {
                rollbackQuietly(connection);
                throw ex;
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Could not close the vote transaction.", ex);
            throw new DataAccessException("The vote could not be saved.", ex);
        }
    }

    @Override
    public boolean hasVoted(String electionId, String voterId) throws DataAccessException {
        String sql = "SELECT COUNT(*) FROM votes WHERE election_id = ? AND voter_id = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, electionId);
            statement.setString(2, voterId);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() && rows.getInt(1) > 0;
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Could not check voter ballot status.", ex);
            throw new DataAccessException("The voter's ballot status could not be read.", ex);
        }
    }

    @Override
    public int countByElection(String electionId) throws DataAccessException {
        String sql = "SELECT COUNT(*) FROM votes WHERE election_id = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, electionId);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? rows.getInt(1) : 0;
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Could not count votes for election.", ex);
            throw new DataAccessException("Vote totals could not be read.", ex);
        }
    }

    @Override
    public int countByCandidate(String candidateId) throws DataAccessException {
        String sql = "SELECT COUNT(*) FROM votes WHERE candidate_id = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, candidateId);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? rows.getInt(1) : 0;
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Could not count votes for candidate.", ex);
            throw new DataAccessException("Candidate vote totals could not be read.", ex);
        }
    }

    @Override
    public int countAll() throws DataAccessException {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM votes");
             ResultSet rows = statement.executeQuery()) {
            return rows.next() ? rows.getInt(1) : 0;
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Could not count all votes.", ex);
            throw new DataAccessException("Vote totals could not be read.", ex);
        }
    }

    private void rollbackQuietly(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException rollbackFailure) {
            LOGGER.log(Level.WARNING, "Vote rollback also failed.", rollbackFailure);
        }
    }
}
