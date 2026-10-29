package com.votesecure.ui;

import com.votesecure.AppContext;
import com.votesecure.model.Election;
import com.votesecure.model.ElectionStatus;
import com.votesecure.model.ResultsSummary;
import com.votesecure.model.Voter;
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

/** Voter home screen showing eligibility, election state and the ballot action. */
public final class VoterDashboardPanel extends JPanel implements Refreshable {

    private final MainFrame frame;
    private final AppContext context;
    private final JPanel body = new JPanel();
    private final FlatButton refreshButton = UiFactory.secondaryButton("Refresh");

    public VoterDashboardPanel(MainFrame frame, AppContext context) {
        this.frame = frame;
        this.context = context;
        setOpaque(false);
        setLayout(new BorderLayout(0, 20));
        setBorder(new EmptyBorder(24, 28, 24, 28));

        refreshButton.addActionListener(event -> rebuild());
        FlatButton resultsButton = UiFactory.secondaryButton("View results");
        resultsButton.addActionListener(event -> frame.showResults(false));

        add(UiFactory.pageHeader("Voter dashboard", "Your ballot, your voice.",
                refreshButton, resultsButton), BorderLayout.NORTH);

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
            Voter voter = frame.getCurrentVoter();
            if (voter == null) {
                frame.showVoterLoginScreen();
                return;
            }
            Voter fresh = context.getVoterRepository().findById(voter.getVoterId()).orElse(voter);
            Election election = context.getElectionService().getCurrentElection();
            ElectionStatus status = election.statusAt(context.getElectionService().now());
            ResultsSummary summary = context.getVotingManager().calculateResults();

            body.add(buildWelcome(fresh));
            body.add(Box.createVerticalStrut(16));
            body.add(buildStats(status, summary));
            body.add(Box.createVerticalStrut(16));
            body.add(buildActions(fresh, election, status));
            body.add(Box.createVerticalStrut(8));
        } catch (Exception ex) {
            body.add(UiFactory.infoBox(UiFactory.safeMessage(ex), Theme.DANGER, Theme.DANGER_LIGHT, 900));
        }
        body.revalidate();
        body.repaint();
    }

    private JPanel buildWelcome(Voter voter) {
        CardPanel card = new CardPanel();
        card.setLayout(new BorderLayout(20, 0));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        JLabel greeting = new JLabel("Welcome, " + voter.getName());
        greeting.setFont(Theme.H2);
        greeting.setForeground(Theme.TEXT);
        greeting.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel details = new JLabel("Voter ID " + voter.getVoterId()
                + "   ·   " + (voter.isEligible() ? "Eligible to vote" : "Not eligible to vote"));
        details.setFont(Theme.BODY);
        details.setForeground(Theme.MUTED);
        details.setAlignmentX(Component.LEFT_ALIGNMENT);
        text.add(greeting);
        text.add(Box.createVerticalStrut(6));
        text.add(details);
        card.add(text, BorderLayout.CENTER);

        StatusBadge ballotBadge = voter.hasVoted()
                ? UiFactory.badge("VOTE RECORDED", Theme.SUCCESS, Theme.SUCCESS_LIGHT)
                : UiFactory.badge("NOT VOTED YET", Theme.MUTED, new java.awt.Color(238, 242, 247));
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        right.setOpaque(false);
        right.add(ballotBadge);
        card.add(right, BorderLayout.EAST);
        return card;
    }

    private JPanel buildStats(ElectionStatus status, ResultsSummary summary) {
        JPanel row = new JPanel(new GridLayout(1, 3, 16, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));
        row.add(UiFactory.statCard("Election status", status.getDisplayName(),
                summary.getElection().getElectionName(), Theme.statusColor(status)));
        row.add(UiFactory.statCard("Votes cast", Integer.toString(summary.getVotesCast()),
                "valid ballots saved", Theme.PRIMARY));
        row.add(UiFactory.statCard("Turnout", String.format(Locale.ENGLISH, "%.1f%%", summary.getTurnoutPercentage()),
                "of eligible voters", Theme.PRIMARY_DARK));
        return row;
    }

    private JPanel buildActions(Voter voter, Election election, ElectionStatus status) {
        JPanel row = new JPanel(new GridLayout(1, 2, 16, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 290));

        CardPanel ballot = new CardPanel();
        ballot.setLayout(new BoxLayout(ballot, BoxLayout.Y_AXIS));
        JLabel ballotTitle = UiFactory.heading("Your ballot");
        ballotTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        ballot.add(ballotTitle);
        ballot.add(Box.createVerticalStrut(8));

        String message;
        boolean canVote = voter.isEligible() && status == ElectionStatus.OPEN && !voter.hasVoted();
        if (!voter.isEligible()) {
            message = "Your account is not eligible for this election. Please contact the administrator.";
        } else if (voter.hasVoted()) {
            message = "Your vote has been recorded successfully. Each voter may vote only once in an election.";
        } else if (status == ElectionStatus.NOT_STARTED) {
            message = "Voting has not started yet. It opens on "
                    + DateTimeUtils.format(election.getStartDateTime()) + ".";
        } else if (status == ElectionStatus.CLOSED) {
            message = "Voting has closed. The election ended on "
                    + DateTimeUtils.format(election.getEndDateTime()) + ".";
        } else {
            message = "The election is open. Review the candidate list, select one candidate and confirm your vote.";
        }
        JLabel bodyText = UiFactory.wrappedLabel(message, 460, Theme.MUTED, Theme.BODY);
        bodyText.setAlignmentX(Component.LEFT_ALIGNMENT);
        ballot.add(bodyText);
        ballot.add(Box.createVerticalStrut(18));

        FlatButton castButton = canVote
                ? UiFactory.primaryButton("Cast your vote")
                : UiFactory.secondaryButton(voter.hasVoted() ? "Vote already recorded" : "Voting unavailable");
        castButton.setEnabled(canVote);
        UiFactory.makeFullWidth(castButton);
        castButton.addActionListener(event -> frame.showVoteScreen());
        ballot.add(castButton);
        ballot.add(Box.createVerticalStrut(10));
        JLabel privacy = UiFactory.wrappedLabel(
                "Your candidate choice is not shown next to your name on any results screen.",
                460, Theme.MUTED, Theme.SMALL);
        privacy.setAlignmentX(Component.LEFT_ALIGNMENT);
        ballot.add(privacy);

        CardPanel details = new CardPanel();
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
        JLabel detailsTitle = UiFactory.heading("Election details");
        detailsTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        details.add(detailsTitle);
        details.add(Box.createVerticalStrut(10));
        details.add(line("Election", election.getElectionName()));
        details.add(Box.createVerticalStrut(8));
        details.add(line("Voting opens", DateTimeUtils.format(election.getStartDateTime())));
        details.add(Box.createVerticalStrut(8));
        details.add(line("Voting closes", DateTimeUtils.format(election.getEndDateTime())));
        details.add(Box.createVerticalStrut(12));
        details.add(UiFactory.infoBox(DateTimeUtils.TIME_ZONE_NOTE,
                Theme.PRIMARY_DARK, Theme.PRIMARY_LIGHT, 400));
        details.add(Box.createVerticalStrut(14));
        FlatButton resultButton = UiFactory.secondaryButton("View election results");
        UiFactory.makeFullWidth(resultButton);
        resultButton.addActionListener(event -> frame.showResults(false));
        details.add(resultButton);

        row.add(ballot);
        row.add(details);
        return row;
    }

    private JPanel line(String label, String value) {
        JPanel panel = new JPanel(new BorderLayout(12, 0));
        panel.setOpaque(false);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        JLabel labelView = UiFactory.fieldLabel(label.toUpperCase());
        JLabel valueView = UiFactory.body(value);
        panel.add(labelView, BorderLayout.WEST);
        panel.add(valueView, BorderLayout.EAST);
        return panel;
    }
}
