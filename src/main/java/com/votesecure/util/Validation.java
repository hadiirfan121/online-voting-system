package com.votesecure.util;

import com.votesecure.exception.ValidationException;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Central place for user-input validation. Keeping the rules here means the
 * same checks are used by the admin screens, the services and the tests.
 */
public final class Validation {

    private static final Pattern IDENTIFIER = Pattern.compile("[A-Z0-9_-]{3,20}");
    private static final Pattern PIN = Pattern.compile("[0-9]{4,12}");
    private static final Pattern ADMIN_USERNAME = Pattern.compile("[a-z0-9_.-]{3,30}");
    private static final Pattern NAME = Pattern.compile("[\\p{L} .'\\-]{2,80}");

    private Validation() {
    }

    public static String normalizeIdentifier(String value, String fieldLabel) throws ValidationException {
        String cleaned = requireText(value, fieldLabel).toUpperCase(Locale.ROOT);
        if (!IDENTIFIER.matcher(cleaned).matches()) {
            throw new ValidationException(fieldLabel + " must be 3-20 characters using letters, numbers, hyphen or underscore.");
        }
        return cleaned;
    }

    public static String normalizeAdminUsername(String value) throws ValidationException {
        String cleaned = requireText(value, "Administrator username").toLowerCase(Locale.ROOT);
        if (!ADMIN_USERNAME.matcher(cleaned).matches()) {
            throw new ValidationException("Administrator username must be 3-30 characters using lowercase letters, numbers, dot, hyphen or underscore.");
        }
        return cleaned;
    }

    public static String requireName(String value, String fieldLabel) throws ValidationException {
        String cleaned = requireText(value, fieldLabel);
        if (!NAME.matcher(cleaned).matches()) {
            throw new ValidationException(fieldLabel + " must be 2-80 characters and contain only letters, spaces, dots, apostrophes or hyphens.");
        }
        return cleaned;
    }

    public static String requirePartyName(String value) throws ValidationException {
        String cleaned = requireText(value, "Political party");
        if (cleaned.length() < 2 || cleaned.length() > 80) {
            throw new ValidationException("Political party must be between 2 and 80 characters.");
        }
        if (containsControlCharacter(cleaned)) {
            throw new ValidationException("Political party contains unsupported characters.");
        }
        return cleaned;
    }

    public static String requireElectionName(String value) throws ValidationException {
        String cleaned = requireText(value, "Election name");
        if (cleaned.length() < 3 || cleaned.length() > 100) {
            throw new ValidationException("Election name must be between 3 and 100 characters.");
        }
        if (containsControlCharacter(cleaned)) {
            throw new ValidationException("Election name contains unsupported characters.");
        }
        return cleaned;
    }

    public static String optionalDescription(String value) throws ValidationException {
        if (value == null) {
            return "";
        }
        String cleaned = value.trim();
        if (cleaned.length() > 300) {
            throw new ValidationException("Description must be 300 characters or fewer.");
        }
        if (containsControlCharacter(cleaned)) {
            throw new ValidationException("Description contains unsupported characters.");
        }
        return cleaned;
    }

    public static String requirePin(String value) throws ValidationException {
        String cleaned = value == null ? "" : value.trim();
        if (!PIN.matcher(cleaned).matches()) {
            throw new ValidationException("PIN must be 4-12 digits.");
        }
        return cleaned;
    }

    public static String requirePassword(String value) throws ValidationException {
        if (value == null || value.isEmpty()) {
            throw new ValidationException("Password is required.");
        }
        if (value.length() < 8 || value.length() > 64) {
            throw new ValidationException("Password must be between 8 and 64 characters.");
        }
        if (value.chars().anyMatch(Character::isWhitespace)) {
            throw new ValidationException("Password must not contain spaces.");
        }
        boolean upper = value.chars().anyMatch(Character::isUpperCase);
        boolean lower = value.chars().anyMatch(Character::isLowerCase);
        boolean digit = value.chars().anyMatch(Character::isDigit);
        boolean special = value.chars().anyMatch(ch -> !Character.isLetterOrDigit(ch));
        if (!(upper && lower && digit && special)) {
            throw new ValidationException("Password must include uppercase, lowercase, number and special character.");
        }
        return value;
    }

    public static String requireText(String value, String fieldLabel) throws ValidationException {
        if (value == null || value.isBlank()) {
            throw new ValidationException(fieldLabel + " is required.");
        }
        return value.trim();
    }

    public static void requireChronologicalWindow(LocalDateTime start, LocalDateTime end) throws ValidationException {
        if (start == null) {
            throw new ValidationException("Election start date/time is required.");
        }
        if (end == null) {
            throw new ValidationException("Election end date/time is required.");
        }
        if (!end.isAfter(start)) {
            throw new ValidationException("The election end date/time must be after the start date/time.");
        }
    }

    private static boolean containsControlCharacter(String value) {
        return value.chars().anyMatch(Character::isISOControl);
    }
}
