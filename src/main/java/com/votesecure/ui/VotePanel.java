package com.votesecure.ui;

import com.votesecure.AppContext;
import com.votesecure.exception.VoteSecureException;
import com.votesecure.model.Candidate;
import com.votesecure.model.Election;
import com.votesecure.model.ElectionStatus;
import com.votesecure.model.Vote;
import com.votesecure.model.Voter;
import com.votesecure.util.DateTimeUtils;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;

/** Candidate list, selection and confirmation before a vote is saved. */
public final class VotePanel extends JPanel implements Refreshable {

    private final MainFrame frame;
    private final AppContext context;
    private final JPanel body = new JPanel();
    private final FlatButton refreshButton = UiFactory.secondaryButton("Refresh");
    private List<Candidate> candidates = new ArrayList<>();

    public VotePanel(MainFrame frame, AppContext context) {
        this.frame = frame;
        this.context = context;
        setOpaque(false);
        setLayout(new BorderLayout(0, 20));
        setBorder(new EmptyBorder(24, 28, 24, 28));

        refreshButton.addActionListener(event -> rebuild());
        FlatButton back = UiFactory.secondaryButton("Back to dashboard");
        back.addActionListener(event -> frame.showVoterDashboard());
        add(UiFactory.pageHeader("Cast your vote", "Select one candidate, review your choice, then confirm.",
                refreshButton, back), BorderLayout.NORTH);

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
        candidates = new ArrayList<>();
        try {
            Voter sessionVoter = frame.getCurrentVoter();
            if (sessionVoter == null) {
                frame.showVoterLoginScreen();
                return;
            }
            Voter voter = context.getVoterRepository().findById(sessionVoter.getVoterId()).orElse(sessionVoter);
            Election election = context.getElectionService().getCurrentElection();
            ElectionStatus status = election.statusAt(context.getElectionService().now());
            boolean alreadyVoted = voter.hasVoted()
                    || context.getVoteRepository().hasVoted(election.getElectionId(), voter.getVoterId());

            if (!voter.isEligible() || status != ElectionStatus.OPEN || alreadyVoted) {
                body.add(buildUnavailableCard(voter, election, status, alreadyVoted));
                return;
            }

            candidates = context.getCandidateRepository().findByElection(election.getElectionId());
            if (candidates.isEmpty()) {
                CardPanel empty = new CardPanel();
                empty.setLayout(new BorderLayout());
                empty.add(UiFactory.infoBox("No candidates have been registered for this election.",
                        Theme.WARNING, Theme.WARNING_LIGHT, 800), BorderLayout.NORTH);
                body.add(empty);
                return;
            }
            body.add(buildBallotCard(voter, election));
        } catch (Exception ex) {
            body.add(UiFactory.infoBox(UiFactory.safeMessage(ex), Theme.DANGER, Theme.DANGER_LIGHT, 900));
        }
        body.revalidate();
        body.repaint();
    }

    private JPanel buildUnavailableCard(Voter voter, Election election, ElectionStatus status, boolean alreadyVoted) {
        CardPanel card = new CardPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        JLabel title = UiFactory.heading("Voting is not available");
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(title);
        card.add(Box.createVerticalStrut(10));

        String message;
        if (alreadyVoted) {
            message = "You have already cast a vote in this election. Each voter may vote only once, and a submitted ballot cannot be changed.";
        } else if (!voter.isEligible()) {
            message = "This voter account is not eligible to vote. Please contact the administrator.";
        } else if (status == ElectionStatus.NOT_STARTED) {
            message = "The election has not started yet. Voting opens on "
                    + DateTimeUtils.format(election.getStartDateTime()) + ".";
        } else {
            message = "The election has closed. Voting ended on "
                    + DateTimeUtils.format(election.getEndDateTime()) + ".";
        }
        JLabel bodyText = UiFactory.wrappedLabel(message, 820, Theme.MUTED, Theme.BODY);
        bodyText.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(bodyText);
        card.add(Box.createVerticalStrut(18));

        FlatButton back = UiFactory.primaryButton("Return to dashboard");
        back.setAlignmentX(Component.LEFT_ALIGNMENT);
        back.addActionListener(event -> frame.showVoterDashboard());
        card.add(back);
        return card;
    }

    private JPanel buildBallotCard(Voter voter, Election election) {
        CardPanel card = new CardPanel();
        card.setLayout(new BorderLayout(0, 14));

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        JLabel title = UiFactory.heading("Candidates for " + election.getElectionName());
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel instruction = UiFactory.wrappedLabel(
                "Select exactly one candidate. You will be asked to confirm before your ballot is saved.",
                900, Theme.MUTED, Theme.BODY);
        instruction.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.add(title);
        top.add(Box.createVerticalStrut(6));
        top.add(instruction);
        card.add(top, BorderLayout.NORTH);

        SimpleTableModel model = new SimpleTableModel("ID", "Candidate", "Political party", "Description");
        List<Object[]> rows = new ArrayList<>();
        for (Candidate candidate : candidates) {
            rows.add(new Object[]{
                    candidate.getCandidateId(),
                    candidate.getName(),
                    candidate.getPoliticalParty(),
                    candidate.getDescription().isBlank() ? "-" : candidate.getDescription()
            });
        }
        model.setRows(rows);
        JTable table = UiFactory.table(model);
        table.setRowHeight(46);
        table.getColumnModel().getColumn(0).setPreferredWidth(60);
        table.getColumnModel().getColumn(1).setPreferredWidth(170);
        table.getColumnModel().getColumn(2).setPreferredWidth(170);
        table.getColumnModel().getColumn(3).setPreferredWidth(430);

        JPanel tableWrapper = new JPanel(new BorderLayout());
        tableWrapper.setOpaque(false);
        tableWrapper.setPreferredSize(new Dimension(900, 260));
        tableWrapper.add(UiFactory.tableScroll(table), BorderLayout.CENTER);
        card.add(tableWrapper, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(16, 0));
        bottom.setOpaque(false);
        JLabel selectedLabel = UiFactory.muted("No candidate selected yet.");
        selectedLabel.setFont(Theme.BODY);
        bottom.add(selectedLabel, BorderLayout.CENTER);

        FlatButton submit = UiFactory.primaryButton("Review and submit vote");
        submit.setEnabled(false);
        submit.addActionListener(event -> submitVote(voter, election, table, model));
        bottom.add(submit, BorderLayout.EAST);

        table.getSelectionModel().addListSelectionListener(event -> {
            if (event.getValueIsAdjusting()) {
                return;
            }
            int row = table.getSelectedRow();
            if (row >= 0 && row < candidates.size()) {
                Candidate candidate = candidates.get(row);
                selectedLabel.setText("Selected: " + candidate.getName()
                        + " (" + candidate.getPoliticalParty() + ")");
                selectedLabel.setForeground(Theme.PRIMARY_DARK);
                submit.setEnabled(true);
            } else {
                selectedLabel.setText("No candidate selected yet.");
                selectedLabel.setForeground(Theme.MUTED);
                submit.setEnabled(false);
            }
        });
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        card.add(bottom, BorderLayout.SOUTH);

        JPanel wrapper = new JPanel(new BorderLayout(0, 14));
        wrapper.setOpaque(false);
        wrapper.add(card, BorderLayout.CENTER);
        wrapper.add(UiFactory.infoBox(
                "Privacy note: the database stores which ballot belongs to which voter so that duplicate voting can be prevented. "
                        + "Voter-facing screens never show this mapping; results are displayed only as totals.",
                Theme.MUTED, new java.awt.Color(247, 249, 252), 900), BorderLayout.SOUTH);
        return wrapper;
    }

    private void submitVote(Voter voter, Election election, JTable table, SimpleTableModel model) {
        int row = table.getSelectedRow();
        if (row < 0 || row >= candidates.size()) {
            UiFactory.showError(this, "Please select a candidate first.");
            return;
        }
        Candidate candidate = candidates.get(row);
        boolean confirmed = UiFactory.confirm(this, "Confirm your vote",
                "Election: " + election.getElectionName() + "\n"
                        + "Candidate: " + candidate.getName() + "\n"
                        + "Political party: " + candidate.getPoliticalParty() + "\n\n"
                        + "Your vote is final and cannot be changed after submission. Submit this ballot now?");
        if (!confirmed) {
            return;
        }
        try {
            Vote vote = context.getVotingManager().castVote(voter.getVoterId(), candidate.getCandidateId());
            frame.onVoteRecorded(vote);
        } catch (VoteSecureException ex) {
            UiFactory.showError(this, ex.getMessage());
            rebuild();
        } catch (RuntimeException ex) {
            UiFactory.showError(this, UiFactory.safeMessage(ex));
            rebuild();
        }
    }
}
