package com.votesecure.ui;

import javax.swing.JButton;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** A flat, rounded, hand-drawn button so the interface does not depend on the OS look. */
public final class FlatButton extends JButton {

    private final Color baseColor;
    private final Color hoverColor;
    private final Color borderColor;

    public FlatButton(String text, Color baseColor, Color hoverColor, Color textColor, Color borderColor) {
        super(text);
        this.baseColor = baseColor;
        this.hoverColor = hoverColor;
        this.borderColor = borderColor;
        setFont(Theme.BUTTON);
        setForeground(textColor);
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(new EmptyBorder(10, 18, 10, 18));
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int width = getWidth();
        int height = getHeight();

        if (baseColor.getAlpha() > 0) {
            Color fill = baseColor;
            if (!isEnabled()) {
                fill = blend(baseColor, Theme.SURFACE, 0.55f);
            } else if (getModel().isPressed()) {
                fill = darken(baseColor, 0.12f);
            } else if (getModel().isRollover()) {
                fill = hoverColor;
            }
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, width, height, 12, 12);
        }

        if (borderColor != null) {
            g2.setColor(isEnabled() ? borderColor : Theme.BORDER);
            g2.drawRoundRect(0, 0, width - 1, height - 1, 12, 12);
        }
        g2.dispose();
        super.paintComponent(graphics);
    }

    private Color blend(Color from, Color to, float ratio) {
        float inverse = 1f - ratio;
        return new Color(
                Math.round(from.getRed() * inverse + to.getRed() * ratio),
                Math.round(from.getGreen() * inverse + to.getGreen() * ratio),
                Math.round(from.getBlue() * inverse + to.getBlue() * ratio));
    }

    private Color darken(Color color, float amount) {
        return blend(color, Color.BLACK, amount);
    }
}
