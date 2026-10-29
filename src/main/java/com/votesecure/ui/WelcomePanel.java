package com.votesecure.ui;

import com.votesecure.AppContext;

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
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;

/** Welcome screen with the two sign-in entry points and public results link. */
public final class WelcomePanel extends JPanel implements Refreshable {

    private final MainFrame frame;

    public WelcomePanel(MainFrame frame, AppContext context) {
        this.frame = frame;
        setOpaque(false);
        setLayout(new GridLayout(1, 2, 0, 0));
        add(buildHero());
        add(buildAccessPanel());
    }

    private JPanel buildHero() {
        GradientPanel hero = new GradientPanel(Theme.PRIMARY_DEEP, Theme.PRIMARY);
        hero.setLayout(new BorderLayout(0, 24));
        hero.setBorder(new EmptyBorder(44, 48, 36, 48));

        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        brand.setOpaque(false);
        brand.add(new LogoMark(Color.WHITE, Theme.PRIMARY, 42));
        JPanel brandText = new JPanel();
        brandText.setOpaque(false);
        brandText.setLayout(new BoxLayout(brandText, BoxLayout.Y_AXIS));
        JLabel brandName = new JLabel("VOTESECURE");
        brandName.setFont(new Font(Theme.FONT_FAMILY, Font.BOLD, 15));
        brandName.setForeground(Color.WHITE);
        JLabel brandTagline = new JLabel("Campus election platform");
        brandTagline.setFont(Theme.SMALL);
        brandTagline.setForeground(Theme.WHITE_SOFT);
        brandText.add(brandName);
        brandText.add(brandTagline);
        brand.add(brandText);
        hero.add(brand, BorderLayout.NORTH);

        JPanel centre = new JPanel();
        centre.setOpaque(false);
        centre.setLayout(new BoxLayout(centre, BoxLayout.Y_AXIS));

        JLabel eyebrow = new JLabel("ONE PERSON  ·  ONE VOTE");
        eyebrow.setFont(Theme.SMALL_BOLD);
        eyebrow.setForeground(Theme.WHITE_SOFT);
        eyebrow.setAlignmentX(Component.LEFT_ALIGNMENT);
        centre.add(eyebrow);
        centre.add(Box.createVerticalStrut(18));

        JLabel headline = new JLabel("<html>Vote with<br>confidence.</html>");
        headline.setFont(Theme.HERO);
        headline.setForeground(Color.WHITE);
        headline.setAlignmentX(Component.LEFT_ALIGNMENT);
        centre.add(headline);
        centre.add(Box.createVerticalStrut(18));

        JLabel description = UiFactory.wrappedLabel(
                "A clean, secure and easy-to-use desktop voting experience built for a "
                        + "college election demonstration using Java and SQLite.",
                430, Theme.WHITE_SOFT, Theme.BODY_LARGE);
        description.setAlignmentX(Component.LEFT_ALIGNMENT);
        centre.add(description);
        centre.add(Box.createVerticalStrut(30));

        centre.add(feature("Secure sign-in", "PINs are salted and hashed with PBKDF2."));
        centre.add(feature("One ballot per voter", "Enforced by a database unique constraint."));
        centre.add(feature("Transparent results", "Totals are calculated from saved ballots."));
        hero.add(centre, BorderLayout.CENTER);

        JLabel footer = new JLabel("Academic prototype  ·  Java 17  ·  Swing  ·  SQLite");
        footer.setFont(Theme.SMALL);
        footer.setForeground(Theme.WHITE_SOFT);
        hero.add(footer, BorderLayout.SOUTH);
        return hero;
    }

    private JPanel feature(String title, String description) {
        JPanel row = new JPanel();
        row.setOpaque(false);
        row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel titleLabel = new JLabel("•  " + title);
        titleLabel.setFont(new Font(Theme.FONT_FAMILY, Font.BOLD, 14));
        titleLabel.setForeground(Color.WHITE);
        JLabel descriptionLabel = new JLabel("     " + description);
        descriptionLabel.setFont(Theme.SMALL);
        descriptionLabel.setForeground(Theme.WHITE_SOFT);
        row.add(titleLabel);
        row.add(Box.createVerticalStrut(3));
        row.add(descriptionLabel);
        row.add(Box.createVerticalStrut(15));
        return row;
    }

    private JPanel buildAccessPanel() {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setOpaque(false);
        wrapper.setBorder(new EmptyBorder(36, 54, 36, 54));

        CardPanel card = new CardPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        StatusBadge badge = UiFactory.badge("SECURE ACCESS", Theme.PRIMARY, Theme.PRIMARY_LIGHT);
        badge.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(badge);
        card.add(Box.createVerticalStrut(16));

        JLabel title = new JLabel("Welcome");
        title.setFont(Theme.H1);
        title.setForeground(Theme.TEXT);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(title);
        card.add(Box.createVerticalStrut(6));

        JLabel subtitle = UiFactory.wrappedLabel(
                "Sign in to cast your vote or to manage the demonstration election.",
                360, Theme.MUTED, Theme.BODY);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(subtitle);
        card.add(Box.createVerticalStrut(22));

        FlatButton voterButton = UiFactory.primaryButton("Sign in as voter");
        UiFactory.makeFullWidth(voterButton);
        voterButton.addActionListener(event -> frame.showVoterLoginScreen());
        card.add(voterButton);
        card.add(Box.createVerticalStrut(10));

        FlatButton adminButton = UiFactory.secondaryButton("Sign in as administrator");
        UiFactory.makeFullWidth(adminButton);
        adminButton.addActionListener(event -> frame.showAdminLoginScreen());
        card.add(adminButton);
        card.add(Box.createVerticalStrut(10));

        FlatButton resultsButton = UiFactory.ghostButton("View election results");
        UiFactory.makeFullWidth(resultsButton);
        resultsButton.addActionListener(event -> frame.showResults(false));
        card.add(resultsButton);
        card.add(Box.createVerticalStrut(22));

        JPanel divider = new JPanel();
        divider.setBackground(Theme.BORDER);
        divider.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        divider.setPreferredSize(new Dimension(10, 1));
        divider.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(divider);
        card.add(Box.createVerticalStrut(18));

        JPanel demo = UiFactory.infoBox(
                "DEMONSTRATION ACCESS\n"
                        + "Voter: V001  ·  PIN 1234   (also V002-V010)\n"
                        + "Administrator: admin  ·  Admin@123\n"
                        + "These sample credentials exist only in the local demo database.",
                Theme.PRIMARY_DARK, Theme.PRIMARY_LIGHT, 340);
        demo.setAlignmentX(Component.LEFT_ALIGNMENT);
        demo.setMaximumSize(new Dimension(Integer.MAX_VALUE, demo.getPreferredSize().height));
        card.add(demo);
        card.add(Box.createVerticalStrut(16));

        JLabel disclaimer = UiFactory.wrappedLabel(
                "This is a local academic prototype, not an official election platform.",
                340, Theme.MUTED, Theme.SMALL);
        disclaimer.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(disclaimer);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.CENTER;
        constraints.insets = new Insets(0, 0, 0, 0);
        wrapper.add(card, constraints);
        return wrapper;
    }

    @Override
    public void onShow() {
        // The welcome screen has no live data to reload.
    }
}
