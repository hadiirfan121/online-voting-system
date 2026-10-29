package com.votesecure.ui;

import com.votesecure.AppContext;
import com.votesecure.util.DateTimeUtils;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.time.LocalDateTime;

/** Confirmation screen shown after a ballot has been committed. */
public final class VoteSuccessPanel extends JPanel implements Refreshable {

    private final MainFrame frame;
    private final JLabel thanksLabel = new JLabel("Thank you.");
    private final JLabel timeLabel = new JLabel("Your ballot has been saved.");

    public VoteSuccessPanel(MainFrame frame, AppContext context) {
        this.frame = frame;
        setOpaque(false);
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(24, 28, 24, 28));

        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setOpaque(false);
        wrapper.add(buildCard());
        add(wrapper, BorderLayout.CENTER);
    }

    private JPanel buildCard() {
        CardPanel card = new CardPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setPreferredSize(new Dimension(600, 480));

        CheckBadge badge = new CheckBadge();
        badge.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(badge);
        card.add(Box.createVerticalStrut(18));

        JLabel title = new JLabel("Vote recorded successfully");
        title.setFont(Theme.H1);
        title.setForeground(Theme.SUCCESS);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(title);
        card.add(Box.createVerticalStrut(10));

        thanksLabel.setFont(Theme.H3);
        thanksLabel.setForeground(Theme.TEXT);
        thanksLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(thanksLabel);
        card.add(Box.createVerticalStrut(6));

        timeLabel.setFont(Theme.BODY);
        timeLabel.setForeground(Theme.MUTED);
        timeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(timeLabel);
        card.add(Box.createVerticalStrut(18));

        JLabel finalNote = UiFactory.wrappedLabel(
                "Your ballot is final and cannot be changed. "
                        + "You may close this window or return to your dashboard.",
                440, Theme.MUTED, Theme.BODY);
        finalNote.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(finalNote);
        card.add(Box.createVerticalStrut(22));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttons.setOpaque(false);
        FlatButton dashboard = UiFactory.primaryButton("Return to dashboard");
        dashboard.addActionListener(event -> frame.showVoterDashboard());
        FlatButton results = UiFactory.secondaryButton("View results");
        results.addActionListener(event -> frame.showResults(false));
        FlatButton signOut = UiFactory.ghostButton("Sign out");
        signOut.addActionListener(event -> frame.signOut());
        buttons.add(dashboard);
        buttons.add(results);
        buttons.add(signOut);
        buttons.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(buttons);
        card.add(Box.createVerticalStrut(16));

        JLabel privacy = UiFactory.wrappedLabel(
                "Results are published as aggregate totals only. This is an academic prototype and is not an official election system.",
                440, Theme.MUTED, Theme.SMALL);
        privacy.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(privacy);
        return card;
    }

    public void setSubmission(String voterName, LocalDateTime votedAt) {
        thanksLabel.setText("Thank you, " + voterName + ".");
        timeLabel.setText("Ballot saved on " + DateTimeUtils.format(votedAt) + " (local time).");
    }

    @Override
    public void onShow() {
        revalidate();
        repaint();
    }

    private static final class CheckBadge extends JComponent {
        private static final long serialVersionUID = 1L;

        CheckBadge() {
            setPreferredSize(new Dimension(86, 86));
            setMinimumSize(new Dimension(86, 86));
            setMaximumSize(new Dimension(86, 86));
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Theme.SUCCESS_LIGHT);
            g2.fillOval(0, 0, 84, 84);
            g2.setColor(Theme.SUCCESS);
            g2.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Path2D check = new Path2D.Double();
            check.moveTo(22, 44);
            check.lineTo(37, 59);
            check.lineTo(65, 29);
            g2.draw(check);
            g2.dispose();
        }
    }
}
