package com.votesecure.ui;

import javax.swing.JLabel;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** Small rounded label such as "Voting open" or "Locked". */
public final class StatusBadge extends JLabel {

    private final Color background;

    public StatusBadge(String text, Color foreground, Color background) {
        super(text);
        this.background = background;
        setForeground(foreground);
        setFont(Theme.SMALL_BOLD);
        setOpaque(false);
        setBorder(new EmptyBorder(5, 10, 5, 10));
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(background);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
        g2.dispose();
        super.paintComponent(graphics);
    }
}
