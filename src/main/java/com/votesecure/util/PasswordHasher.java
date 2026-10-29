package com.votesecure.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

/**
 * Creates and verifies salted PBKDF2 password hashes.
 *
 * <p>The plain PIN/password is converted to a hash before it reaches the
 * database. Verification uses a constant-time comparison.</p>
 */
public final class PasswordHasher {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 120_000;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int SALT_LENGTH_BYTES = 16;

    private final SecureRandom secureRandom = new SecureRandom();

    /** Returns a new random salt encoded as Base64. */
    public String newSalt() {
        byte[] salt = new byte[SALT_LENGTH_BYTES];
        secureRandom.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /** Hashes a raw credential with the supplied Base64 salt. */
    public String hash(String rawCredential, String saltBase64) {
        if (rawCredential == null || rawCredential.isEmpty()) {
            throw new IllegalArgumentException("Credential is required for hashing.");
        }
        if (saltBase64 == null || saltBase64.isBlank()) {
            throw new IllegalArgumentException("Salt is required for hashing.");
        }
        byte[] salt;
        try {
            salt = Base64.getDecoder().decode(saltBase64);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Salt is not valid Base64.", ex);
        }
        return Base64.getEncoder().encodeToString(derive(rawCredential.toCharArray(), salt));
    }

    /** Verifies a raw credential against a stored hash and salt. */
    public boolean verify(String rawCredential, String expectedHashBase64, String saltBase64) {
        if (rawCredential == null || expectedHashBase64 == null || saltBase64 == null) {
            return false;
        }
        try {
            byte[] salt = Base64.getDecoder().decode(saltBase64);
            byte[] expected = Base64.getDecoder().decode(expectedHashBase64);
            byte[] actual = derive(rawCredential.toCharArray(), salt);
            return MessageDigest.isEqual(expected, actual);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private byte[] derive(char[] credential, byte[] salt) {
        PBEKeySpec spec = new PBEKeySpec(credential, salt, ITERATIONS, KEY_LENGTH_BITS);
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            return factory.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
            throw new IllegalStateException("Secure password hashing is not available.", ex);
        } finally {
            spec.clearPassword();
        }
    }
}
