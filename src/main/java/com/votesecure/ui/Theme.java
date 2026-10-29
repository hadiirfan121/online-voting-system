package com.votesecure.ui;

import com.votesecure.model.ElectionStatus;

import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Central colour palette, fonts and Swing defaults for the blue-and-white theme. */
public final class Theme {

    private static final Logger LOGGER = Logger.getLogger(Theme.class.getName());

    public static final Color PRIMARY = new Color(30, 99, 197);
    public static final Color PRIMARY_DARK = new Color(22, 74, 148);
    public static final Color PRIMARY_DEEP = new Color(15, 52, 105);
    public static final Color PRIMARY_LIGHT = new Color(234, 242, 253);
    public static final Color PRIMARY_SOFT = new Color(205, 225, 250);
    public static final Color BACKGROUND = new Color(243, 246, 251);
    public static final Color SURFACE = Color.WHITE;
    public static final Color TEXT = new Color(26, 38, 54);
    public static final Color MUTED = new Color(101, 116, 139);
    public static final Color BORDER = new Color(218, 226, 236);
    public static final Color SUCCESS = new Color(24, 143, 78);
    public static final Color SUCCESS_LIGHT = new Color(231, 247, 238);
    public static final Color DANGER = new Color(201, 57, 57);
    public static final Color DANGER_LIGHT = new Color(253, 236, 236);
    public static final Color WARNING = new Color(166, 112, 26);
    public static final Color WARNING_LIGHT = new Color(255, 247, 227);
    public static final Color WHITE_SOFT = new Color(225, 236, 252);

    public static final String FONT_FAMILY = chooseFontFamily();
    public static final Font HERO = new Font(FONT_FAMILY, Font.BOLD, 42);
    public static final Font H1 = new Font(FONT_FAMILY, Font.BOLD, 28);
    public static final Font H2 = new Font(FONT_FAMILY, Font.BOLD, 20);
    public static final Font H3 = new Font(FONT_FAMILY, Font.BOLD, 16);
    public static final Font BODY = new Font(FONT_FAMILY, Font.PLAIN, 14);
    public static final Font BODY_LARGE = new Font(FONT_FAMILY, Font.PLAIN, 16);
    public static final Font SMALL = new Font(FONT_FAMILY, Font.PLAIN, 12);
    public static final Font SMALL_BOLD = new Font(FONT_FAMILY, Font.BOLD, 12);
    public static final Font FIELD = new Font(FONT_FAMILY, Font.PLAIN, 14);
    public static final Font BUTTON = new Font(FONT_FAMILY, Font.BOLD, 14);
    public static final Font STAT_VALUE = new Font(FONT_FAMILY, Font.BOLD, 30);
    public static final Font MONO = new Font("Consolas", Font.PLAIN, 13);

    private Theme() {
    }

    private static String chooseFontFamily() {
        try {
            String[] available = GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getAvailableFontFamilyNames();
            if (Arrays.asList(available).contains("Segoe UI")) {
                return "Segoe UI";
            }
        } catch (RuntimeException ex) {
            LOGGER.log(Level.FINE, "Could not inspect installed fonts.", ex);
        }
        return "SansSerif";
    }

    public static void install() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "System look and feel could not be loaded.", ex);
        }
        UIManager.put("Panel.background", BACKGROUND);
        UIManager.put("OptionPane.background", SURFACE);
        UIManager.put("OptionPane.messageFont", BODY);
        UIManager.put("OptionPane.buttonFont", BUTTON);
        UIManager.put("Label.foreground", TEXT);
        UIManager.put("Label.font", BODY);
        UIManager.put("TextField.font", FIELD);
        UIManager.put("PasswordField.font", FIELD);
        UIManager.put("TextArea.font", BODY);
        UIManager.put("ComboBox.font", FIELD);
        UIManager.put("CheckBox.font", BODY);
        UIManager.put("RadioButton.font", BODY);
        UIManager.put("Table.font", BODY);
        UIManager.put("Table.rowHeight", 32);
        UIManager.put("Table.background", SURFACE);
        UIManager.put("Table.foreground", TEXT);
        UIManager.put("Table.selectionBackground", PRIMARY_LIGHT);
        UIManager.put("Table.selectionForeground", TEXT);
        UIManager.put("Table.gridColor", BORDER);
        UIManager.put("TableHeader.font", SMALL_BOLD);
        UIManager.put("TableHeader.background", PRIMARY_LIGHT);
        UIManager.put("TableHeader.foreground", PRIMARY_DARK);
        UIManager.put("ScrollPane.background", BACKGROUND);
        UIManager.put("Viewport.background", SURFACE);
        UIManager.put("ScrollBar.background", BACKGROUND);
        UIManager.put("FileChooser.background", SURFACE);
        UIManager.put("ToolTip.background", TEXT);
        UIManager.put("ToolTip.foreground", Color.WHITE);
        UIManager.put("ToolTip.font", SMALL);
    }

    public static Color statusColor(ElectionStatus status) {
        return switch (status) {
            case OPEN -> SUCCESS;
            case NOT_STARTED -> WARNING;
            case CLOSED -> MUTED;
        };
    }

    public static Color statusBackground(ElectionStatus status) {
        return switch (status) {
            case OPEN -> SUCCESS_LIGHT;
            case NOT_STARTED -> WARNING_LIGHT;
            case CLOSED -> new Color(238, 242, 247);
        };
    }
}
