package com.votesecure.service;

import com.votesecure.exception.ExportException;
import com.votesecure.model.CandidateResult;
import com.votesecure.model.ResultsSummary;
import com.votesecure.util.DateTimeUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Writes human-readable TXT and spreadsheet-friendly CSV result reports using
 * the actual values in a {@link ResultsSummary}.
 */
public final class ReportExporter {

    public static final String DISCLAIMER =
            "This report was produced by VoteSecure, an academic local prototype. "
                    + "It is not an official, digitally certified election record and must not be used "
                    + "for a real public election.";

    private static final String TITLE = "VOTESECURE - ONLINE VOTING SYSTEM";

    public void exportText(ResultsSummary summary, Path destination, String notice) throws ExportException {
        write(destination, buildText(summary, notice));
    }

    public void exportCsv(ResultsSummary summary, Path destination, String notice) throws ExportException {
        // UTF-8 BOM helps Microsoft Excel display the file correctly.
        write(destination, "\uFEFF" + buildCsv(summary, notice));
    }

    public String buildText(ResultsSummary summary, String notice) {
        StringBuilder text = new StringBuilder();
        text.append(TITLE).append('\n');
        text.append("Election Results Report\n");
        text.append("============================================================\n");
        if (notice != null && !notice.isBlank()) {
            text.append(notice.trim()).append('\n');
            text.append("------------------------------------------------------------\n");
        }
        text.append("Election: ").append(summary.getElection().getElectionName()).append('\n');
        text.append("Election ID: ").append(summary.getElection().getElectionId()).append('\n');
        text.append("Status: ").append(summary.getStatus().getDisplayName()).append('\n');
        text.append("Voting window: ")
                .append(DateTimeUtils.format(summary.getElection().getStartDateTime()))
                .append("  to  ")
                .append(DateTimeUtils.format(summary.getElection().getEndDateTime()))
                .append('\n');
        text.append("Report generated: ").append(DateTimeUtils.format(summary.getGeneratedAt())).append('\n');
        text.append(DateTimeUtils.TIME_ZONE_NOTE).append('\n');
        text.append('\n');

        text.append("PARTICIPATION\n");
        text.append("------------------------------------------------------------\n");
        text.append("Eligible registered voters: ").append(summary.getEligibleVoters()).append('\n');
        text.append("Votes cast (valid): ").append(summary.getVotesCast()).append('\n');
        text.append("Turnout: ").append(formatPercentage(summary.getTurnoutPercentage())).append('\n');
        text.append('\n');

        text.append("CANDIDATE RESULTS\n");
        text.append("------------------------------------------------------------\n");
        text.append(String.format(Locale.ENGLISH, "%-12s  %-24s  %-20s  %7s  %10s%n",
                "Candidate ID", "Candidate Name", "Political Party", "Votes", "% Valid"));
        for (CandidateResult result : summary.getCandidateResults()) {
            text.append(String.format(Locale.ENGLISH, "%-12s  %-24s  %-20s  %7d  %9.1f%%%n",
                    result.getCandidate().getCandidateId(),
                    result.getCandidate().getName(),
                    result.getCandidate().getPoliticalParty(),
                    result.getVotes(),
                    result.getPercentageOfValidVotes()));
        }
        text.append('\n');
        text.append("Total valid votes: ").append(summary.getVotesCast()).append('\n');
        text.append('\n');

        text.append("OUTCOME\n");
        text.append("------------------------------------------------------------\n");
        text.append(summary.getOutcomeText()).append('\n');
        if (summary.isTie()) {
            text.append("Two or more candidates have the same highest vote count.\n");
        }
        text.append('\n');

        text.append("DISCLAIMER\n");
        text.append("------------------------------------------------------------\n");
        text.append(DISCLAIMER).append('\n');
        return text.toString();
    }

    public String buildCsv(ResultsSummary summary, String notice) {
        StringBuilder csv = new StringBuilder();
        csv.append(line("Report", "VoteSecure Election Results"));
        if (notice != null && !notice.isBlank()) {
            csv.append(line("Notice", notice.trim()));
        }
        csv.append(line("Election ID", summary.getElection().getElectionId()));
        csv.append(line("Election Name", summary.getElection().getElectionName()));
        csv.append(line("Status", summary.getStatus().getDisplayName()));
        csv.append(line("Voting Starts", DateTimeUtils.format(summary.getElection().getStartDateTime())));
        csv.append(line("Voting Ends", DateTimeUtils.format(summary.getElection().getEndDateTime())));
        csv.append(line("Report Generated", DateTimeUtils.format(summary.getGeneratedAt())));
        csv.append(line("Eligible Registered Voters", Integer.toString(summary.getEligibleVoters())));
        csv.append(line("Votes Cast", Integer.toString(summary.getVotesCast())));
        csv.append(line("Turnout (%)", formatNumber(summary.getTurnoutPercentage())));
        csv.append(line("Outcome", summary.getOutcomeText()));
        csv.append("\r\n");

        csv.append("Candidate ID,Candidate Name,Political Party,Votes,Percentage of Valid Votes\r\n");
        for (CandidateResult result : summary.getCandidateResults()) {
            csv.append(escape(result.getCandidate().getCandidateId())).append(',')
                    .append(escape(result.getCandidate().getName())).append(',')
                    .append(escape(result.getCandidate().getPoliticalParty())).append(',')
                    .append(result.getVotes()).append(',')
                    .append(formatNumber(result.getPercentageOfValidVotes()))
                    .append("\r\n");
        }
        csv.append("\r\n");
        csv.append(line("Disclaimer", DISCLAIMER));
        return csv.toString();
    }

    private String line(String label, String value) {
        return escape(label) + "," + escape(value) + "\r\n";
    }

    private String escape(String value) {
        String safe = value == null ? "" : value;
        if (safe.contains(",") || safe.contains("\"") || safe.contains("\n") || safe.contains("\r")) {
            return '"' + safe.replace("\"", "\"\"") + '"';
        }
        return safe;
    }

    private String formatPercentage(double value) {
        return String.format(Locale.ENGLISH, "%.1f%%", value);
    }

    private String formatNumber(double value) {
        return String.format(Locale.ENGLISH, "%.1f", value);
    }

    private void write(Path destination, String content) throws ExportException {
        if (destination == null) {
            throw new ExportException("No report destination was selected.");
        }
        try {
            Path parent = destination.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(destination, content, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new ExportException(
                    "The report could not be written to the selected location. Check the folder permissions and try again.",
                    ex);
        }
    }
}
