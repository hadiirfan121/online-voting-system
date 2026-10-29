package com.votesecure.ui;

import com.votesecure.AppContext;
import com.votesecure.exception.VoteSecureException;
import com.votesecure.model.Candidate;
import com.votesecure.model.Election;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

/** Administrator screen for adding, editing and deleting candidates before the freeze. */
public final class CandidateManagementPanel extends JPanel implements Refreshable {

    private final MainFrame frame;
    private final AppContext context;
    private final JTextField idField = UiFactory.textField(18);
    private final JTextField nameField = UiFactory.textField(18);
    private final JTextField partyField = UiFactory.textField(18);
    private final JTextArea descriptionArea = UiFactory.textArea(4);
    private final JLabel messageLabel = UiFactory.errorLabel();
    private final JLabel lockLabel = UiFactory.wrappedLabel(" ", 330, Theme.MUTED, Theme.SMALL);
    private final SimpleTableModel tableModel = new SimpleTableModel("ID", "Candidate", "Party", "Votes");
    private final JTable table = UiFactory.table(tableModel);
    private final StatusBadge countBadge = UiFactory.badge("0 CANDIDATES", Theme.PRIMARY_DARK, Theme.PRIMARY_LIGHT);
    private final FlatButton addButton = UiFactory.primaryButton("Add candidate");
    private final FlatButton saveButton = UiFactory.secondaryButton("Save changes");
    private final FlatButton deleteButton = UiFactory.dangerButton("Delete selected");
    private final FlatButton clearButton = UiFactory.ghostButton("Clear");

    private List<Candidate> candidates = new ArrayList<>();
    private boolean editing;
    private boolean configurationLocked;
    private boolean loadingTable;

    public CandidateManagementPanel(MainFrame frame, AppContext context) {
        this.frame = frame;
        this.context = context;
        setOpaque(false);
        setLayout(new BorderLayout(0, 20));
        setBorder(new EmptyBorder(24, 28, 24, 28));

        FlatButton back = UiFactory.secondaryButton("Back to dashboard");
        back.addActionListener(event -> frame.showAdminDashboard());
        add(UiFactory.pageHeader("Candidate management",
                "Prepare the ballot. Changes are frozen once the first vote is recorded.", back),
                BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout(16, 0));
        content.setOpaque(false);
        content.add(buildForm(), BorderLayout.WEST);
        content.add(buildList(), BorderLayout.CENTER);
        add(content, BorderLayout.CENTER);

        addButton.addActionListener(event -> addCandidate());
        saveButton.addActionListener(event -> updateCandidate());
        deleteButton.addActionListener(event -> deleteCandidate());
        clearButton.addActionListener(event -> clearForm());

        table.getSelectionModel().addListSelectionListener(event -> {
            if (event.getValueIsAdjusting() || loadingTable || configurationLocked) {
                return;
            }
            int row = table.getSelectedRow();
            if (row >= 0 && row < candidates.size()) {
                showCandidate(candidates.get(row));
            }
        });
        refreshAll();
    }

    private JPanel buildForm() {
        CardPanel card = new CardPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setPreferredSize(new Dimension(400, 600));

        JLabel title = UiFactory.heading("Candidate details");
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(title);
        card.add(Box.createVerticalStrut(6));
        lockLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(lockLabel);
        card.add(Box.createVerticalStrut(10));

        messageLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(messageLabel);
        card.add(Box.createVerticalStrut(4));

        card.add(UiFactory.labeledField("CANDIDATE ID", idField));
        card.add(Box.createVerticalStrut(10));
        card.add(UiFactory.labeledField("FULL NAME", nameField));
        card.add(Box.createVerticalStrut(10));
        card.add(UiFactory.labeledField("POLITICAL PARTY", partyField));
        card.add(Box.createVerticalStrut(10));

        JPanel description = new JPanel();
        description.setOpaque(false);
        description.setLayout(new BoxLayout(description, BoxLayout.Y_AXIS));
        JLabel descriptionLabel = UiFactory.fieldLabel("DESCRIPTION (OPTIONAL)");
        descriptionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JScrollPane scroll = new JScrollPane(descriptionArea);
        scroll.setBorder(new javax.swing.border.LineBorder(Theme.BORDER));
        scroll.setPreferredSize(new Dimension(340, 90));
        scroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        description.add(descriptionLabel);
        description.add(Box.createVerticalStrut(6));
        description.add(scroll);
        description.setAlignmentX(Component.LEFT_ALIGNMENT);
        description.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        card.add(description);
        card.add(Box.createVerticalStrut(18));

        JPanel buttons = new JPanel(new GridLayout(2, 2, 8, 8));
        buttons.setOpaque(false);
        buttons.add(addButton);
        buttons.add(saveButton);
        buttons.add(deleteButton);
        buttons.add(clearButton);
        buttons.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttons.setMaximumSize(new Dimension(Integer.MAX_VALUE, 92));
        card.add(buttons);
        card.add(Box.createVerticalStrut(14));

        JLabel hint = UiFactory.wrappedLabel(
                "Candidate IDs are unique. A candidate with recorded votes cannot be deleted, "
                        + "and the ballot is frozen after the first vote.",
                330, Theme.MUTED, Theme.SMALL);
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(hint);
        return card;
    }

    private JPanel buildList() {
        CardPanel card = new CardPanel();
        card.setLayout(new BorderLayout(0, 12));
        JPanel heading = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        heading.setOpaque(false);
        heading.add(UiFactory.heading("Candidates on the ballot"));
        heading.add(countBadge);
        card.add(heading, BorderLayout.NORTH);
        card.add(UiFactory.tableScroll(table), BorderLayout.CENTER);

        JLabel footer = UiFactory.smallMuted("Select a row to edit the candidate details.");
        card.add(footer, BorderLayout.SOUTH);
        return card;
    }

    private void refreshAll() {
        messageLabel.setText(" ");
        try {
            Election election = context.getElectionService().getCurrentElection();
            configurationLocked = context.getElectionService().isConfigurationLocked(election);
            if (configurationLocked) {
                lockLabel.setText(context.getElectionService().configurationLockMessage(election));
                lockLabel.setForeground(Theme.WARNING);
            } else {
                lockLabel.setText("Configuration is editable until the first vote is recorded.");
                lockLabel.setForeground(Theme.MUTED);
            }
            reloadTable();
            clearForm();
            applyLockState();
        } catch (Exception ex) {
            messageLabel.setForeground(Theme.DANGER);
            messageLabel.setText(UiFactory.safeMessage(ex));
        }
    }

    private void reloadTable() {
        try {
            Election election = context.getElectionService().getCurrentElection();
            candidates = context.getCandidateRepository().findByElection(election.getElectionId());
            List<Object[]> rows = new ArrayList<>();
            int totalVotes = 0;
            for (Candidate candidate : candidates) {
                int votes = context.getVoteRepository().countByCandidate(candidate.getCandidateId());
                totalVotes += votes;
                rows.add(new Object[]{
                        candidate.getCandidateId(),
                        candidate.getName(),
                        candidate.getPoliticalParty(),
                        votes
                });
            }
            loadingTable = true;
            table.clearSelection();
            tableModel.setRows(rows);
            loadingTable = false;
            countBadge.setText(candidates.size() + (candidates.size() == 1 ? " CANDIDATE" : " CANDIDATES"));
            if (totalVotes > 0) {
                configurationLocked = true;
                lockLabel.setText("At least one vote has been recorded; the ballot is locked.");
                lockLabel.setForeground(Theme.WARNING);
                applyLockState();
            }
        } catch (Exception ex) {
            loadingTable = false;
            messageLabel.setForeground(Theme.DANGER);
            messageLabel.setText(UiFactory.safeMessage(ex));
        }
    }

    private void applyLockState() {
        idField.setEnabled(!configurationLocked && !editing);
        nameField.setEnabled(!configurationLocked);
        partyField.setEnabled(!configurationLocked);
        descriptionArea.setEnabled(!configurationLocked);
        addButton.setEnabled(!configurationLocked && !editing);
        saveButton.setEnabled(!configurationLocked && editing);
        deleteButton.setEnabled(!configurationLocked && editing);
        clearButton.setEnabled(true);
    }

    private void showCandidate(Candidate candidate) {
        editing = true;
        idField.setText(candidate.getCandidateId());
        nameField.setText(candidate.getName());
        partyField.setText(candidate.getPoliticalParty());
        descriptionArea.setText(candidate.getDescription());
        messageLabel.setForeground(Theme.MUTED);
        messageLabel.setText("Editing " + candidate.getCandidateId() + ".");
        applyLockState();
    }

    private void addCandidate() {
        messageLabel.setForeground(Theme.DANGER);
        messageLabel.setText(" ");
        try {
            Candidate candidate = context.getAdminService().addCandidate(
                    idField.getText(), nameField.getText(), partyField.getText(), descriptionArea.getText());
            messageLabel.setForeground(Theme.SUCCESS);
            messageLabel.setText("Candidate " + candidate.getCandidateId() + " added.");
            reloadTable();
            clearForm();
        } catch (VoteSecureException ex) {
            messageLabel.setText(ex.getMessage());
        } catch (RuntimeException ex) {
            messageLabel.setText(UiFactory.safeMessage(ex));
        }
    }

    private void updateCandidate() {
        if (!editing) {
            return;
        }
        messageLabel.setForeground(Theme.DANGER);
        messageLabel.setText(" ");
        try {
            Candidate candidate = context.getAdminService().updateCandidate(
                    idField.getText(), nameField.getText(), partyField.getText(), descriptionArea.getText());
            messageLabel.setForeground(Theme.SUCCESS);
            messageLabel.setText("Candidate " + candidate.getCandidateId() + " updated.");
            reloadTable();
            clearForm();
        } catch (VoteSecureException ex) {
            messageLabel.setText(ex.getMessage());
        } catch (RuntimeException ex) {
            messageLabel.setText(UiFactory.safeMessage(ex));
        }
    }

    private void deleteCandidate() {
        if (!editing) {
            return;
        }
        String candidateId = idField.getText();
        if (!UiFactory.confirm(this, "Delete candidate",
                "Delete candidate " + candidateId + " from the ballot?\n\n"
                        + "This is only allowed before any vote has been recorded.")) {
            return;
        }
        messageLabel.setForeground(Theme.DANGER);
        messageLabel.setText(" ");
        try {
            context.getAdminService().deleteCandidate(candidateId);
            messageLabel.setForeground(Theme.SUCCESS);
            messageLabel.setText("Candidate " + candidateId + " deleted.");
            reloadTable();
            clearForm();
        } catch (VoteSecureException ex) {
            messageLabel.setText(ex.getMessage());
        } catch (RuntimeException ex) {
            messageLabel.setText(UiFactory.safeMessage(ex));
        }
    }

    private void clearForm() {
        editing = false;
        idField.setText("");
        nameField.setText("");
        partyField.setText("");
        descriptionArea.setText("");
        table.clearSelection();
        applyLockState();
    }

    @Override
    public void onShow() {
        refreshAll();
    }
}
