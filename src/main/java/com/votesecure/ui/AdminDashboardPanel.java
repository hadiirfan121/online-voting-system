package com.votesecure.ui;

import com.votesecure.AppContext;
import com.votesecure.model.Election;
import com.votesecure.model.ElectionStatus;
import com.votesecure.model.ResultsSummary;
import com.votesecure.util.DateTimeUtils;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.Locale;

/** Administrator home screen with election status, statistics and management links. */
public final class AdminDashboardPanel extends JPanel implements Refreshable {

    private final MainFrame frame;
    private final AppContext context;
    private final JPanel body = new JPanel();
    private final FlatButton refreshButton = UiFactory.secondaryButton("Refresh");

    public AdminDashboardPanel(MainFrame frame, AppContext context) {
        this.frame = frame;
        this.context = context;
        setOpaque(false);
        setLayout(new BorderLayout(0, 20));
        setBorder(new EmptyBorder(24, 28, 24, 28));

        refreshButton.addActionListener(event -> rebuild());
        add(UiFactory.pageHeader("Administrator dashboard",
                "Manage voters, candidates, the election window and reports.",
                refreshButton), BorderLayout.NORTH);

        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        add(body, BorderLayout.CENTER);
        rebuild();
    }

    @Override
    public void onShow() {
        rebuild();
    }

    private void rebuild() {
        body.removeAll();
        try {
            Election election = context.getElectionService().getCurrentElection();
            ElectionStatus status = election.statusAt(context.getElectionService().now());
            ResultsSummary summary = context.getVotingManager().calculateResults();
            int registeredVoters = context.getAdminService().getRegisteredVoterCount();
            String lockMessage = context.getElectionService().configurationLockMessage(election);

            body.add(buildOverview(election, status, lockMessage));
            body.add(Box.createVerticalStrut(16));
            body.add(buildStats(summary, registeredVoters));
            body.add(Box.createVerticalStrut(16));
            body.add(buildActions());
            body.add(Box.createVerticalStrut(8));
            JLabel database = UiFactory.smallMuted("Local database: " + context.getDatabase().getDatabaseFile());
            database.setAlignmentX(Component.LEFT_ALIGNMENT);
            body.add(database);
        } catch (Exception ex) {
            body.add(UiFactory.infoBox(UiFactory.safeMessage(ex), Theme.DANGER, Theme.DANGER_LIGHT, 900));
        }
        body.revalidate();
        body.repaint();
    }

    private JPanel buildOverview(Election election, ElectionStatus status, String lockMessage) {
        CardPanel card = new CardPanel();
        card.setLayout(new BorderLayout(20, 0));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        titleRow.setOpaque(false);
        JLabel title = new JLabel(election.getElectionName());
        title.setFont(Theme.H2);
        title.setForeground(Theme.TEXT);
        titleRow.add(title);
        titleRow.add(UiFactory.badge(status.getDisplayName(),
                Theme.statusColor(status), Theme.statusBackground(status)));
        titleRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel window = new JLabel(DateTimeUtils.format(election.getStartDateTime())
                + "   to   " + DateTimeUtils.format(election.getEndDateTime()));
        window.setFont(Theme.BODY);
        window.setForeground(Theme.MUTED);
        window.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel lock = UiFactory.wrappedLabel(lockMessage, 720,
                lockMessage.startsWith("Election configuration is locked") ? Theme.WARNING : Theme.MUTED,
                Theme.SMALL);
        lock.setAlignmentX(Component.LEFT_ALIGNMENT);
        text.add(titleRow);
        text.add(Box.createVerticalStrut(6));
        text.add(window);
        text.add(Box.createVerticalStrut(6));
        text.add(lock);
        card.add(text, BorderLayout.CENTER);

        FlatButton settings = UiFactory.secondaryButton("Election settings");
        settings.addActionListener(event -> frame.showElectionSettings());
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        right.setOpaque(false);
        right.add(settings);
        card.add(right, BorderLayout.EAST);
        return card;
    }

    private JPanel buildStats(ResultsSummary summary, int registeredVoters) {
        JPanel row = new JPanel(new GridLayout(1, 4, 16, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));
        row.add(UiFactory.statCard("Registered voters", Integer.toString(registeredVoters),
                "all voter records", Theme.PRIMARY_DARK));
        row.add(UiFactory.statCard("Candidates", Integer.toString(summary.getCandidateResults().size()),
                "on the ballot", Theme.PRIMARY));
        row.add(UiFactory.statCard("Votes cast", Integer.toString(summary.getVotesCast()),
                "valid ballots", Theme.PRIMARY));
        row.add(UiFactory.statCard("Turnout", String.format(Locale.ENGLISH, "%.1f%%", summary.getTurnoutPercentage()),
                "of eligible voters", Theme.PRIMARY_DARK));
        return row;
    }

    private JPanel buildActions() {
        CardPanel card = new CardPanel();
        card.setLayout(new BorderLayout(0, 14));
        JLabel title = UiFactory.heading("Manage the election");
        card.add(title, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(2, 3, 12, 12));
        grid.setOpaque(false);
        grid.add(actionButton("Voter management", "Register and search voters", frame::showVoterManagement));
        grid.add(actionButton("Candidate management", "Add, edit or remove candidates", frame::showCandidateManagement));
        grid.add(actionButton("Election settings", "Name and voting window", frame::showElectionSettings));
        grid.add(actionButton("Live results", "Administrator preview of counts", () -> frame.showResults(true)));
        grid.add(actionButton("Export reports", "Save TXT and CSV result reports", frame::showExportReports));
        grid.add(actionButton("Change password", "Update the demo administrator password",
                () -> new ChangePasswordDialog(frame, context).setVisible(true)));
        card.add(grid, BorderLayout.CENTER);
        return card;
    }

    private FlatButton actionButton(String title, String description, Runnable action) {
        FlatButton button = UiFactory.secondaryButton(title);
        button.setToolTipText(description);
        button.setFont(Theme.BODY);
        button.addActionListener(event -> action.run());
        return button;
    }
}
