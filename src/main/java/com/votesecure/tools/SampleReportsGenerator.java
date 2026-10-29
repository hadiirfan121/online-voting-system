package com.votesecure.tools;

import com.votesecure.AppContext;
import com.votesecure.database.DatabaseManager;
import com.votesecure.model.ResultsSummary;
import com.votesecure.service.ReportExporter;
import com.votesecure.util.DateTimeUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Generates the sample reports in {@code reports/} from an isolated
 * demonstration database. The demonstration election is seeded, six sample
 * ballots are cast through the real voting service, and the report is generated
 * with a clock after the voting window so the sample shows a final result.
 *
 * <p>This never opens or modifies the normal application database.</p>
 */
public final class SampleReportsGenerator {

    private static final String NOTICE =
            "SAMPLE / DEMONSTRATION REPORT - generated from an isolated demonstration database "
                    + "containing 10 sample voters, 3 sample candidates and 6 sample ballots. "
                    + "These are not real votes and this is not an official election result.";

    private SampleReportsGenerator() {
    }

    public static void main(String[] args) throws Exception {
        Path projectRoot = Path.of("").toAbsolutePath();
        Path reportsDirectory = projectRoot.resolve("reports");
        Path sampleDirectory = projectRoot.resolve("target").resolve("sample-data");
        Files.createDirectories(reportsDirectory);
        Files.createDirectories(sampleDirectory);

        Path databaseFile = sampleDirectory.resolve("sample-votesecure.db");
        Files.deleteIfExists(databaseFile);
        Files.deleteIfExists(sampleDirectory.resolve("sample-votesecure.db-wal"));
        Files.deleteIfExists(sampleDirectory.resolve("sample-votesecure.db-shm"));

        ZoneId zone = ZoneId.systemDefault();
        LocalDateTime votingMoment = LocalDateTime.of(2026, 9, 15, 10, 0);
        Clock votingClock = Clock.fixed(votingMoment.atZone(zone).toInstant(), zone);

        DatabaseManager database = new DatabaseManager(databaseFile, votingClock);
        database.initialize();
        AppContext context = AppContext.create(database, votingClock);

        String[][] ballots = {
                {"V001", "C001"},
                {"V002", "C001"},
                {"V003", "C001"},
                {"V004", "C002"},
                {"V005", "C002"},
                {"V006", "C003"}
        };
        for (String[] ballot : ballots) {
            context.getVotingManager().castVote(ballot[0], ballot[1]);
        }

        // A separate clock after the demo election's end makes the sample report final.
        AppContext reporting = AppContext.create(database, Clock.systemDefaultZone());
        ResultsSummary summary = reporting.getResultsService()
                .buildSummary(reporting.getElectionService().getCurrentElection());

        Path textReport = reportsDirectory.resolve("Sample_Results.txt");
        Path csvReport = reportsDirectory.resolve("Sample_Results.csv");
        ReportExporter exporter = reporting.getReportExporter();
        exporter.exportText(summary, textReport, NOTICE);
        exporter.exportCsv(summary, csvReport, NOTICE);

        System.out.println("Sample reports generated from " + databaseFile);
        System.out.println("Election: " + summary.getElection().getElectionName());
        System.out.println("Votes: " + summary.getVotesCast() + "/" + summary.getEligibleVoters()
                + " (turnout " + String.format(java.util.Locale.ENGLISH, "%.1f%%",
                summary.getTurnoutPercentage()) + ")");
        System.out.println("Outcome: " + summary.getOutcomeText());
        System.out.println("Window: " + DateTimeUtils.format(summary.getElection().getStartDateTime())
                + " to " + DateTimeUtils.format(summary.getElection().getEndDateTime()));
        System.out.println("TXT: " + textReport);
        System.out.println("CSV: " + csvReport);
    }
}
