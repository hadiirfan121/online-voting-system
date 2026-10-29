package com.votesecure.repository;

import com.votesecure.database.DatabaseManager;
import com.votesecure.exception.DataAccessException;
import com.votesecure.exception.ValidationException;
import com.votesecure.model.Election;
import com.votesecure.util.DateTimeUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/** JDBC implementation of {@link ElectionRepository}. */
public final class JdbcElectionRepository implements ElectionRepository {

    private static final Logger LOGGER = Logger.getLogger(JdbcElectionRepository.class.getName());
    private final DatabaseManager database;

    public JdbcElectionRepository(DatabaseManager database) {
        this.database = database;
    }

    @Override
    public Optional<Election> findById(String electionId) throws DataAccessException {
        String sql = "SELECT election_id, election_name, start_datetime, end_datetime "
                + "FROM elections WHERE election_id = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, electionId);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? Optional.of(map(rows)) : Optional.empty();
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Could not read election by ID.", ex);
            throw new DataAccessException("The election could not be loaded.", ex);
        }
    }

    @Override
    public Optional<Election> findCurrent() throws DataAccessException {
        String sql = "SELECT election_id, election_name, start_datetime, end_datetime "
                + "FROM elections ORDER BY created_at DESC, election_id DESC LIMIT 1";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rows = statement.executeQuery()) {
            return rows.next() ? Optional.of(map(rows)) : Optional.empty();
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Could not read the current election.", ex);
            throw new DataAccessException("The election could not be loaded.", ex);
        }
    }

    @Override
    public List<Election> findAll() throws DataAccessException {
        String sql = "SELECT election_id, election_name, start_datetime, end_datetime "
                + "FROM elections ORDER BY created_at, election_id";
        List<Election> elections = new ArrayList<>();
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                elections.add(map(rows));
            }
            return elections;
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Could not list elections.", ex);
            throw new DataAccessException("Elections could not be loaded.", ex);
        }
    }

    @Override
    public void save(Election election) throws DataAccessException {
        String sql = "INSERT INTO elections (election_id, election_name, start_datetime, end_datetime, created_at) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, election.getElectionId());
            statement.setString(2, election.getElectionName());
            statement.setString(3, DateTimeUtils.toStorage(election.getStartDateTime()));
            statement.setString(4, DateTimeUtils.toStorage(election.getEndDateTime()));
            statement.setString(5, DateTimeUtils.toStorage(LocalDateTime.now()));
            statement.executeUpdate();
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Could not save election.", ex);
            throw new DataAccessException("The election could not be saved.", ex);
        }
    }

    @Override
    public void update(Election election) throws DataAccessException, ValidationException {
        String sql = "UPDATE elections SET election_name = ?, start_datetime = ?, end_datetime = ? "
                + "WHERE election_id = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, election.getElectionName());
            statement.setString(2, DateTimeUtils.toStorage(election.getStartDateTime()));
            statement.setString(3, DateTimeUtils.toStorage(election.getEndDateTime()));
            statement.setString(4, election.getElectionId());
            if (statement.executeUpdate() != 1) {
                throw new ValidationException("The election could not be found.");
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Could not update election.", ex);
            throw new DataAccessException("The election settings could not be saved.", ex);
        }
    }

    private Election map(ResultSet rows) throws SQLException, DataAccessException {
        try {
            return new Election(
                    rows.getString("election_id"),
                    rows.getString("election_name"),
                    DateTimeUtils.fromStorage(rows.getString("start_datetime")),
                    DateTimeUtils.fromStorage(rows.getString("end_datetime")));
        } catch (DateTimeParseException ex) {
            throw new DataAccessException("Stored election dates are invalid.", ex);
        }
    }
}
