package com.votesecure.service;

import com.votesecure.exception.ConfigurationLockedException;
import com.votesecure.exception.DataAccessException;
import com.votesecure.exception.ValidationException;
import com.votesecure.model.Election;
import com.votesecure.model.ElectionStatus;
import com.votesecure.repository.ElectionRepository;
import com.votesecure.repository.VoteRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads the current election and applies the voting-window and configuration
 * rules. The {@link Clock} is injected so tests can simulate any point in time.
 */
public final class ElectionService {

    private final ElectionRepository elections;
    private final VoteRepository votes;
    private final Clock clock;

    public ElectionService(ElectionRepository elections, VoteRepository votes, Clock clock) {
        this.elections = elections;
        this.votes = votes;
        this.clock = clock;
    }

    public Election getCurrentElection() throws DataAccessException {
        return elections.findCurrent()
                .orElseThrow(() -> new DataAccessException("No election is configured."));
    }

    public LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    public ElectionStatus getCurrentStatus() throws DataAccessException {
        return getCurrentElection().statusAt(now());
    }

    public ElectionStatus getStatus(Election election) {
        return election.statusAt(now());
    }

    public boolean isVotingOpen(Election election) {
        return election.statusAt(now()) == ElectionStatus.OPEN;
    }

    /**
     * Configuration is frozen once any ballot exists or once the election has
     * closed. Before the first ballot the administrator may still correct the
     * schedule or candidate list without corrupting a result.
     */
    public boolean isConfigurationLocked(Election election) throws DataAccessException {
        if (!now().isBefore(election.getEndDateTime())) {
            return true;
        }
        return votes.countByElection(election.getElectionId()) > 0;
    }

    public void requireConfigurationEditable() throws DataAccessException, ConfigurationLockedException {
        Election election = getCurrentElection();
        if (isConfigurationLocked(election)) {
            throw new ConfigurationLockedException(configurationLockMessage(election));
        }
    }

    public String configurationLockMessage(Election election) throws DataAccessException {
        List<String> reasons = new ArrayList<>();
        if (votes.countByElection(election.getElectionId()) > 0) {
            reasons.add("at least one vote has been recorded");
        }
        if (!now().isBefore(election.getEndDateTime())) {
            reasons.add("the election has closed");
        }
        if (reasons.isEmpty()) {
            return "Election configuration is currently editable.";
        }
        return "Election configuration is locked because " + String.join(" and ", reasons) + ".";
    }

    public void updateElection(Election updated)
            throws DataAccessException, ValidationException, ConfigurationLockedException {
        Election current = getCurrentElection();
        if (!current.getElectionId().equals(updated.getElectionId())) {
            throw new ValidationException("The election being edited is not the current election.");
        }
        if (isConfigurationLocked(current)) {
            throw new ConfigurationLockedException(configurationLockMessage(current));
        }
        elections.update(updated);
    }
}
