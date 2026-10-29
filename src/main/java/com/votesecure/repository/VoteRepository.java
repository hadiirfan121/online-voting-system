package com.votesecure.repository;

import com.votesecure.exception.DataAccessException;
import com.votesecure.exception.DuplicateVoteException;
import com.votesecure.exception.ValidationException;

import java.time.LocalDateTime;

/**
 * Persistence contract for ballots. There is deliberately no method that lists
 * the voter-to-candidate mapping, because no screen needs that information.
 */
public interface VoteRepository {

    /**
     * Inserts a ballot and marks the voter as having voted inside one
     * transaction. The database unique constraint is the final defence against
     * duplicate submissions.
     *
     * @return generated vote ID
     */
    long castVote(String electionId, String voterId, String candidateId, LocalDateTime votedAt)
            throws DataAccessException, DuplicateVoteException, ValidationException;

    boolean hasVoted(String electionId, String voterId) throws DataAccessException;

    int countByElection(String electionId) throws DataAccessException;

    int countByCandidate(String candidateId) throws DataAccessException;

    int countAll() throws DataAccessException;
}
