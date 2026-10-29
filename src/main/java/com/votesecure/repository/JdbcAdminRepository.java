package com.votesecure.repository;

import com.votesecure.database.DatabaseManager;
import com.votesecure.exception.DataAccessException;
import com.votesecure.exception.DuplicateEntityException;
import com.votesecure.exception.ValidationException;
import com.votesecure.model.Administrator;
import com.votesecure.util.DateTimeUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/** JDBC implementation of {@link AdminRepository}. */
public final class JdbcAdminRepository implements AdminRepository {

    private static final Logger LOGGER = Logger.getLogger(JdbcAdminRepository.class.getName());
    private final DatabaseManager database;

    public JdbcAdminRepository(DatabaseManager database) {
        this.database = database;
    }

    @Override
    public Optional<Administrator> findByUsername(String username) throws DataAccessException {
        String sql = "SELECT username, password_hash, password_salt, must_change_password, created_at "
                + "FROM admins WHERE username = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? Optional.of(map(rows)) : Optional.empty();
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Could not read administrator account.", ex);
            throw new DataAccessException("The administrator account could not be loaded.", ex);
        }
    }

    @Override
    public void save(Administrator administrator) throws DataAccessException, DuplicateEntityException {
        String sql = "INSERT INTO admins (username, password_hash, password_salt, must_change_password, created_at) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, administrator.getUsername());
            statement.setString(2, administrator.getPasswordHash());
            statement.setString(3, administrator.getPasswordSalt());
            statement.setInt(4, administrator.isMustChangePassword() ? 1 : 0);
            statement.setString(5, DateTimeUtils.toStorage(administrator.getCreatedAt()));
            statement.executeUpdate();
        } catch (SQLException ex) {
            if (SqlErrors.isUniqueViolation(ex)) {
                throw new DuplicateEntityException("That administrator username already exists.", ex);
            }
            LOGGER.log(Level.SEVERE, "Could not save administrator.", ex);
            throw new DataAccessException("The administrator account could not be saved.", ex);
        }
    }

    @Override
    public void updatePassword(String username, String passwordHash, String passwordSalt,
                               boolean mustChangePassword)
            throws DataAccessException, ValidationException {
        String sql = "UPDATE admins SET password_hash = ?, password_salt = ?, must_change_password = ? "
                + "WHERE username = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, passwordHash);
            statement.setString(2, passwordSalt);
            statement.setInt(3, mustChangePassword ? 1 : 0);
            statement.setString(4, username);
            if (statement.executeUpdate() != 1) {
                throw new ValidationException("The administrator account could not be found.");
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Could not update administrator password.", ex);
            throw new DataAccessException("The administrator password could not be updated.", ex);
        }
    }

    private Administrator map(ResultSet rows) throws SQLException, DataAccessException {
        try {
            return new Administrator(
                    rows.getString("username"),
                    rows.getString("password_hash"),
                    rows.getString("password_salt"),
                    rows.getInt("must_change_password") == 1,
                    DateTimeUtils.fromStorage(rows.getString("created_at")));
        } catch (DateTimeParseException ex) {
            throw new DataAccessException("Stored administrator dates are invalid.", ex);
        }
    }
}
