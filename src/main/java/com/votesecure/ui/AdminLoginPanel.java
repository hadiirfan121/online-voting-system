package com.votesecure.ui;

import com.votesecure.AppContext;
import com.votesecure.exception.AuthenticationException;
import com.votesecure.model.Administrator;

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

/** Administrator sign-in screen. */
public final class AdminLoginPanel extends JPanel implements Refreshable {

    private final MainFrame frame;
    private final AppContext context;
    private final JTextField usernameField = UiFactory.textField(20);
    private final JPasswordField passwordField = UiFactory.passwordField(20);
    private final JLabel errorLabel = UiFactory.errorLabel();
    private final FlatButton signInButton = UiFactory.primaryButton("Sign in as administrator");

    public AdminLoginPanel(MainFrame frame, AppContext context) {
        this.frame = frame;
        this.context = context;
        setOpaque(false);
        setLayout(new BorderLayout());
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
        usernameField.addActionListener(event -> passwordField.requestFocusInWindow());
        passwordField.addActionListener(event -> signIn());
    }

    private JPanel buildCard() {
        CardPanel card = new CardPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setPreferredSize(new Dimension(440, 490));

        StatusBadge badge = UiFactory.badge("ADMINISTRATOR", Theme.PRIMARY_DARK, Theme.PRIMARY_LIGHT);
        badge.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(badge);
        card.add(Box.createVerticalStrut(16));

        JLabel title = UiFactory.title("Administrator sign in");
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(title);
        card.add(Box.createVerticalStrut(6));

        JLabel subtitle = UiFactory.wrappedLabel(
                "Manage voters, candidates, the election window and result reports.",
                360, Theme.MUTED, Theme.BODY);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(subtitle);
        card.add(Box.createVerticalStrut(20));

        errorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(errorLabel);
        card.add(Box.createVerticalStrut(4));

        card.add(UiFactory.labeledField("USERNAME", usernameField));
        card.add(Box.createVerticalStrut(14));
        card.add(UiFactory.labeledField("PASSWORD", passwordField));
        card.add(Box.createVerticalStrut(20));

        UiFactory.makeFullWidth(signInButton);
        card.add(signInButton);
        card.add(Box.createVerticalStrut(18));

        JPanel demo = UiFactory.infoBox(
                "DEMONSTRATION ADMINISTRATOR\nUsername: admin\nPassword: Admin@123\n"
                        + "Change it from the dashboard after signing in.",
                Theme.PRIMARY_DARK, Theme.PRIMARY_LIGHT, 340);
        demo.setAlignmentX(Component.LEFT_ALIGNMENT);
        demo.setMaximumSize(new Dimension(Integer.MAX_VALUE, demo.getPreferredSize().height));
        card.add(demo);
        return card;
    }

    private void signIn() {
        errorLabel.setText(" ");
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());
        if (username.isBlank() || password.isBlank()) {
            errorLabel.setText("Please enter both the username and password.");
            return;
        }
        signInButton.setEnabled(false);
        try {
            Administrator administrator = context.getAuthenticationService()
                    .authenticateAdministrator(username, password);
            passwordField.setText("");
            frame.onAdministratorAuthenticated(administrator);
        } catch (AuthenticationException ex) {
            errorLabel.setText(ex.getMessage());
            passwordField.selectAll();
            passwordField.requestFocusInWindow();
        } catch (RuntimeException ex) {
            errorLabel.setText(UiFactory.safeMessage(ex));
        } finally {
            signInButton.setEnabled(true);
        }
    }

    @Override
    public void onShow() {
        errorLabel.setText(" ");
        passwordField.setText("");
        usernameField.requestFocusInWindow();
    }
}
