package com.votesecure.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/** Configures a small rotating diagnostic log in the application-data folder. */
public final class LoggingConfig {

    private static final Logger APP_LOGGER = Logger.getLogger("com.votesecure");

    private LoggingConfig() {
    }

    public static void configure(Path logDirectory) {
        try {
            Files.createDirectories(logDirectory);
            if (APP_LOGGER.getHandlers().length > 0) {
                return;
            }
            FileHandler handler = new FileHandler(
                    logDirectory.resolve("votesecure-%g.log").toString(),
                    1_000_000, 3, true);
            handler.setFormatter(new SimpleFormatter());
            handler.setLevel(Level.INFO);
            APP_LOGGER.addHandler(handler);
            APP_LOGGER.setLevel(Level.INFO);
            APP_LOGGER.setUseParentHandlers(false);
        } catch (IOException ex) {
            System.err.println("VoteSecure diagnostic logging could not be started: " + ex.getMessage());
        }
    }
}
