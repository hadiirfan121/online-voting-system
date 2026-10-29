package com.votesecure.repository;

import com.votesecure.database.DatabaseManager;
import com.votesecure.exception.DataAccessException;
import com.votesecure.exception.DuplicateEntityException;
import com.votesecure.exception.ValidationException;
import com.votesecure.model.Voter;
import com.votesecure.util.DateTimeUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/** JDBC implementation of {@link VoterRepository}. */
public final class JdbcVoterRepository implements VoterRepository {

    private static final Logger LOGGER = Logger.getLogger(JdbcVoterRepository.class.getName());
    private static final String SELECT_COLUMNS =
            "SELECT voter_id, name, pin_hash, pin_salt, eligible, has_voted, registered_at FROM voters ";
    private final DatabaseManager database;

    public JdbcVoterRepository(DatabaseManager database) {
        this.database = database;
    }

    @Override
    public Optional<Voter> findById(String voterId) throws DataAccessException {
        String sql = SELECT_COLUMNS + "WHERE voter_id = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, voterId);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? Optional.of(map(rows)) : Optional.empty();
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Could not read voter.", ex);
            throw new DataAccessException("The voter could not be loaded.", ex);
        }
    }

    @Override
    public List<Voter> findAll() throws DataAccessException {
        String sql = SELECT_COLUMNS + "ORDER BY voter_id";
        List<Voter> voters = new ArrayList<>();
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                voters.add(map(rows));
            }
            return voters;
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Could not list voters.", ex);
            throw new DataAccessException("Voters could not be loaded.", ex);
        }
    }

    @Override
    public List<Voter> search(String query) throws DataAccessException {
        if (query == null || query.isBlank()) {
            return findAll();
        }
        String sql = SELECT_COLUMNS
                + "WHERE LOWER(voter_id) LIKE ? OR LOWER(name) LIKE ? ORDER BY voter_id";
        String pattern = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
        List<Voter> voters = new ArrayList<>();
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, pattern);
            statement.setString(2, pattern);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    voters.add(map(rows));
                }
            }
            return voters;
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Could not search voters.", ex);
            throw new DataAccessException("The voter list could not be searched.", ex);
        }
    }

    @Override
    public void save(Voter voter) throws DataAccessException, DuplicateEntityException {
        String sql = "INSERT INTO voters (voter_id, name, pin_hash, pin_salt, eligible, has_voted, registered_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, voter.getVoterId());
            statement.setString(2, voter.getName());
            statement.setString(3, voter.getPinHash());
            statement.setString(4, voter.getPinSalt());
            statement.setInt(5, voter.isEligible() ? 1 : 0);
            statement.setInt(6, voter.hasVoted() ? 1 : 0);
            statement.setString(7, DateTimeUtils.toStorage(voter.getRegisteredAt()));
            statement.executeUpdate();
        } catch (SQLException ex) {
            if (SqlErrors.isUniqueViolation(ex)) {
                throw new DuplicateEntityException("A voter with this ID already exists.", ex);
            }
            LOGGER.log(Level.SEVERE, "Could not save voter.", ex);
            throw new DataAccessException("The voter could not be saved.", ex);
        }
    }

    @Override
    public void updateEligibility(String voterId, boolean eligible)
            throws DataAccessException, ValidationException {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE voters SET eligible = ? WHERE voter_id = ?")) {
            statement.setInt(1, eligible ? 1 : 0);
            statement.setString(2, voterId);
            if (statement.executeUpdate() != 1) {
                throw new ValidationException("The voter could not be found.");
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Could not update voter eligibility.", ex);
            throw new DataAccessException("Voter eligibility could not be updated.", ex);
        }
    }

    @Override
    public int countEligible() throws DataAccessException {
        return count("SELECT COUNT(*) FROM voters WHERE eligible = 1");
    }

    @Override
    public int countAll() throws DataAccessException {
        return count("SELECT COUNT(*) FROM voters");
    }

    private int count(String sql) throws DataAccessException {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rows = statement.executeQuery()) {
            return rows.next() ? rows.getInt(1) : 0;
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Could not count voters.", ex);
            throw new DataAccessException("Voter totals could not be read.", ex);
        }
    }

    private Voter map(ResultSet rows) throws SQLException, DataAccessException {
        try {
            return new Voter(
                    rows.getString("voter_id"),
                    rows.getString("name"),
                    rows.getInt("eligible") == 1,
                    rows.getInt("has_voted") == 1,
                    rows.getString("pin_hash"),
                    rows.getString("pin_salt"),
                    DateTimeUtils.fromStorage(rows.getString("registered_at")));
        } catch (DateTimeParseException ex) {
            throw new DataAccessException("Stored voter dates are invalid.", ex);
        }
    }
}
