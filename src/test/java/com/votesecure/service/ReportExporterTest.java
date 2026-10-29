package com.votesecure.service;

import com.votesecure.exception.ExportException;
import com.votesecure.model.Candidate;
import com.votesecure.model.ResultsSummary;
import com.votesecure.support.TestEnvironment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportExporterTest {

    private TestEnvironment env;
    private ReportExporter exporter;

    @BeforeEach
    void setUp() throws Exception {
        env = new TestEnvironment();
        exporter = env.context().getReportExporter();
        env.addCandidate("C001");
        env.addCandidate("C002");
        env.registerVoter("V001");
        env.registerVoter("V002");
        env.context().getVotingManager().castVote("V001", "C001");
        env.context().getVotingManager().castVote("V002", "C002");
    }

    @AfterEach
    void tearDown() {
        env.close();
    }

    @Test
    @DisplayName("A readable UTF-8 text report is generated from real results")
    void textReportIsWritten() throws Exception {
        ResultsSummary summary = env.results();
        Path destination = env.tempDirectory().resolve("reports/results.txt");

        exporter.exportText(summary, destination, "AUTOMATED TEST REPORT");

        assertTrue(Files.exists(destination));
        String text = Files.readString(destination, StandardCharsets.UTF_8);
        assertTrue(text.contains("VOTESECURE - ONLINE VOTING SYSTEM"));
        assertTrue(text.contains("Election: Test Election"));
        assertTrue(text.contains("AUTOMATED TEST REPORT"));
        assertTrue(text.contains("Candidate Alpha"));
        assertTrue(text.contains("Candidate Beta"));
        assertTrue(text.contains("Votes cast (valid): 2"));
        assertTrue(text.contains("Turnout: 100.0%"));
        assertTrue(text.contains("Tie between Candidate Alpha, Candidate Beta"));
        assertTrue(text.contains(ReportExporter.DISCLAIMER));
        assertTrue(text.contains("Report generated:"));
    }

    @Test
    @DisplayName("A CSV report is generated with UTF-8 BOM and the expected table")
    void csvReportIsWritten() throws Exception {
        ResultsSummary summary = env.results();
        Path destination = env.tempDirectory().resolve("reports/results.csv");

        exporter.exportCsv(summary, destination, "AUTOMATED TEST REPORT");

        assertTrue(Files.exists(destination));
        byte[] bytes = Files.readAllBytes(destination);
        assertEquals((byte) 0xEF, bytes[0]);
        assertEquals((byte) 0xBB, bytes[1]);
        assertEquals((byte) 0xBF, bytes[2]);

        String csv = Files.readString(destination, StandardCharsets.UTF_8);
        assertTrue(csv.contains("Candidate ID,Candidate Name,Political Party,Votes,Percentage of Valid Votes"));
        assertTrue(csv.contains("C001,Candidate Alpha,Test Party,1,50.0"));
        assertTrue(csv.contains("C002,Candidate Beta,Test Party,1,50.0"));
        assertTrue(csv.contains("Eligible Registered Voters,2"));
        assertTrue(csv.contains("Votes Cast,2"));
        assertTrue(csv.contains("Turnout (%),100.0"));
        assertTrue(csv.contains("Tie"));
        assertTrue(csv.contains(ReportExporter.DISCLAIMER));
    }

    @Test
    @DisplayName("CSV quoting protects commas in candidate data")
    void csvEscapesSpecialCharacters() throws Exception {
        Candidate special = new Candidate("C009", env.election().getElectionId(),
                "Patel, Asha", "Unity, Party", "A \"quoted\" description",
                env.now());
        env.context().getCandidateRepository().save(special);
        Path destination = env.tempDirectory().resolve("special.csv");

        exporter.exportCsv(env.results(), destination, null);

        String csv = Files.readString(destination, StandardCharsets.UTF_8);
        assertTrue(csv.contains("\"Patel, Asha\",\"Unity, Party\",0,0.0"));
    }

    @Test
    @DisplayName("A zero-vote report formats percentages without dividing by zero")
    void zeroVoteReportIsSafe() throws Exception {
        TestEnvironment empty = new TestEnvironment();
        try {
            empty.addCandidate("C001");
            ResultsSummary summary = empty.results();
            Path text = empty.tempDirectory().resolve("empty.txt");
            Path csv = empty.tempDirectory().resolve("empty.csv");
            exporter.exportText(summary, text, null);
            exporter.exportCsv(summary, csv, null);

            assertTrue(Files.readString(text, StandardCharsets.UTF_8)
                    .contains("Turnout: 0.0%"));
            String csvText = Files.readString(csv, StandardCharsets.UTF_8);
            assertTrue(csvText.contains("Turnout (%),0.0"));
            assertTrue(csvText.contains("C001,Candidate Alpha,Test Party,0,0.0"));
        } finally {
            empty.close();
        }
    }

    @Test
    @DisplayName("An unwritable destination produces a friendly export error")
    void exportFailureIsReportedGracefully() throws Exception {
        Path blocker = Files.writeString(env.tempDirectory().resolve("blocker.txt"), "not a folder");
        Path destination = blocker.resolve("report.txt");

        assertThrows(ExportException.class,
                () -> exporter.exportText(env.results(), destination, null));
    }

    @Test
    @DisplayName("The exported bytes use UTF-8 encoding")
    void textReportUsesUtf8() throws Exception {
        Path destination = env.tempDirectory().resolve("utf8.txt");
        exporter.exportText(env.results(), destination, "Café — résumé");
        byte[] expectedPrefix = "VOTESECURE".getBytes(StandardCharsets.UTF_8);
        byte[] actual = Files.readAllBytes(destination);
        assertArrayEquals(expectedPrefix, java.util.Arrays.copyOfRange(actual, 0, expectedPrefix.length));
        assertTrue(Files.readString(destination, StandardCharsets.UTF_8).contains("Café — résumé"));
    }
}
