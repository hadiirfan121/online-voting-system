package com.votesecure.repository;

import com.votesecure.exception.DataAccessException;
import com.votesecure.exception.DuplicateEntityException;
import com.votesecure.exception.ValidationException;
import com.votesecure.model.Administrator;

import java.util.Optional;

/** Persistence contract for administrator accounts. */
public interface AdminRepository {

    Optional<Administrator> findByUsername(String username) throws DataAccessException;

    void save(Administrator administrator) throws DataAccessException, DuplicateEntityException;

    void updatePassword(String username, String passwordHash, String passwordSalt, boolean mustChangePassword)
            throws DataAccessException, ValidationException;
}
