package com.votesecure.repository;

import com.votesecure.exception.DataAccessException;
import com.votesecure.exception.DuplicateEntityException;
import com.votesecure.exception.ValidationException;
import com.votesecure.model.Voter;

import java.util.List;
import java.util.Optional;

/** Persistence contract for voters. */
public interface VoterRepository {

    Optional<Voter> findById(String voterId) throws DataAccessException;

    List<Voter> findAll() throws DataAccessException;

    List<Voter> search(String query) throws DataAccessException;

    void save(Voter voter) throws DataAccessException, DuplicateEntityException;

    void updateEligibility(String voterId, boolean eligible) throws DataAccessException, ValidationException;

    int countEligible() throws DataAccessException;

    int countAll() throws DataAccessException;
}
