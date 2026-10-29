package com.votesecure.ui;

import javax.swing.border.Border;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;

/** Soft rounded border with inner padding for text fields and text areas. */
public final class RoundedBorder implements Border {

    private final int arc;
    private final Insets insets;
    private Color color;

    public RoundedBorder(Color color, int arc, Insets insets) {
        this.color = color;
        this.arc = arc;
        this.insets = insets;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    @Override
    public Insets getBorderInsets(Component component) {
        return insets;
    }

    @Override
    public boolean isBorderOpaque() {
        return false;
    }

    @Override
    public void paintBorder(Component component, Graphics graphics, int x, int y, int width, int height) {
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(color);
        g2.drawRoundRect(x, y, width - 1, height - 1, arc, arc);
        g2.dispose();
    }
}
