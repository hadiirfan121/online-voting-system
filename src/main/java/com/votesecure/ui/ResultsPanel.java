package com.votesecure.ui;

import com.votesecure.AppContext;
import com.votesecure.model.CandidateResult;
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
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Public final results and administrator live-result view. */
public final class ResultsPanel extends JPanel implements Refreshable {

    private final MainFrame frame;
    private final AppContext context;
    private final JPanel body = new JPanel();
    private final FlatButton exportButton = UiFactory.secondaryButton("Export results");
    private boolean administratorView;

    public ResultsPanel(MainFrame frame, AppContext context) {
        this.frame = frame;
        this.context = context;
        setOpaque(false);
        setLayout(new BorderLayout(0, 20));
        setBorder(new EmptyBorder(24, 28, 24, 28));

        FlatButton back = UiFactory.secondaryButton("Back");
        back.addActionListener(event -> frame.goBackFromResults());
        FlatButton refresh = UiFactory.secondaryButton("Refresh");
        refresh.addActionListener(event -> rebuild());
        exportButton.addActionListener(event -> frame.showExportReports());

        add(UiFactory.pageHeader("Election results",
                "Counts are calculated from the ballots saved in the database.",
                back, refresh, exportButton), BorderLayout.NORTH);

        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        add(body, BorderLayout.CENTER);
        rebuild();
    }

    public void setAdministratorView(boolean administratorView) {
        this.administratorView = administratorView;
        exportButton.setVisible(administratorView);
        rebuild();
    }

    @Override
    public void onShow() {
        rebuild();
    }

    private void rebuild() {
        body.removeAll();
        exportButton.setVisible(administratorView);
        try {
            Election election = context.getElectionService().getCurrentElection();
            ElectionStatus status = election.statusAt(context.getElectionService().now());

            if (!administratorView && status != ElectionStatus.CLOSED) {
                body.add(buildSealedCard(election, status));
                body.revalidate();
                body.repaint();
                return;
            }

            ResultsSummary summary = context.getVotingManager().calculateResults();
            body.add(buildHeaderCard(summary));
            body.add(Box.createVerticalStrut(16));
            body.add(buildStats(summary));
            body.add(Box.createVerticalStrut(16));
            body.add(buildOutcome(summary));
            body.add(Box.createVerticalStrut(16));
            body.add(buildTable(summary));
            body.add(Box.createVerticalStrut(10));

            JLabel footnote = UiFactory.smallMuted("Generated "
                    + DateTimeUtils.format(summary.getGeneratedAt()) + "  ·  "
                    + DateTimeUtils.TIME_ZONE_NOTE
                    + "  ·  This academic prototype is not an official election system.");
            footnote.setAlignmentX(Component.LEFT_ALIGNMENT);
            body.add(footnote);
        } catch (Exception ex) {
            body.add(UiFactory.infoBox(UiFactory.safeMessage(ex), Theme.DANGER, Theme.DANGER_LIGHT, 900));
        }
        body.revalidate();
        body.repaint();
    }

    private JPanel buildSealedCard(Election election, ElectionStatus status) {
        CardPanel card = new CardPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        JLabel title = UiFactory.heading("Results are sealed");
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(title);
        card.add(Box.createVerticalStrut(10));
        String message = "Public results become available after the election closes on "
                + DateTimeUtils.format(election.getEndDateTime()) + ". "
                + "Administrators may inspect a clearly labelled live preview during the demonstration.";
        JLabel text = UiFactory.wrappedLabel(message, 820, Theme.MUTED, Theme.BODY);
        text.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(text);
        card.add(Box.createVerticalStrut(16));

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        row.setOpaque(false);
        row.add(UiFactory.badge(status.getDisplayName(), Theme.statusColor(status), Theme.statusBackground(status)));
        row.add(UiFactory.badge("Election: " + election.getElectionName(), Theme.PRIMARY_DARK, Theme.PRIMARY_LIGHT));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(row);
        return card;
    }

    private JPanel buildHeaderCard(ResultsSummary summary) {
        CardPanel card = new CardPanel();
        card.setLayout(new BorderLayout(16, 0));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 104));

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        JLabel title = new JLabel(summary.getElection().getElectionName());
        title.setFont(Theme.H2);
        title.setForeground(Theme.TEXT);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel window = new JLabel(DateTimeUtils.format(summary.getElection().getStartDateTime())
                + "   to   " + DateTimeUtils.format(summary.getElection().getEndDateTime()));
        window.setFont(Theme.BODY);
        window.setForeground(Theme.MUTED);
        window.setAlignmentX(Component.LEFT_ALIGNMENT);
        text.add(title);
        text.add(Box.createVerticalStrut(6));
        text.add(window);
        card.add(text, BorderLayout.CENTER);

        JPanel badges = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        badges.setOpaque(false);
        if (administratorView) {
            badges.add(UiFactory.badge("LIVE PREVIEW", Theme.WARNING, Theme.WARNING_LIGHT));
        }
        badges.add(UiFactory.badge(summary.getStatus().getDisplayName(),
                Theme.statusColor(summary.getStatus()), Theme.statusBackground(summary.getStatus())));
        card.add(badges, BorderLayout.EAST);
        return card;
    }

    private JPanel buildStats(ResultsSummary summary) {
        JPanel row = new JPanel(new GridLayout(1, 4, 16, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));
        row.add(UiFactory.statCard("Eligible voters", Integer.toString(summary.getEligibleVoters()),
                "registered and eligible", Theme.PRIMARY_DARK));
        row.add(UiFactory.statCard("Votes cast", Integer.toString(summary.getVotesCast()),
                "valid ballots", Theme.PRIMARY));
        row.add(UiFactory.statCard("Turnout",
                String.format(Locale.ENGLISH, "%.1f%%", summary.getTurnoutPercentage()),
                "of eligible voters", Theme.PRIMARY));
        row.add(UiFactory.statCard("Candidates", Integer.toString(summary.getCandidateResults().size()),
                "on the ballot", Theme.PRIMARY_DARK));
        return row;
    }

    private JPanel buildOutcome(ResultsSummary summary) {
        boolean tie = summary.isTie();
        Color foreground;
        Color background;
        if (!summary.hasVotes()) {
            foreground = Theme.MUTED;
            background = new Color(247, 249, 252);
        } else if (tie) {
            foreground = Theme.WARNING;
            background = Theme.WARNING_LIGHT;
        } else {
            foreground = Theme.SUCCESS;
            background = Theme.SUCCESS_LIGHT;
        }
        JPanel panel = new JPanel(new BorderLayout(16, 0));
        panel.setOpaque(true);
        panel.setBackground(background);
        panel.setBorder(new EmptyBorder(16, 18, 16, 18));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 84));

        JLabel label = UiFactory.fieldLabel(summary.getOutcomeLabel().toUpperCase());
        JLabel outcome = new JLabel(summary.getOutcomeText());
        outcome.setFont(Theme.H2);
        outcome.setForeground(foreground);
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(label);
        text.add(Box.createVerticalStrut(4));
        text.add(outcome);
        panel.add(text, BorderLayout.CENTER);

        String note = summary.isTie()
                ? "Two or more candidates share the highest vote count."
                : summary.hasVotes() ? "Highest number of valid votes." : "No ballots have been recorded yet.";
        JLabel noteLabel = UiFactory.smallMuted(note);
        panel.add(noteLabel, BorderLayout.EAST);
        return panel;
    }

    private JPanel buildTable(ResultsSummary summary) {
        CardPanel card = new CardPanel();
        card.setLayout(new BorderLayout(0, 12));
        JLabel title = UiFactory.heading("Candidate-wise results");
        card.add(title, BorderLayout.NORTH);

        SimpleTableModel model = new SimpleTableModel(
                "Candidate ID", "Candidate", "Political party", "Votes", "% of valid votes");
        List<Object[]> rows = new ArrayList<>();
        for (CandidateResult result : summary.getCandidateResults()) {
            rows.add(new Object[]{
                    result.getCandidate().getCandidateId(),
                    result.getCandidate().getName(),
                    result.getCandidate().getPoliticalParty(),
                    result.getVotes(),
                    String.format(Locale.ENGLISH, "%.1f%%", result.getPercentageOfValidVotes())
            });
        }
        model.setRows(rows);
        javax.swing.JTable table = UiFactory.table(model);
        table.getColumnModel().getColumn(0).setPreferredWidth(110);
        table.getColumnModel().getColumn(1).setPreferredWidth(220);
        table.getColumnModel().getColumn(2).setPreferredWidth(220);
        table.getColumnModel().getColumn(3).setPreferredWidth(90);
        table.getColumnModel().getColumn(4).setPreferredWidth(140);
        card.add(UiFactory.tableScroll(table), BorderLayout.CENTER);
        return card;
    }
}
