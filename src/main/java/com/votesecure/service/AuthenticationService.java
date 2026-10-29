package com.votesecure.service;

import com.votesecure.exception.AuthenticationException;
import com.votesecure.exception.DataAccessException;
import com.votesecure.model.Administrator;
import com.votesecure.model.Voter;
import com.votesecure.repository.AdminRepository;
import com.votesecure.repository.VoterRepository;
import com.votesecure.util.PasswordHasher;

import java.util.Locale;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Verifies voter PINs and administrator passwords against salted hashes.
 * Failure messages do not reveal whether the ID or the credential was wrong.
 */
public final class AuthenticationService {

    private static final Logger LOGGER = Logger.getLogger(AuthenticationService.class.getName());

    private final VoterRepository voters;
    private final AdminRepository administrators;
    private final PasswordHasher passwordHasher;

    public AuthenticationService(VoterRepository voters,
                                 AdminRepository administrators,
                                 PasswordHasher passwordHasher) {
        this.voters = voters;
        this.administrators = administrators;
        this.passwordHasher = passwordHasher;
    }

    public Voter authenticateVoter(String voterId, String pin) throws AuthenticationException {
        String normalizedId = voterId == null ? "" : voterId.trim().toUpperCase(Locale.ROOT);
        if (normalizedId.isEmpty() || pin == null || pin.isEmpty()) {
            throw new AuthenticationException("Please enter your voter ID and PIN.");
        }
        try {
            Optional<Voter> found = voters.findById(normalizedId);
            if (found.isEmpty()) {
                throw new AuthenticationException("Invalid voter ID or PIN.");
            }
            Voter voter = found.get();
            if (!passwordHasher.verify(pin, voter.getPinHash(), voter.getPinSalt())) {
                throw new AuthenticationException("Invalid voter ID or PIN.");
            }
            return voter;
        } catch (DataAccessException ex) {
            LOGGER.log(Level.SEVERE, "Voter authentication database failure.", ex);
            throw new AuthenticationException("Sign-in is temporarily unavailable. Please try again.", ex);
        }
    }

    public Administrator authenticateAdministrator(String username, String password)
            throws AuthenticationException {
        String normalizedUsername = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
        if (normalizedUsername.isEmpty() || password == null || password.isEmpty()) {
            throw new AuthenticationException("Please enter the administrator username and password.");
        }
        try {
            Optional<Administrator> found = administrators.findByUsername(normalizedUsername);
            if (found.isEmpty()) {
                throw new AuthenticationException("Invalid administrator username or password.");
            }
            Administrator administrator = found.get();
            if (!passwordHasher.verify(password, administrator.getPasswordHash(), administrator.getPasswordSalt())) {
                throw new AuthenticationException("Invalid administrator username or password.");
            }
            return administrator;
        } catch (DataAccessException ex) {
            LOGGER.log(Level.SEVERE, "Administrator authentication database failure.", ex);
            throw new AuthenticationException("Sign-in is temporarily unavailable. Please try again.", ex);
        }
    }
}
