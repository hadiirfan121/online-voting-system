package com.votesecure.repository;

import com.votesecure.database.DatabaseManager;
import com.votesecure.exception.DataAccessException;
import com.votesecure.exception.DuplicateEntityException;
import com.votesecure.exception.ValidationException;
import com.votesecure.model.Candidate;
import com.votesecure.util.DateTimeUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/** JDBC implementation of {@link CandidateRepository}. */
public final class JdbcCandidateRepository implements CandidateRepository {

    private static final Logger LOGGER = Logger.getLogger(JdbcCandidateRepository.class.getName());
    private static final String SELECT_COLUMNS =
            "SELECT candidate_id, election_id, name, party, description, created_at FROM candidates ";
    private final DatabaseManager database;

    public JdbcCandidateRepository(DatabaseManager database) {
        this.database = database;
    }

    @Override
    public Optional<Candidate> findById(String candidateId) throws DataAccessException {
        String sql = SELECT_COLUMNS + "WHERE candidate_id = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, candidateId);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? Optional.of(map(rows)) : Optional.empty();
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Could not read candidate.", ex);
            throw new DataAccessException("The candidate could not be loaded.", ex);
        }
    }

    @Override
    public List<Candidate> findByElection(String electionId) throws DataAccessException {
        String sql = SELECT_COLUMNS + "WHERE election_id = ? ORDER BY candidate_id";
        List<Candidate> candidates = new ArrayList<>();
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, electionId);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    candidates.add(map(rows));
                }
            }
            return candidates;
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Could not list candidates.", ex);
            throw new DataAccessException("Candidates could not be loaded.", ex);
        }
    }

    @Override
    public void save(Candidate candidate) throws DataAccessException, DuplicateEntityException, ValidationException {
        String sql = "INSERT INTO candidates (candidate_id, election_id, name, party, description, created_at) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, candidate.getCandidateId());
            statement.setString(2, candidate.getElectionId());
            statement.setString(3, candidate.getName());
            statement.setString(4, candidate.getPoliticalParty());
            statement.setString(5, candidate.getDescription());
            statement.setString(6, DateTimeUtils.toStorage(candidate.getCreatedAt()));
            statement.executeUpdate();
        } catch (SQLException ex) {
            if (SqlErrors.isUniqueViolation(ex)) {
                throw new DuplicateEntityException("A candidate with this ID already exists.", ex);
            }
            if (SqlErrors.isForeignKeyViolation(ex)) {
                throw new ValidationException("The election for this candidate does not exist.", ex);
            }
            LOGGER.log(Level.SEVERE, "Could not save candidate.", ex);
            throw new DataAccessException("The candidate could not be saved.", ex);
        }
    }

    @Override
    public void update(Candidate candidate) throws DataAccessException, ValidationException {
        String sql = "UPDATE candidates SET name = ?, party = ?, description = ? WHERE candidate_id = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, candidate.getName());
            statement.setString(2, candidate.getPoliticalParty());
            statement.setString(3, candidate.getDescription());
            statement.setString(4, candidate.getCandidateId());
            if (statement.executeUpdate() != 1) {
                throw new ValidationException("The candidate could not be found.");
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Could not update candidate.", ex);
            throw new DataAccessException("The candidate could not be updated.", ex);
        }
    }

    @Override
    public void delete(String candidateId) throws DataAccessException, ValidationException {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM candidates WHERE candidate_id = ?")) {
            statement.setString(1, candidateId);
            if (statement.executeUpdate() != 1) {
                throw new ValidationException("The candidate could not be found.");
            }
        } catch (SQLException ex) {
            if (SqlErrors.isForeignKeyViolation(ex)) {
                throw new ValidationException("This candidate has recorded votes and cannot be deleted.", ex);
            }
            LOGGER.log(Level.SEVERE, "Could not delete candidate.", ex);
            throw new DataAccessException("The candidate could not be deleted.", ex);
        }
    }

    @Override
    public int countByElection(String electionId) throws DataAccessException {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM candidates WHERE election_id = ?")) {
            statement.setString(1, electionId);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? rows.getInt(1) : 0;
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Could not count candidates.", ex);
            throw new DataAccessException("Candidate totals could not be read.", ex);
        }
    }

    private Candidate map(ResultSet rows) throws SQLException, DataAccessException {
        try {
            return new Candidate(
                    rows.getString("candidate_id"),
                    rows.getString("election_id"),
                    rows.getString("name"),
                    rows.getString("party"),
                    rows.getString("description"),
                    DateTimeUtils.fromStorage(rows.getString("created_at")));
        } catch (DateTimeParseException ex) {
            throw new DataAccessException("Stored candidate dates are invalid.", ex);
        }
    }
}
