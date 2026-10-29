package com.votesecure.ui;

import com.votesecure.AppContext;
import com.votesecure.exception.VoteSecureException;
import com.votesecure.model.Voter;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;

/** Administrator screen for registering, listing and searching voters. */
public final class VoterManagementPanel extends JPanel implements Refreshable {

    private final MainFrame frame;
    private final AppContext context;
    private final JTextField voterIdField = UiFactory.textField(18);
    private final JTextField nameField = UiFactory.textField(18);
    private final JTextField pinField = UiFactory.textField(18);
    private final javax.swing.JCheckBox eligibleBox = UiFactory.checkBox("Eligible to vote", true);
    private final JLabel messageLabel = UiFactory.errorLabel();
    private final JTextField searchField = UiFactory.textField(18);
    private final SimpleTableModel tableModel = new SimpleTableModel("Voter ID", "Name", "Eligible", "Has voted");
    private final JTable table = UiFactory.table(tableModel);
    private final StatusBadge countBadge = UiFactory.badge("0 VOTERS", Theme.PRIMARY_DARK, Theme.PRIMARY_LIGHT);

    public VoterManagementPanel(MainFrame frame, AppContext context) {
        this.frame = frame;
        this.context = context;
        setOpaque(false);
        setLayout(new BorderLayout(0, 20));
        setBorder(new EmptyBorder(24, 28, 24, 28));

        FlatButton back = UiFactory.secondaryButton("Back to dashboard");
        back.addActionListener(event -> frame.showAdminDashboard());
        add(UiFactory.pageHeader("Voter management",
                "Register eligible voters and search the voter roll.", back), BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout(16, 0));
        content.setOpaque(false);
        content.add(buildForm(), BorderLayout.WEST);
        content.add(buildList(), BorderLayout.CENTER);
        add(content, BorderLayout.CENTER);

        searchField.addActionListener(event -> search());
        refreshTable();
    }

    private JPanel buildForm() {
        CardPanel card = new CardPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setPreferredSize(new Dimension(390, 560));

        JLabel title = UiFactory.heading("Register a voter");
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(title);
        card.add(Box.createVerticalStrut(6));
        JLabel note = UiFactory.wrappedLabel(
                "The voter uses this ID and PIN to sign in. The PIN is stored only as a salted hash.",
                330, Theme.MUTED, Theme.SMALL);
        note.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(note);
        card.add(Box.createVerticalStrut(14));

        messageLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(messageLabel);
        card.add(Box.createVerticalStrut(4));

        card.add(UiFactory.labeledField("VOTER ID", voterIdField));
        card.add(Box.createVerticalStrut(12));
        card.add(UiFactory.labeledField("FULL NAME", nameField));
        card.add(Box.createVerticalStrut(12));
        card.add(UiFactory.labeledField("INITIAL PIN (4-12 DIGITS)", pinField));
        card.add(Box.createVerticalStrut(10));
        eligibleBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(eligibleBox);
        card.add(Box.createVerticalStrut(18));

        FlatButton register = UiFactory.primaryButton("Register voter");
        UiFactory.makeFullWidth(register);
        register.addActionListener(event -> registerVoter());
        card.add(register);
        card.add(Box.createVerticalStrut(14));

        card.add(UiFactory.infoBox(
                "Voter IDs are unique. A duplicate ID is rejected by the database, not just by this form.",
                Theme.MUTED, new java.awt.Color(247, 249, 252), 330));
        return card;
    }

    private JPanel buildList() {
        CardPanel card = new CardPanel();
        card.setLayout(new BorderLayout(0, 12));

        JPanel top = new JPanel(new BorderLayout(12, 8));
        top.setOpaque(false);
        JPanel heading = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        heading.setOpaque(false);
        heading.add(UiFactory.heading("Registered voters"));
        heading.add(countBadge);
        top.add(heading, BorderLayout.NORTH);

        JPanel search = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        search.setOpaque(false);
        FlatButton searchButton = UiFactory.secondaryButton("Search");
        searchButton.addActionListener(event -> search());
        FlatButton clearButton = UiFactory.ghostButton("Clear");
        clearButton.addActionListener(event -> {
            searchField.setText("");
            refreshTable();
        });
        search.add(UiFactory.fieldLabel("SEARCH BY ID OR NAME"));
        search.add(searchField);
        search.add(searchButton);
        search.add(clearButton);
        top.add(search, BorderLayout.CENTER);
        card.add(top, BorderLayout.NORTH);

        card.add(UiFactory.tableScroll(table), BorderLayout.CENTER);
        return card;
    }

    private void registerVoter() {
        messageLabel.setForeground(Theme.DANGER);
        messageLabel.setText(" ");
        String voterId = voterIdField.getText();
        String name = nameField.getText();
        String pin = pinField.getText();
        if (voterId.isBlank() || name.isBlank() || pin.isBlank()) {
            messageLabel.setText("Please complete the voter ID, name and PIN.");
            return;
        }
        try {
            Voter voter = context.getAdminService()
                    .registerVoter(voterId, name, pin, eligibleBox.isSelected());
            messageLabel.setForeground(Theme.SUCCESS);
            messageLabel.setText("Voter " + voter.getVoterId() + " registered successfully.");
            voterIdField.setText("");
            nameField.setText("");
            pinField.setText("");
            refreshTable();
        } catch (VoteSecureException ex) {
            messageLabel.setForeground(Theme.DANGER);
            messageLabel.setText(ex.getMessage());
        } catch (RuntimeException ex) {
            messageLabel.setForeground(Theme.DANGER);
            messageLabel.setText(UiFactory.safeMessage(ex));
        }
    }

    private void search() {
        try {
            String query = searchField.getText();
            List<Voter> voters = context.getAdminService().searchVoters(query);
            fillTable(voters);
        } catch (Exception ex) {
            UiFactory.showError(this, UiFactory.safeMessage(ex));
        }
    }

    private void refreshTable() {
        try {
            fillTable(context.getAdminService().listVoters());
        } catch (Exception ex) {
            messageLabel.setForeground(Theme.DANGER);
            messageLabel.setText(UiFactory.safeMessage(ex));
        }
    }

    private void fillTable(List<Voter> voters) {
        List<Object[]> rows = new ArrayList<>();
        for (Voter voter : voters) {
            rows.add(new Object[]{
                    voter.getVoterId(),
                    voter.getName(),
                    voter.isEligible() ? "Yes" : "No",
                    voter.hasVoted() ? "Yes" : "No"
            });
        }
        tableModel.setRows(rows);
        countBadge.setText(voters.size() + (voters.size() == 1 ? " VOTER" : " VOTERS"));
    }

    @Override
    public void onShow() {
        messageLabel.setText(" ");
        refreshTable();
    }
}
