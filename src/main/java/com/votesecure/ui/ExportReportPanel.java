package com.votesecure.ui;

import com.votesecure.AppContext;
import com.votesecure.exception.VoteSecureException;
import com.votesecure.model.Election;
import com.votesecure.model.ElectionStatus;
import com.votesecure.model.ResultsSummary;
import com.votesecure.service.ReportExporter;
import com.votesecure.util.DateTimeUtils;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Administrator screen that exports the current result as TXT or CSV. */
public final class ExportReportPanel extends JPanel implements Refreshable {

    private static final DateTimeFormatter FILE_STAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmm");

    private final MainFrame frame;
    private final AppContext context;
    private final JTextArea previewArea = new JTextArea();
    private final JLabel statusLabel = UiFactory.errorLabel();
    private final JLabel detailsLabel = UiFactory.wrappedLabel(" ", 520, Theme.MUTED, Theme.SMALL);
    private final FlatButton exportTextButton = UiFactory.primaryButton("Export TXT report");
    private final FlatButton exportCsvButton = UiFactory.secondaryButton("Export CSV report");
    private final FlatButton refreshButton = UiFactory.ghostButton("Refresh preview");

    private ResultsSummary summary;
    private File lastDirectory;

    public ExportReportPanel(MainFrame frame, AppContext context) {
        this.frame = frame;
        this.context = context;
        setOpaque(false);
        setLayout(new BorderLayout(0, 20));
        setBorder(new EmptyBorder(24, 28, 24, 28));

        FlatButton back = UiFactory.secondaryButton("Back to dashboard");
        back.addActionListener(event -> frame.showAdminDashboard());
        add(UiFactory.pageHeader("Export results",
                "Save an official-looking report of the current database results.", back),
                BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout(16, 0));
        content.setOpaque(false);
        content.add(buildControls(), BorderLayout.WEST);
        content.add(buildPreview(), BorderLayout.CENTER);
        add(content, BorderLayout.CENTER);

        exportTextButton.addActionListener(event -> export("txt"));
        exportCsvButton.addActionListener(event -> export("csv"));
        refreshButton.addActionListener(event -> refreshPreview(true));
        refreshPreview(false);
    }

    private JPanel buildControls() {
        CardPanel card = new CardPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setPreferredSize(new Dimension(360, 580));

        JLabel title = UiFactory.heading("Report options");
        title.setAlignmentX(LEFT_ALIGNMENT);
        card.add(title);
        card.add(Box.createVerticalStrut(8));

        JLabel intro = UiFactory.wrappedLabel(
                "The reports contain the election details, candidate counts, percentages, "
                        + "turnout, outcome and a generation timestamp. All numbers are read "
                        + "from the SQLite database — nothing is estimated or invented.",
                310, Theme.MUTED, Theme.SMALL);
        intro.setAlignmentX(LEFT_ALIGNMENT);
        card.add(intro);
        card.add(Box.createVerticalStrut(18));

        detailsLabel.setAlignmentX(LEFT_ALIGNMENT);
        card.add(detailsLabel);
        card.add(Box.createVerticalStrut(16));

        statusLabel.setAlignmentX(LEFT_ALIGNMENT);
        card.add(statusLabel);
        card.add(Box.createVerticalStrut(8));

        JPanel buttons = new JPanel(new GridLayout(3, 1, 8, 8));
        buttons.setOpaque(false);
        buttons.add(exportTextButton);
        buttons.add(exportCsvButton);
        buttons.add(refreshButton);
        buttons.setAlignmentX(LEFT_ALIGNMENT);
        buttons.setMaximumSize(new Dimension(Integer.MAX_VALUE, 142));
        card.add(buttons);
        card.add(Box.createVerticalStrut(16));

        JPanel note = UiFactory.infoBox(
                "TXT is suitable for printing or attaching to a report. CSV opens in "
                        + "Microsoft Excel and other spreadsheet software (UTF-8 with BOM).",
                Theme.PRIMARY_DARK, Theme.PRIMARY_LIGHT, 290);
        note.setAlignmentX(LEFT_ALIGNMENT);
        note.setMaximumSize(new Dimension(Integer.MAX_VALUE, note.getPreferredSize().height + 8));
        card.add(note);
        card.add(Box.createVerticalGlue());

        JLabel disclaimer = UiFactory.wrappedLabel(
                "Academic prototype only: this export is not a digitally certified "
                        + "election record.",
                310, Theme.MUTED, Theme.SMALL);
        disclaimer.setAlignmentX(LEFT_ALIGNMENT);
        card.add(disclaimer);
        return card;
    }

    private JPanel buildPreview() {
        CardPanel card = new CardPanel();
        card.setLayout(new BorderLayout(0, 10));

        JPanel heading = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        heading.setOpaque(false);
        heading.add(UiFactory.heading("Text report preview"));
        heading.add(UiFactory.badge("ADMIN ONLY", Theme.PRIMARY_DARK, Theme.PRIMARY_LIGHT));
        card.add(heading, BorderLayout.NORTH);

        previewArea.setEditable(false);
        previewArea.setFont(Theme.MONO);
        previewArea.setForeground(Theme.TEXT);
        previewArea.setBackground(new java.awt.Color(250, 252, 255));
        previewArea.setBorder(new EmptyBorder(12, 14, 12, 14));
        previewArea.setLineWrap(false);
        previewArea.setCaretPosition(0);
        card.add(UiFactory.scrollTextArea(previewArea), BorderLayout.CENTER);
        card.add(UiFactory.smallMuted(
                "Preview shows the TXT layout. The CSV file contains the same data in a table."),
                BorderLayout.SOUTH);
        return card;
    }

    @Override
    public void onShow() {
        refreshPreview(false);
    }

    private void refreshPreview(boolean announce) {
        statusLabel.setForeground(Theme.DANGER);
        statusLabel.setText(" ");
        try {
            Election election = context.getElectionService().getCurrentElection();
            summary = context.getResultsService().buildSummary(election);
            String notice = exportNotice(summary);
            previewArea.setText(context.getReportExporter().buildText(summary, notice));
            previewArea.setCaretPosition(0);
            detailsLabel.setText("<html><body style='width:300px'>"
                    + "<b>" + escape(summary.getElection().getElectionName()) + "</b><br>"
                    + "Status: " + summary.getStatus().getDisplayName() + "<br>"
                    + "Votes cast: " + summary.getVotesCast() + " of "
                    + summary.getEligibleVoters() + " eligible voters<br>"
                    + "Snapshot: " + DateTimeUtils.format(summary.getGeneratedAt())
                    + "</body></html>");
            if (announce) {
                statusLabel.setForeground(Theme.SUCCESS);
                statusLabel.setText("Preview refreshed from the database.");
            }
        } catch (Exception ex) {
            summary = null;
            statusLabel.setText(UiFactory.safeMessage(ex));
            previewArea.setText("Results could not be loaded. " + UiFactory.safeMessage(ex));
        }
    }

    private String exportNotice(ResultsSummary current) {
        if (current.getStatus() == ElectionStatus.OPEN) {
            return "LIVE PREVIEW — the election is still open. These figures are a snapshot taken "
                    + "while voting is in progress and are not the final declared result.";
        }
        if (!current.hasVotes()) {
            return "NO VOTES RECORDED — this report was generated from the current database, "
                    + "which contains no ballots for this election.";
        }
        return null;
    }

    private void export(String format) {
        if (summary == null) {
            refreshPreview(false);
            if (summary == null) {
                return;
            }
        }
        JFileChooser chooser = new JFileChooser(lastDirectory != null ? lastDirectory
                : new File(System.getProperty("user.home")));
        chooser.setDialogTitle("Save " + format.toUpperCase() + " results report");
        String extension = format;
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                format.equals("csv") ? "CSV spreadsheet (*.csv)" : "Text document (*.txt)", extension);
        chooser.setFileFilter(filter);
        chooser.setAcceptAllFileFilterUsed(false);
        String base = "VoteSecure_Results_" + FILE_STAMP.format(LocalDateTime.now());
        chooser.setSelectedFile(new File(chooser.getCurrentDirectory(), base + "." + extension));

        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            statusLabel.setForeground(Theme.MUTED);
            statusLabel.setText("Export cancelled. No file was written.");
            return;
        }
        File selected = chooser.getSelectedFile();
        if (selected == null) {
            return;
        }
        String name = selected.getName();
        if (!name.toLowerCase().endsWith("." + extension)) {
            selected = new File(selected.getParentFile(), name + "." + extension);
        }
        lastDirectory = selected.getParentFile();
        Path destination = selected.toPath();

        statusLabel.setForeground(Theme.DANGER);
        statusLabel.setText(" ");
        String notice = exportNotice(summary);
        try {
            if (format.equals("csv")) {
                context.getReportExporter().exportCsv(summary, destination, notice);
            } else {
                context.getReportExporter().exportText(summary, destination, notice);
            }
            statusLabel.setForeground(Theme.SUCCESS);
            statusLabel.setText("Saved to " + destination);
            UiFactory.showSuccess(this, "The " + format.toUpperCase()
                    + " report was saved successfully:\n\n" + destination);
        } catch (VoteSecureException ex) {
            statusLabel.setText(ex.getMessage());
            UiFactory.showError(this, ex.getMessage());
        } catch (RuntimeException ex) {
            String message = UiFactory.safeMessage(ex);
            statusLabel.setText(message);
            UiFactory.showError(this, message);
        }
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
