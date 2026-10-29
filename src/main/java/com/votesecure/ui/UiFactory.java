package com.votesecure.ui;

import com.votesecure.exception.VoteSecureException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Factory for consistently styled labels, fields, cards, tables and dialogs. */
public final class UiFactory {

    private static final Logger LOGGER = Logger.getLogger(UiFactory.class.getName());

    private UiFactory() {
    }

    public static FlatButton primaryButton(String text) {
        return new FlatButton(text, Theme.PRIMARY, Theme.PRIMARY_DARK, Color.WHITE, null);
    }

    public static FlatButton secondaryButton(String text) {
        return new FlatButton(text, Theme.SURFACE, Theme.PRIMARY_LIGHT, Theme.PRIMARY, Theme.PRIMARY_SOFT);
    }

    public static FlatButton dangerButton(String text) {
        return new FlatButton(text, Theme.DANGER_LIGHT, new Color(250, 218, 218), Theme.DANGER, new Color(240, 198, 198));
    }

    public static FlatButton ghostButton(String text) {
        return new FlatButton(text, new Color(0, 0, 0, 0), Theme.PRIMARY_LIGHT, Theme.PRIMARY, null);
    }

    public static FlatButton linkButton(String text) {
        FlatButton button = new FlatButton(text, new Color(0, 0, 0, 0), new Color(0, 0, 0, 0),
                Theme.PRIMARY, null);
        button.setBorder(new EmptyBorder(4, 2, 4, 2));
        button.setFont(Theme.BODY);
        return button;
    }

    public static JTextField textField(int columns) {
        JTextField field = new JTextField(columns);
        styleTextField(field);
        return field;
    }

    public static JPasswordField passwordField(int columns) {
        JPasswordField field = new JPasswordField(columns);
        styleTextField(field);
        return field;
    }

    public static JTextArea textArea(int rows) {
        JTextArea area = new JTextArea(rows, 20);
        area.setFont(Theme.FIELD);
        area.setForeground(Theme.TEXT);
        area.setBackground(Theme.SURFACE);
        area.setCaretColor(Theme.TEXT);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        RoundedBorder border = new RoundedBorder(Theme.BORDER, 10, new Insets(8, 10, 8, 10));
        area.setBorder(border);
        area.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent event) {
                border.setColor(Theme.PRIMARY);
                area.repaint();
            }

            @Override
            public void focusLost(FocusEvent event) {
                border.setColor(Theme.BORDER);
                area.repaint();
            }
        });
        return area;
    }

    private static void styleTextField(JTextField field) {
        field.setFont(Theme.FIELD);
        field.setForeground(Theme.TEXT);
        field.setBackground(Theme.SURFACE);
        field.setCaretColor(Theme.TEXT);
        field.setPreferredSize(new Dimension(240, 40));
        RoundedBorder border = new RoundedBorder(Theme.BORDER, 10, new Insets(8, 10, 8, 10));
        field.setBorder(border);
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent event) {
                border.setColor(Theme.PRIMARY);
                field.repaint();
            }

            @Override
            public void focusLost(FocusEvent event) {
                border.setColor(Theme.BORDER);
                field.repaint();
            }
        });
    }

    public static JCheckBox checkBox(String text, boolean selected) {
        JCheckBox box = new JCheckBox(text, selected);
        box.setOpaque(false);
        box.setFont(Theme.BODY);
        box.setForeground(Theme.TEXT);
        box.setFocusPainted(false);
        box.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return box;
    }

    public static JLabel title(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.H1);
        label.setForeground(Theme.TEXT);
        return label;
    }

    public static JLabel heading(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.H2);
        label.setForeground(Theme.TEXT);
        return label;
    }

    public static JLabel subheading(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.H3);
        label.setForeground(Theme.TEXT);
        return label;
    }

    public static JLabel body(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.BODY);
        label.setForeground(Theme.TEXT);
        return label;
    }

    public static JLabel muted(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.BODY);
        label.setForeground(Theme.MUTED);
        return label;
    }

    public static JLabel smallMuted(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.SMALL);
        label.setForeground(Theme.MUTED);
        return label;
    }

    public static JLabel fieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.SMALL_BOLD);
        label.setForeground(Theme.MUTED);
        return label;
    }

    public static JLabel errorLabel() {
        JLabel label = new JLabel(" ");
        label.setFont(Theme.SMALL);
        label.setForeground(Theme.DANGER);
        return label;
    }

    public static StatusBadge badge(String text, Color foreground, Color background) {
        return new StatusBadge(text, foreground, background);
    }

    public static JPanel pageHeader(String title, String subtitle, JComponent... actions) {
        JPanel header = new JPanel(new BorderLayout(24, 0));
        header.setOpaque(false);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new javax.swing.BoxLayout(text, javax.swing.BoxLayout.Y_AXIS));
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(Theme.H1);
        titleLabel.setForeground(Theme.TEXT);
        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setFont(Theme.BODY);
        subtitleLabel.setForeground(Theme.MUTED);
        text.add(titleLabel);
        text.add(Box.createVerticalStrut(4));
        text.add(subtitleLabel);
        header.add(text, BorderLayout.CENTER);

        if (actions != null && actions.length > 0) {
            JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            right.setOpaque(false);
            for (JComponent action : actions) {
                if (action != null) {
                    right.add(action);
                }
            }
            header.add(right, BorderLayout.EAST);
        }
        return header;
    }

    public static JPanel labeledField(String labelText, JComponent field) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new javax.swing.BoxLayout(panel, javax.swing.BoxLayout.Y_AXIS));
        JLabel label = fieldLabel(labelText);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(label);
        panel.add(Box.createVerticalStrut(6));
        panel.add(field);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, panel.getPreferredSize().height + 8));
        return panel;
    }

    public static JPanel formColumn() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new javax.swing.BoxLayout(panel, javax.swing.BoxLayout.Y_AXIS));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return panel;
    }

    public static JPanel infoBox(String message, Color foreground, Color background) {
        return infoBox(message, foreground, background, 660);
    }

    public static JPanel infoBox(String message, Color foreground, Color background, int width) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(true);
        panel.setBackground(background);
        panel.setBorder(new EmptyBorder(12, 14, 12, 14));
        JLabel label = wrappedLabel(message, width, foreground, Theme.BODY);
        panel.add(label, BorderLayout.CENTER);
        return panel;
    }

    public static JPanel statCard(String label, String value, String hint, Color accent) {
        CardPanel card = new CardPanel();
        card.setLayout(new BorderLayout(0, 6));
        JLabel labelView = new JLabel(label.toUpperCase());
        labelView.setFont(Theme.SMALL_BOLD);
        labelView.setForeground(Theme.MUTED);
        JLabel valueView = new JLabel(value);
        valueView.setFont(Theme.STAT_VALUE);
        valueView.setForeground(accent);
        JLabel hintView = new JLabel(hint == null ? " " : hint);
        hintView.setFont(Theme.SMALL);
        hintView.setForeground(Theme.MUTED);
        card.add(labelView, BorderLayout.NORTH);
        card.add(valueView, BorderLayout.CENTER);
        card.add(hintView, BorderLayout.SOUTH);
        return card;
    }

    public static JTable table(TableModel model) {
        JTable table = new JTable(model);
        table.setFont(Theme.BODY);
        table.setForeground(Theme.TEXT);
        table.setBackground(Theme.SURFACE);
        table.setRowHeight(36);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(Theme.BORDER);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(false);
        table.setBorder(null);
        table.setDefaultRenderer(Object.class, new BodyRenderer());
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setPreferredSize(new Dimension(0, 38));
        table.getTableHeader().setDefaultRenderer(new HeaderRenderer());
        return table;
    }

    public static JScrollPane tableScroll(JTable table) {
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(new LineBorder(Theme.BORDER));
        scroll.getViewport().setBackground(Theme.SURFACE);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    public static JScrollPane scrollTextArea(JTextArea area) {
        JScrollPane scroll = new JScrollPane(area);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Theme.SURFACE);
        return scroll;
    }

    public static void makeFullWidth(JButton button) {
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
    }

    public static void showError(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, wrappedLabel(message, 380, Theme.TEXT, Theme.BODY),
                "VoteSecure", JOptionPane.ERROR_MESSAGE);
    }

    public static void showInfo(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, wrappedLabel(message, 380, Theme.TEXT, Theme.BODY),
                "VoteSecure", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void showSuccess(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, wrappedLabel(message, 380, Theme.TEXT, Theme.BODY),
                "VoteSecure", JOptionPane.INFORMATION_MESSAGE);
    }

    public static boolean confirm(Component parent, String title, String message) {
        int choice = JOptionPane.showConfirmDialog(parent,
                wrappedLabel(message, 380, Theme.TEXT, Theme.BODY),
                title, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        return choice == JOptionPane.YES_OPTION;
    }

    public static String safeMessage(Throwable throwable) {
        if (throwable instanceof VoteSecureException && throwable.getMessage() != null) {
            return throwable.getMessage();
        }
        LOGGER.log(Level.SEVERE, "An unexpected UI failure occurred.", throwable);
        return "An unexpected error occurred. Please check the log file in the application data folder and try again.";
    }

    public static JLabel wrappedLabel(String message, int width, Color color, Font font) {
        String safe = message == null ? "" : message;
        safe = safe.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\n", "<br>");
        // A table cell honours the requested pixel width exactly; body/div widths are
        // rendered about 30% wider by Swing's HTML view, which clips card text.
        JLabel label = new JLabel("<html><table cellpadding='0' cellspacing='0' border='0'>"
                + "<tr><td width='" + width + "'>" + safe + "</td></tr></table></html>");
        label.setFont(font);
        label.setForeground(color);
        return label;
    }

    private static final class BodyRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setBorder(new EmptyBorder(0, 12, 0, 12));
            if (isSelected) {
                setBackground(Theme.PRIMARY_LIGHT);
                setForeground(Theme.TEXT);
            } else {
                setBackground(row % 2 == 0 ? Theme.SURFACE : new Color(250, 252, 255));
                setForeground(Theme.TEXT);
            }
            return this;
        }
    }

    private static final class HeaderRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setBackground(Theme.PRIMARY_LIGHT);
            setForeground(Theme.PRIMARY_DARK);
            setFont(Theme.SMALL_BOLD);
            setBorder(new EmptyBorder(0, 12, 0, 12));
            setHorizontalAlignment(LEFT);
            return this;
        }
    }
}
