package com.votesecure.ui;

import com.votesecure.AppContext;
import com.votesecure.exception.AuthenticationException;
import com.votesecure.model.Voter;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/** Voter sign-in screen. */
public final class VoterLoginPanel extends JPanel implements Refreshable {

    private final MainFrame frame;
    private final AppContext context;
    private final JTextField voterIdField = UiFactory.textField(20);
    private final JPasswordField pinField = UiFactory.passwordField(20);
    private final JLabel errorLabel = UiFactory.errorLabel();
    private final FlatButton signInButton = UiFactory.primaryButton("Sign in as voter");

    public VoterLoginPanel(MainFrame frame, AppContext context) {
        this.frame = frame;
        this.context = context;
        setOpaque(false);
        setLayout(new BorderLayout(0, 0));
        setBorder(new EmptyBorder(24, 32, 24, 32));

        FlatButton back = UiFactory.linkButton("< Back to welcome");
        back.addActionListener(event -> frame.showWelcomeScreen());
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        top.setOpaque(false);
        top.add(back);
        add(top, BorderLayout.NORTH);

        JPanel centre = new JPanel(new GridBagLayout());
        centre.setOpaque(false);
        centre.add(buildCard(), new GridBagConstraints(0, 0, 1, 1, 1, 1,
                GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        add(centre, BorderLayout.CENTER);

        signInButton.addActionListener(event -> signIn());
        voterIdField.addActionListener(event -> pinField.requestFocusInWindow());
        pinField.addActionListener(event -> signIn());
    }

    private JPanel buildCard() {
        CardPanel card = new CardPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setPreferredSize(new Dimension(440, 480));

        StatusBadge badge = UiFactory.badge("VOTER ACCESS", Theme.PRIMARY, Theme.PRIMARY_LIGHT);
        badge.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(badge);
        card.add(Box.createVerticalStrut(16));

        JLabel title = UiFactory.title("Voter sign in");
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(title);
        card.add(Box.createVerticalStrut(6));

        JLabel subtitle = UiFactory.wrappedLabel(
                "Use the voter ID and PIN issued by the election administrator.",
                360, Theme.MUTED, Theme.BODY);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(subtitle);
        card.add(Box.createVerticalStrut(20));

        errorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(errorLabel);
        card.add(Box.createVerticalStrut(4));

        JPanel idField = UiFactory.labeledField("VOTER ID", voterIdField);
        card.add(idField);
        card.add(Box.createVerticalStrut(14));

        JPanel pin = UiFactory.labeledField("PIN", pinField);
        card.add(pin);
        card.add(Box.createVerticalStrut(20));

        UiFactory.makeFullWidth(signInButton);
        card.add(signInButton);
        card.add(Box.createVerticalStrut(18));

        JPanel demo = UiFactory.infoBox(
                "DEMONSTRATION VOTERS\nUse V001-V010 with PIN 1234.\nFor example: V001 / 1234",
                Theme.PRIMARY_DARK, Theme.PRIMARY_LIGHT, 340);
        demo.setAlignmentX(Component.LEFT_ALIGNMENT);
        demo.setMaximumSize(new Dimension(Integer.MAX_VALUE, demo.getPreferredSize().height));
        card.add(demo);
        return card;
    }

    private void signIn() {
        errorLabel.setText(" ");
        String voterId = voterIdField.getText();
        String pin = new String(pinField.getPassword());
        if (voterId.isBlank() || pin.isBlank()) {
            errorLabel.setText("Please enter both your voter ID and PIN.");
            return;
        }
        signInButton.setEnabled(false);
        try {
            Voter voter = context.getVotingManager().authenticateVoter(voterId, pin);
            pinField.setText("");
            frame.onVoterAuthenticated(voter);
        } catch (AuthenticationException ex) {
            errorLabel.setText(ex.getMessage());
            pinField.selectAll();
            pinField.requestFocusInWindow();
        } catch (RuntimeException ex) {
            errorLabel.setText(UiFactory.safeMessage(ex));
        } finally {
            signInButton.setEnabled(true);
        }
    }

    @Override
    public void onShow() {
        errorLabel.setText(" ");
        pinField.setText("");
        voterIdField.requestFocusInWindow();
    }
}
