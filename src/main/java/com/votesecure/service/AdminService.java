package com.votesecure.service;

import com.votesecure.exception.AuthenticationException;
import com.votesecure.exception.ConfigurationLockedException;
import com.votesecure.exception.DataAccessException;
import com.votesecure.exception.ValidationException;
import com.votesecure.exception.VoteSecureException;
import com.votesecure.model.Administrator;
import com.votesecure.model.Candidate;
import com.votesecure.model.Election;
import com.votesecure.model.Voter;
import com.votesecure.repository.AdminRepository;
import com.votesecure.repository.CandidateRepository;
import com.votesecure.repository.VoteRepository;
import com.votesecure.repository.VoterRepository;
import com.votesecure.util.PasswordHasher;
import com.votesecure.util.Validation;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Administrator use cases: voter registration, candidate management, election
 * settings and password change. All business validation lives here rather than
 * in the Swing screens.
 */
public final class AdminService {

    private final VoterRepository voters;
    private final CandidateRepository candidates;
    private final AdminRepository administrators;
    private final VoteRepository votes;
    private final ElectionService electionService;
    private final PasswordHasher passwordHasher;
    private final Clock clock;

    public AdminService(VoterRepository voters,
                        CandidateRepository candidates,
                        AdminRepository administrators,
                        VoteRepository votes,
                        ElectionService electionService,
                        PasswordHasher passwordHasher,
                        Clock clock) {
        this.voters = voters;
        this.candidates = candidates;
        this.administrators = administrators;
        this.votes = votes;
        this.electionService = electionService;
        this.passwordHasher = passwordHasher;
        this.clock = clock;
    }

    public Voter registerVoter(String voterId, String name, String pin, boolean eligible)
            throws VoteSecureException {
        String normalizedId = Validation.normalizeIdentifier(voterId, "Voter ID");
        String cleanName = Validation.requireName(name, "Voter name");
        String cleanPin = Validation.requirePin(pin);

        String salt = passwordHasher.newSalt();
        String hash = passwordHasher.hash(cleanPin, salt);
        Voter voter = new Voter(normalizedId, cleanName, eligible, false, hash, salt,
                LocalDateTime.now(clock));
        voters.save(voter);
        return voter;
    }

    public List<Voter> listVoters() throws DataAccessException {
        return voters.findAll();
    }

    public List<Voter> searchVoters(String query) throws DataAccessException {
        return voters.search(query);
    }

    public Candidate addCandidate(String candidateId, String name, String party, String description)
            throws VoteSecureException {
        electionService.requireConfigurationEditable();
        Election election = electionService.getCurrentElection();
        String normalizedId = Validation.normalizeIdentifier(candidateId, "Candidate ID");
        String cleanName = Validation.requireName(name, "Candidate name");
        String cleanParty = Validation.requirePartyName(party);
        String cleanDescription = Validation.optionalDescription(description);

        Candidate candidate = new Candidate(normalizedId, election.getElectionId(),
                cleanName, cleanParty, cleanDescription, LocalDateTime.now(clock));
        candidates.save(candidate);
        return candidate;
    }

    public Candidate updateCandidate(String candidateId, String name, String party, String description)
            throws VoteSecureException {
        electionService.requireConfigurationEditable();
        String normalizedId = Validation.normalizeIdentifier(candidateId, "Candidate ID");
        Candidate existing = candidates.findById(normalizedId)
                .orElseThrow(() -> new ValidationException("The candidate could not be found."));
        String cleanName = Validation.requireName(name, "Candidate name");
        String cleanParty = Validation.requirePartyName(party);
        String cleanDescription = Validation.optionalDescription(description);

        Candidate updated = new Candidate(existing.getCandidateId(), existing.getElectionId(),
                cleanName, cleanParty, cleanDescription, existing.getCreatedAt());
        candidates.update(updated);
        return updated;
    }

    public void deleteCandidate(String candidateId) throws VoteSecureException {
        electionService.requireConfigurationEditable();
        String normalizedId = Validation.normalizeIdentifier(candidateId, "Candidate ID");
        if (votes.countByCandidate(normalizedId) > 0) {
            throw new ConfigurationLockedException(
                    "This candidate has recorded votes and cannot be deleted.");
        }
        candidates.delete(normalizedId);
    }

    public Election updateElectionSettings(String electionName, LocalDateTime start, LocalDateTime end)
            throws VoteSecureException {
        Election current = electionService.getCurrentElection();
        if (electionService.isConfigurationLocked(current)) {
            throw new ConfigurationLockedException(
                    electionService.configurationLockMessage(current));
        }
        String cleanName = Validation.requireElectionName(electionName);
        Validation.requireChronologicalWindow(start, end);
        Election updated = current.withWindow(cleanName, start, end);
        electionService.updateElection(updated);
        return updated;
    }

    public void changeAdministratorPassword(String username, String currentPassword, String newPassword)
            throws VoteSecureException {
        String normalizedUsername = Validation.normalizeAdminUsername(username);
        Administrator administrator = administrators.findByUsername(normalizedUsername)
                .orElseThrow(() -> new AuthenticationException("The administrator account could not be found."));
        if (!passwordHasher.verify(currentPassword, administrator.getPasswordHash(), administrator.getPasswordSalt())) {
            throw new AuthenticationException("The current password is incorrect.");
        }
        String cleanNewPassword = Validation.requirePassword(newPassword);
        if (passwordHasher.verify(cleanNewPassword, administrator.getPasswordHash(), administrator.getPasswordSalt())) {
            throw new ValidationException("The new password must be different from the current password.");
        }
        String salt = passwordHasher.newSalt();
        String hash = passwordHasher.hash(cleanNewPassword, salt);
        administrators.updatePassword(normalizedUsername, hash, salt, false);
    }

    public int getRegisteredVoterCount() throws DataAccessException {
        return voters.countAll();
    }

    public int getCandidateCount() throws DataAccessException {
        Election election = electionService.getCurrentElection();
        return candidates.countByElection(election.getElectionId());
    }

    public int getVotesCastCount() throws DataAccessException {
        Election election = electionService.getCurrentElection();
        return votes.countByElection(election.getElectionId());
    }

    public List<Candidate> listCandidates() throws DataAccessException {
        Election election = electionService.getCurrentElection();
        return candidates.findByElection(election.getElectionId());
    }
}
