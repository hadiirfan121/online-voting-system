package com.votesecure.repository;

import com.votesecure.exception.DataAccessException;
import com.votesecure.exception.ValidationException;
import com.votesecure.model.Election;

import java.util.List;
import java.util.Optional;

/** Persistence contract for elections. */
public interface ElectionRepository {

    Optional<Election> findById(String electionId) throws DataAccessException;

    /** The election shown by this single-election prototype. */
    Optional<Election> findCurrent() throws DataAccessException;

    List<Election> findAll() throws DataAccessException;

    void save(Election election) throws DataAccessException;

    void update(Election election) throws DataAccessException, ValidationException;
}
