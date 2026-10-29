package com.votesecure;

import com.votesecure.database.DatabaseManager;
import com.votesecure.ui.MainFrame;
import com.votesecure.ui.Theme;
import com.votesecure.util.LoggingConfig;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.GraphicsEnvironment;
import java.time.Clock;

/** Application entry point: logging, database initialisation and the Swing window. */
public final class VoteSecureApp {

    private VoteSecureApp() {
    }

    public static void main(String[] args) {
        LoggingConfig.configure(DatabaseManager.createDefault(Clock.systemDefaultZone())
                .getApplicationDataDirectory().resolve("logs"));

        if (GraphicsEnvironment.isHeadless()) {
            System.err.println("VoteSecure needs a graphical desktop environment.");
            System.exit(1);
        }

        try {
            Theme.install();
            DatabaseManager database = DatabaseManager.createDefault(Clock.systemDefaultZone());
            database.initialize();
            AppContext context = AppContext.create(database, Clock.systemDefaultZone());
            SwingUtilities.invokeLater(() -> {
                MainFrame frame = new MainFrame(context);
                frame.setVisible(true);
            });
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    "VoteSecure could not start.\n\n"
                            + "A database or system error occurred. Check that the application-data "
                            + "folder is writable, then try again.\n\n"
                            + "Technical details have been written to the log file.",
                    "VoteSecure", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    static {
        // Use the platform look and feel as a base before Theme.install() adds colours.
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // The custom theme still works with the default look and feel.
        }
    }
}
