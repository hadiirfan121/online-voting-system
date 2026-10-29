package com.votesecure.ui;

import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** White rounded surface used for panels and form sections. */
public final class CardPanel extends JPanel {

    public CardPanel() {
        setOpaque(false);
        setBackground(Theme.SURFACE);
        setBorder(new EmptyBorder(20, 22, 20, 22));
        setLayout(new java.awt.BorderLayout(0, 14));
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int width = getWidth();
        int height = getHeight();

        g2.setColor(new Color(26, 38, 54, 14));
        g2.fillRoundRect(1, 3, width - 4, height - 5, 18, 18);
        g2.setColor(Theme.SURFACE);
        g2.fillRoundRect(0, 0, width - 3, height - 4, 18, 18);
        g2.setColor(Theme.BORDER);
        g2.drawRoundRect(0, 0, width - 4, height - 5, 18, 18);
        g2.dispose();
        super.paintComponent(graphics);
    }
}
