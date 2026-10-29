package com.votesecure.repository;

import com.votesecure.exception.DataAccessException;
import com.votesecure.exception.DuplicateEntityException;
import com.votesecure.exception.ValidationException;
import com.votesecure.model.Candidate;

import java.util.List;
import java.util.Optional;

/** Persistence contract for candidates. */
public interface CandidateRepository {

    Optional<Candidate> findById(String candidateId) throws DataAccessException;

    List<Candidate> findByElection(String electionId) throws DataAccessException;

    void save(Candidate candidate) throws DataAccessException, DuplicateEntityException, ValidationException;

    void update(Candidate candidate) throws DataAccessException, ValidationException;

    void delete(String candidateId) throws DataAccessException, ValidationException;

    int countByElection(String electionId) throws DataAccessException;
}
