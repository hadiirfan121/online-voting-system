package com.votesecure.repository;

import java.sql.SQLException;
import java.util.Locale;

/** Small helpers for recognising common SQLite constraint violations. */
final class SqlErrors {

    private SqlErrors() {
    }

    static boolean isUniqueViolation(SQLException exception) {
        String message = safeMessage(exception);
        return message.contains("unique constraint failed") || message.contains("primary key");
    }

    static boolean isForeignKeyViolation(SQLException exception) {
        return safeMessage(exception).contains("foreign key constraint failed");
    }

    private static String safeMessage(SQLException exception) {
        String message = exception.getMessage();
        return message == null ? "" : message.toLowerCase(Locale.ROOT);
    }
}
