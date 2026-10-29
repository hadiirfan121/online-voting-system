package com.votesecure.ui;

import com.votesecure.AppContext;
import com.votesecure.exception.VoteSecureException;
import com.votesecure.model.Administrator;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;

/** Modal dialog that lets the signed-in administrator change the demo password. */
public final class ChangePasswordDialog extends JDialog {

    private final AppContext context;
    private final MainFrame frame;
    private final JPasswordField currentField = UiFactory.passwordField(18);
    private final JPasswordField newField = UiFactory.passwordField(18);
    private final JPasswordField confirmField = UiFactory.passwordField(18);
    private final JLabel errorLabel = UiFactory.errorLabel();
    private final FlatButton updateButton = UiFactory.primaryButton("Update password");

    public ChangePasswordDialog(MainFrame frame, AppContext context) {
        super(frame, "Change administrator password", true);
        this.frame = frame;
        this.context = context;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BACKGROUND);
        root.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JLabel title = UiFactory.heading("Change administrator password");
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(title);
        content.add(strut(6));
        JLabel note = UiFactory.wrappedLabel(
                "Use at least 8 characters with uppercase, lowercase, a number and a special character. "
                        + "The new password is stored as a salted PBKDF2 hash.",
                360, Theme.MUTED, Theme.SMALL);
        note.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(note);
        content.add(strut(16));

        errorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(errorLabel);
        content.add(strut(4));

        content.add(UiFactory.labeledField("CURRENT PASSWORD", currentField));
        content.add(strut(12));
        content.add(UiFactory.labeledField("NEW PASSWORD", newField));
        content.add(strut(12));
        content.add(UiFactory.labeledField("CONFIRM NEW PASSWORD", confirmField));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        FlatButton cancel = UiFactory.secondaryButton("Cancel");
        cancel.addActionListener(event -> dispose());
        updateButton.addActionListener(event -> updatePassword());
        buttons.add(cancel);
        buttons.add(updateButton);

        CardPanel card = new CardPanel();
        card.setLayout(new BorderLayout(0, 18));
        card.add(content, BorderLayout.CENTER);
        card.add(buttons, BorderLayout.SOUTH);

        root.add(card, BorderLayout.CENTER);
        setContentPane(root);
        getRootPane().setDefaultButton(updateButton);
        getRootPane().registerKeyboardAction(
                (ActionEvent event) -> dispose(),
                KeyStroke.getKeyStroke("ESCAPE"),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        pack();
        setLocationRelativeTo(frame);
    }

    private static JComponent strut(int height) {
        JComponent strut = (JComponent) Box.createVerticalStrut(height);
        strut.setAlignmentX(Component.LEFT_ALIGNMENT);
        strut.setMaximumSize(new Dimension(0, height));
        return strut;
    }

    private void updatePassword() {
        errorLabel.setText(" ");
        Administrator administrator = frame.getCurrentAdmin();
        if (administrator == null) {
            errorLabel.setText("The administrator session has expired. Please sign in again.");
            return;
        }
        String current = new String(currentField.getPassword());
        String next = new String(newField.getPassword());
        String confirmation = new String(confirmField.getPassword());
        if (current.isBlank() || next.isBlank() || confirmation.isBlank()) {
            errorLabel.setText("Please complete all three password fields.");
            return;
        }
        if (!next.equals(confirmation)) {
            errorLabel.setText("The new password and confirmation do not match.");
            return;
        }
        updateButton.setEnabled(false);
        try {
            context.getAdminService()
                    .changeAdministratorPassword(administrator.getUsername(), current, next);
            UiFactory.showSuccess(this, "The administrator password has been updated.");
            dispose();
        } catch (VoteSecureException ex) {
            errorLabel.setText(ex.getMessage());
        } catch (RuntimeException ex) {
            errorLabel.setText(UiFactory.safeMessage(ex));
        } finally {
            updateButton.setEnabled(true);
        }
    }
}
