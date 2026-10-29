package com.votesecure.tools;

import com.votesecure.AppContext;
import com.votesecure.database.DatabaseManager;
import com.votesecure.model.Administrator;
import com.votesecure.model.Vote;
import com.votesecure.model.Voter;
import com.votesecure.ui.MainFrame;
import com.votesecure.ui.Theme;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Launches the real Swing application against an isolated demonstration
 * database and captures genuine screenshots with {@link Robot}. No image is
 * faked or edited; the tool only runs on a machine with a graphical desktop.
 */
public final class ScreenshotTool {

    private ScreenshotTool() {
    }

    public static void main(String[] args) throws Exception {
        if (GraphicsEnvironment.isHeadless()) {
            System.out.println("No graphical desktop is available, so screenshots were not captured.");
            return;
        }

        Path projectRoot = Path.of("").toAbsolutePath();
        Path screenshotDirectory = projectRoot.resolve("docs").resolve("screenshots");
        Path dataDirectory = projectRoot.resolve("target").resolve("screenshot-data");
        Files.createDirectories(screenshotDirectory);
        Files.createDirectories(dataDirectory);
        Path databaseFile = dataDirectory.resolve("screenshot-votesecure.db");
        Files.deleteIfExists(databaseFile);
        Files.deleteIfExists(dataDirectory.resolve("screenshot-votesecure.db-wal"));
        Files.deleteIfExists(dataDirectory.resolve("screenshot-votesecure.db-shm"));

        Clock clock = Clock.systemDefaultZone();
        DatabaseManager database = new DatabaseManager(databaseFile, clock);
        database.initialize();
        AppContext context = AppContext.create(database, clock);

        String[][] ballots = {
                {"V001", "C001"}, {"V002", "C001"}, {"V003", "C001"},
                {"V004", "C002"}, {"V005", "C002"}, {"V006", "C003"}
        };
        for (String[] ballot : ballots) {
            context.getVotingManager().castVote(ballot[0], ballot[1]);
        }

        Theme.install();
        AtomicReference<MainFrame> frameReference = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            MainFrame frame = new MainFrame(context);
            frame.setLocation(10, 5);
            frame.setAlwaysOnTop(true);
            frame.setExtendedState(javax.swing.JFrame.MAXIMIZED_BOTH);
            frame.setVisible(true);
            frameReference.set(frame);
        });
        MainFrame frame = frameReference.get();
        Robot robot = new Robot();
        robot.setAutoDelay(60);

        capture(robot, frame, screenshotDirectory, "01_welcome.png");
        onEdt(() -> frame.showVoterLoginScreen());
        capture(robot, frame, screenshotDirectory, "02_voter_login.png");
        onEdt(() -> frame.showAdminLoginScreen());
        capture(robot, frame, screenshotDirectory, "03_admin_login.png");

        Administrator admin = context.getAuthenticationService()
                .authenticateAdministrator("admin", "Admin@123");
        onEdt(() -> frame.onAdministratorAuthenticated(admin));
        capture(robot, frame, screenshotDirectory, "04_admin_dashboard.png");
        onEdt(() -> frame.showVoterManagement());
        capture(robot, frame, screenshotDirectory, "05_voter_management.png");
        onEdt(() -> frame.showCandidateManagement());
        capture(robot, frame, screenshotDirectory, "06_candidate_management.png");
        onEdt(() -> frame.showElectionSettings());
        capture(robot, frame, screenshotDirectory, "07_election_settings.png");
        onEdt(() -> frame.showExportReports());
        capture(robot, frame, screenshotDirectory, "08_export_report.png");
        onEdt(() -> frame.showResults(true));
        capture(robot, frame, screenshotDirectory, "09_results.png");

        onEdt(() -> frame.signOut());
        Voter voter = context.getAuthenticationService().authenticateVoter("V010", "1234");
        onEdt(() -> frame.onVoterAuthenticated(voter));
        capture(robot, frame, screenshotDirectory, "10_voter_dashboard.png");
        onEdt(() -> frame.showVoteScreen());
        capture(robot, frame, screenshotDirectory, "11_ballot.png");

        Vote vote = context.getVotingManager().castVote("V010", "C002");
        onEdt(() -> frame.onVoteRecorded(vote));
        capture(robot, frame, screenshotDirectory, "12_vote_success.png");

        System.out.println("Screenshots captured with AWT Robot in " + screenshotDirectory);
        onEdt(frame::dispose);
        System.exit(0);
    }

    private static void onEdt(Runnable action) throws Exception {
        SwingUtilities.invokeAndWait(action);
        Thread.sleep(350);
    }

    private static void capture(Robot robot, MainFrame frame, Path directory, String fileName)
            throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            frame.toFront();
            frame.requestFocus();
            frame.repaint();
        });
        robot.waitForIdle();
        Thread.sleep(750);
        java.util.concurrent.atomic.AtomicReference<Rectangle> boundsReference =
                new java.util.concurrent.atomic.AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> boundsReference.set(new Rectangle(
                frame.getContentPane().getLocationOnScreen(),
                frame.getContentPane().getSize())));
        Rectangle bounds = boundsReference.get();
        BufferedImage image = robot.createScreenCapture(bounds);
        Path destination = directory.resolve(fileName);
        ImageIO.write(image, "png", destination.toFile());
        System.out.println("Captured " + destination + " (" + image.getWidth() + "x"
                + image.getHeight() + ")");
    }
}
