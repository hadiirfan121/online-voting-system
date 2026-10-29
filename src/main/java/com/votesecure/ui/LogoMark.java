package com.votesecure.ui;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** Small painted "V" tile used as the application logo. */
public final class LogoMark extends JComponent {

    private final Color background;
    private final Color foreground;
    private final int size;

    public LogoMark(Color background, Color foreground, int size) {
        this.background = background;
        this.foreground = foreground;
        this.size = size;
        setPreferredSize(new Dimension(size, size));
        setMinimumSize(new Dimension(size, size));
        setMaximumSize(new Dimension(size, size));
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(background);
        g2.fillRoundRect(0, 0, size, size, size / 3, size / 3);
        g2.setColor(foreground);
        g2.setFont(new Font(Theme.FONT_FAMILY, Font.BOLD, Math.max(12, size / 2)));
        FontMetrics metrics = g2.getFontMetrics();
        String letter = "V";
        int x = (size - metrics.stringWidth(letter)) / 2;
        int y = (size - metrics.getHeight()) / 2 + metrics.getAscent();
        g2.drawString(letter, x, y);
        g2.dispose();
    }
}
