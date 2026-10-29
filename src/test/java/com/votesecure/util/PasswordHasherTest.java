package com.votesecure.util;

import com.votesecure.exception.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTest {

    private final PasswordHasher hasher = new PasswordHasher();

    @Test
    @DisplayName("A hash verifies its own password and rejects a different one")
    void hashVerification() {
        String salt = hasher.newSalt();
        String hash = hasher.hash("Admin@123", salt);

        assertTrue(hasher.verify("Admin@123", hash, salt));
        assertFalse(hasher.verify("Admin@124", hash, salt));
        assertFalse(hasher.verify("Admin@123", hash, hasher.newSalt()));
    }

    @Test
    @DisplayName("Each salt is unique and produces a different hash for the same password")
    void saltsAreUnique() {
        String firstSalt = hasher.newSalt();
        String secondSalt = hasher.newSalt();
        assertNotEquals(firstSalt, secondSalt);
        assertNotEquals(hasher.hash("4826", firstSalt), hasher.hash("4826", secondSalt));
    }

    @Test
    @DisplayName("A hash never contains the raw password")
    void hashDoesNotContainRawPassword() {
        String salt = hasher.newSalt();
        String hash = hasher.hash("4826", salt);
        assertNotEquals("4826", hash);
        assertFalse(hash.contains("4826"));
        assertTrue(hash.length() > 40);
    }

    @Test
    @DisplayName("PIN validation accepts digits only and rejects weak input")
    void pinValidation() {
        assertDoesNotThrow(() -> Validation.requirePin("4826"));
        assertThrows(ValidationException.class, () -> Validation.requirePin("12"));
        assertThrows(ValidationException.class, () -> Validation.requirePin("abcd"));
        assertThrows(ValidationException.class, () -> Validation.requirePin("12 34"));
    }

    @Test
    @DisplayName("Password validation enforces a basic strength policy")
    void passwordValidation() {
        assertDoesNotThrow(() -> Validation.requirePassword("Strong@123"));
        assertThrows(ValidationException.class, () -> Validation.requirePassword("weak"));
        assertThrows(ValidationException.class, () -> Validation.requirePassword("alllowercase1!"));
        assertThrows(ValidationException.class, () -> Validation.requirePassword("NoDigits!!"));
    }
}
