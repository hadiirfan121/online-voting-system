package com.votesecure.ui;

import com.votesecure.AppContext;
import com.votesecure.exception.VoteSecureException;
import com.votesecure.model.Election;
import com.votesecure.model.ElectionStatus;
import com.votesecure.util.DateTimeUtils;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.time.LocalDateTime;

/** Administrator screen for viewing and editing the election schedule. */
public final class ElectionSettingsPanel extends JPanel implements Refreshable {

    private final MainFrame frame;
    private final AppContext context;
    private final JTextField nameField = UiFactory.textField(24);
    private final JTextField startField = UiFactory.textField(24);
    private final JTextField endField = UiFactory.textField(24);
    private final JLabel messageLabel = UiFactory.errorLabel();
    private final JPanel statusHolder = new JPanel(new BorderLayout());
    private final JLabel lockLabel = UiFactory.wrappedLabel(" ", 560, Theme.MUTED, Theme.SMALL);
    private final JLabel currentWindowLabel = UiFactory.wrappedLabel(" ", 560, Theme.TEXT, Theme.BODY);
    private final JLabel clockLabel = UiFactory.smallMuted(" ");
    private final FlatButton saveButton = UiFactory.primaryButton("Save election settings");
    private final FlatButton resetWindowButton = UiFactory.secondaryButton("Open now for 7 days");

    private Election election;

    public ElectionSettingsPanel(MainFrame frame, AppContext context) {
        this.frame = frame;
        this.context = context;
        setOpaque(false);
        setLayout(new BorderLayout(0, 20));
        setBorder(new EmptyBorder(24, 28, 24, 28));

        FlatButton back = UiFactory.secondaryButton("Back to dashboard");
        back.addActionListener(event -> frame.showAdminDashboard());
        add(UiFactory.pageHeader("Election settings",
                "Set the voting window and review the current election status.", back),
                BorderLayout.NORTH);

        JPanel columns = new JPanel(new GridLayout(1, 2, 18, 0));
        columns.setOpaque(false);
        columns.add(buildStatusCard());
        columns.add(buildFormCard());
        add(columns, BorderLayout.CENTER);

        saveButton.addActionListener(event -> saveSettings());
        resetWindowButton.addActionListener(event -> openNow());
        refreshAll();
    }

    private JPanel buildStatusCard() {
        CardPanel card = new CardPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel title = UiFactory.heading("Current election");
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(title);
        card.add(Box.createVerticalStrut(10));

        statusHolder.setOpaque(false);
        statusHolder.setAlignmentX(Component.LEFT_ALIGNMENT);
        statusHolder.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        card.add(statusHolder);
        card.add(Box.createVerticalStrut(16));

        currentWindowLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(currentWindowLabel);
        card.add(Box.createVerticalStrut(14));

        clockLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(clockLabel);
        card.add(Box.createVerticalStrut(16));

        JPanel rule = UiFactory.infoBox(
                "Voting is allowed only when the current time is at or after the start time "
                        + "and strictly before the end time. " + DateTimeUtils.TIME_ZONE_NOTE,
                Theme.PRIMARY_DARK, Theme.PRIMARY_LIGHT, 430);
        rule.setAlignmentX(Component.LEFT_ALIGNMENT);
        rule.setMaximumSize(new Dimension(Integer.MAX_VALUE, rule.getPreferredSize().height + 8));
        card.add(rule);
        card.add(Box.createVerticalStrut(16));

        lockLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(lockLabel);
        card.add(Box.createVerticalGlue());

        JLabel footnote = UiFactory.wrappedLabel(
                "The schedule and candidate list are frozen once the first vote is recorded "
                        + "or after the election closes, so a result cannot be altered retroactively.",
                480, Theme.MUTED, Theme.SMALL);
        footnote.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(footnote);
        return card;
    }

    private JPanel buildFormCard() {
        CardPanel card = new CardPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel title = UiFactory.heading("Schedule");
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(title);
        card.add(Box.createVerticalStrut(6));

        JLabel hint = UiFactory.wrappedLabel(
                "Enter times as " + DateTimeUtils.inputHint() + ".", 500, Theme.MUTED, Theme.SMALL);
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(hint);
        card.add(Box.createVerticalStrut(16));

        messageLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(messageLabel);
        card.add(Box.createVerticalStrut(4));

        card.add(UiFactory.labeledField("ELECTION NAME", nameField));
        card.add(Box.createVerticalStrut(12));
        card.add(UiFactory.labeledField("VOTING STARTS", startField));
        card.add(Box.createVerticalStrut(12));
        card.add(UiFactory.labeledField("VOTING ENDS", endField));
        card.add(Box.createVerticalStrut(20));

        JPanel buttons = new JPanel(new GridLayout(2, 1, 8, 8));
        buttons.setOpaque(false);
        buttons.add(saveButton);
        buttons.add(resetWindowButton);
        buttons.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttons.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        card.add(buttons);
        card.add(Box.createVerticalStrut(14));

        JLabel note = UiFactory.wrappedLabel(
                "\"Open now for 7 days\" is a demonstration shortcut. It sets the start to the "
                        + "current local time and the end seven days later, and is available only "
                        + "while the configuration is unlocked.",
                480, Theme.MUTED, Theme.SMALL);
        note.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(note);
        card.add(Box.createVerticalGlue());
        return card;
    }

    @Override
    public void onShow() {
        refreshAll();
    }

    private void refreshAll() {
        messageLabel.setForeground(Theme.DANGER);
        messageLabel.setText(" ");
        try {
            election = context.getElectionService().getCurrentElection();
            LocalDateTime now = context.getElectionService().now();
            ElectionStatus status = context.getElectionService().getStatus(election);

            statusHolder.removeAll();
            statusHolder.add(badgeFor(status), BorderLayout.WEST);

            currentWindowLabel.setText("<html><body style='width:440px'>"
                    + "<b>" + escape(election.getElectionName()) + "</b><br>"
                    + "Election ID: " + escape(election.getElectionId()) + "<br>"
                    + "Starts: " + DateTimeUtils.format(election.getStartDateTime()) + "<br>"
                    + "Ends: " + DateTimeUtils.format(election.getEndDateTime())
                    + "</body></html>");
            clockLabel.setText("Current local time: " + DateTimeUtils.format(now));

            nameField.setText(election.getElectionName());
            startField.setText(DateTimeUtils.INPUT.format(election.getStartDateTime()));
            endField.setText(DateTimeUtils.INPUT.format(election.getEndDateTime()));

            boolean locked = context.getElectionService().isConfigurationLocked(election);
            if (locked) {
                lockLabel.setText(context.getElectionService().configurationLockMessage(election));
                lockLabel.setForeground(Theme.WARNING);
            } else {
                lockLabel.setText("Configuration is editable until the first vote is recorded.");
                lockLabel.setForeground(Theme.MUTED);
            }
            nameField.setEnabled(!locked);
            startField.setEnabled(!locked);
            endField.setEnabled(!locked);
            saveButton.setEnabled(!locked);
            resetWindowButton.setEnabled(!locked);
        } catch (Exception ex) {
            messageLabel.setText(UiFactory.safeMessage(ex));
        }
        revalidate();
        repaint();
    }

    private StatusBadge badgeFor(ElectionStatus status) {
        switch (status) {
            case OPEN:
                return UiFactory.badge(status.getDisplayName().toUpperCase(), Theme.SUCCESS, Theme.SUCCESS_LIGHT);
            case NOT_STARTED:
                return UiFactory.badge(status.getDisplayName().toUpperCase(), Theme.WARNING, Theme.WARNING_LIGHT);
            default:
                return UiFactory.badge(status.getDisplayName().toUpperCase(), Theme.MUTED, Theme.BACKGROUND);
        }
    }

    private void saveSettings() {
        messageLabel.setText(" ");
        try {
            LocalDateTime start = DateTimeUtils.parseInput(startField.getText());
            LocalDateTime end = DateTimeUtils.parseInput(endField.getText());
            Election updated = context.getAdminService()
                    .updateElectionSettings(nameField.getText(), start, end);
            refreshAll();
            messageLabel.setForeground(Theme.SUCCESS);
            messageLabel.setText("Election settings saved. Status: "
                    + context.getElectionService().getStatus(updated).getDisplayName() + ".");
        } catch (VoteSecureException ex) {
            messageLabel.setForeground(Theme.DANGER);
            messageLabel.setText(ex.getMessage());
        } catch (RuntimeException ex) {
            messageLabel.setForeground(Theme.DANGER);
            messageLabel.setText(UiFactory.safeMessage(ex));
        }
    }

    private void openNow() {
        if (!UiFactory.confirm(this, "Open election now",
                "Set the voting window to start now and end in seven days?\n\n"
                        + "This changes the schedule of the current demonstration election.")) {
            return;
        }
        messageLabel.setText(" ");
        try {
            LocalDateTime now = context.getElectionService().now();
            context.getAdminService().updateElectionSettings(
                    nameField.getText(), now, now.plusDays(7));
            refreshAll();
            messageLabel.setForeground(Theme.SUCCESS);
            messageLabel.setText("Voting is now open for seven days.");
        } catch (VoteSecureException ex) {
            messageLabel.setForeground(Theme.DANGER);
            messageLabel.setText(ex.getMessage());
        } catch (RuntimeException ex) {
            messageLabel.setForeground(Theme.DANGER);
            messageLabel.setText(UiFactory.safeMessage(ex));
        }
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
