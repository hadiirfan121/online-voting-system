package com.votesecure.util;

import com.votesecure.exception.ValidationException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/**
 * Date/time formats used by the screens, the database and exported reports.
 * Every value is a {@link LocalDateTime} in the computer's default time zone.
 */
public final class DateTimeUtils {

    public static final String TIME_ZONE_NOTE =
            "All election times use this computer's local time zone.";

    public static final DateTimeFormatter DISPLAY =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.ENGLISH);

    public static final DateTimeFormatter INPUT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ENGLISH);

    private static final DateTimeFormatter INPUT_WITH_SECONDS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);

    private DateTimeUtils() {
    }

    public static String format(LocalDateTime value) {
        return value == null ? "-" : DISPLAY.format(value);
    }

    public static String inputHint() {
        return "yyyy-MM-dd HH:mm (for example 2026-10-30 09:00)";
    }

    public static LocalDateTime parseInput(String text) throws ValidationException {
        String cleaned = text == null ? "" : text.trim();
        if (cleaned.isEmpty()) {
            throw new ValidationException("Please enter a date/time.");
        }
        try {
            return LocalDateTime.parse(cleaned, INPUT);
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDateTime.parse(cleaned, INPUT_WITH_SECONDS);
            } catch (DateTimeParseException secondFailure) {
                throw new ValidationException("Use the format " + inputHint() + ".");
            }
        }
    }

    public static String toStorage(LocalDateTime value) {
        return value.toString();
    }

    public static LocalDateTime fromStorage(String value) {
        return LocalDateTime.parse(value);
    }
}
