# VoteSecure — Test Cases

**Project:** VoteSecure — Online Voting System
**Test framework:** JUnit 5.10.2
**Environment:** Windows 11 x64, JDK 25.0.4.1, Maven 3.10.0, SQLite JDBC 3.46.1.0
**Executed:** 28 October 2026
**Command:** `mvn test`

---

## 1. Summary

| Test class | Tests | Passed | Failed | Errors | Skipped |
| --- | ---: | ---: | ---: | ---: | ---: |
| `AuthenticationServiceTest` | 8 | 8 | 0 | 0 | 0 |
| `VotingWorkflowTest` | 14 | 14 | 0 | 0 | 0 |
| `ResultsServiceTest` | 7 | 7 | 0 | 0 | 0 |
| `ReportExporterTest` | 6 | 6 | 0 | 0 | 0 |
| `AdminServiceTest` | 11 | 11 | 0 | 0 | 0 |
| `PasswordHasherTest` | 5 | 5 | 0 | 0 | 0 |
| `ElectionTest` | 4 | 4 | 0 | 0 | 0 |
| `DemoDataTest` | 4 | 4 | 0 | 0 | 0 |
| **Total** | **59** | **59** | **0** | **0** | **0** |

Maven result: `Tests run: 59, Failures: 0, Errors: 0, Skipped: 0` — **BUILD SUCCESS**
(wall time 27.111 s for the full run including compilation).

### Isolation and time control

- Every test class creates a unique temporary directory and SQLite file through
  `com.votesecure.support.TestEnvironment`. The real application database
  (`%USERPROFILE%\.votesecure\votesecure.db`) is never used by tests.
- `TestEnvironment` creates an election `E-TEST` (Test Election) that runs from **09:00** to
  **17:00** on 28 October 2026. The base "now" is **10:00**.
- `MutableClock` can move the clock by an exact duration, which makes start/end boundary tests
  deterministic without sleeping.
- Each test class removes its temporary directory in `@AfterEach`.

---

## 2. Automated test cases

Legend: **Inputs** lists the key data used; every test also uses the common fixture above.
"PASS" means the test was executed by Maven in this environment and reported no failure.

### 2.1 AuthenticationServiceTest

| ID | Test method | Preconditions / inputs | Expected result | Actual |
| --- | --- | --- | --- | --- |
| A01 | `successfulVoterAuthentication` | Register V100 / "Test Voter" / PIN 4826, eligible | Returned voter has ID V100, name Test Voter, eligible, not voted | PASS |
| A02 | `voterIdIsNormalised` | Register V101; sign in with `" v101 "` | Returned ID is `V101` | PASS |
| A03 | `failedVoterAuthentication` | V102 with PIN `9999` (correct PIN 4826) | `AuthenticationException` with exactly "Invalid voter ID or PIN." | PASS |
| A04 | `unknownVoterIsRejected` | ID V999 does not exist | Same generic message as A03 (no user enumeration) | PASS |
| A05 | `blankCredentialsAreRejected` | Empty ID and PIN | `AuthenticationException` | PASS |
| A06 | `successfulAdministratorAuthentication` | `admin` / `Admin@123` | Returned username `admin` | PASS |
| A07 | `failedAdministratorAuthentication` | `admin` / `wrong-password` | `AuthenticationException` | PASS |
| A08 | `pinIsNeverStoredInPlainText` | Register V103 with PIN 4826, reload | Hash ≠ PIN, hash does not contain "4826", salt not blank, hash ≠ salt, `toString()` contains neither hash nor salt | PASS |

### 2.2 VotingWorkflowTest

| ID | Test method | Preconditions / inputs | Expected result | Actual |
| --- | --- | --- | --- | --- |
| V01 | `successfulVoteSubmission` | V001/4826 eligible, candidate C001, now 10:00 | Vote returned with correct election, voter, candidate, `votedAt` = clock; ID > 0; DB count 1; V001 `hasVoted` | PASS |
| V02 | `votingAtStartTimeIsAllowed` | Clock = 09:00:00 (exact start) | Vote accepted; 1 ballot | PASS |
| V03 | `rejectionBeforeElectionStarts` | Clock = 08:59 | `ElectionWindowException` "Voting has not started yet…"; 0 ballots; voter not marked | PASS |
| V04 | `rejectionAtElectionEndTime` | Clock = 17:00:00 (exact end, exclusive) | `ElectionWindowException` "Voting has closed…"; 0 ballots | PASS |
| V05 | `votingBeforeEndTimeIsAllowed` | Clock = 16:59:59 | Vote accepted; 1 ballot | PASS |
| V06 | `rejectionOfRepeatVote` | V006 votes once, attempts again | `DuplicateVoteException` containing "already cast a vote"; still 1 ballot | PASS |
| V07 | `rejectionOfUnregisteredVoter` | V999 does not exist | `ValidationException` "not registered"; 0 ballots | PASS |
| V08 | `rejectionOfIneligibleVoter` | V008 registered with `eligible=false` | `ValidationException` "not eligible"; 0 ballots | PASS |
| V09 | `rejectionOfInvalidCandidate` | Candidate C999 does not exist | `ValidationException` "does not exist"; 0 ballots; voter not marked | PASS |
| V10 | `databaseRejectsDuplicateBallot` | Insert ballot directly, then reset `voters.has_voted = 0`, insert again | Second insert raises `DuplicateVoteException`; still 1 ballot (UNIQUE constraint works independently of the flag) | PASS |
| V11 | `simultaneousSubmissionsProduceOneVote` | Two `AppContext` instances on the same DB, two threads, same voter V011 | Exactly one success; exactly 1 ballot in the election and in total | PASS |
| V12 | `voteTransactionRollsBackOnFailure` | SQLite trigger aborts `has_voted` update | `DataAccessException`; 0 ballots; `hasVoted` false (insert rolled back) | PASS |
| V13 | `voteTimestampUsesInjectedClock` | Advance clock by 20 minutes | `votedAt` = 10:20 | PASS |
| V14 | `votedStatusIsPersisted` | Vote, reopen the application context | Voter V014 still `hasVoted` in the new connection | PASS |

### 2.3 ResultsServiceTest

Common fixture: candidates C001 "Candidate Alpha", C002 "Candidate Beta", C003 "Candidate Gamma".

| ID | Test method | Preconditions / inputs | Expected result | Actual |
| --- | --- | --- | --- | --- |
| R01 | `zeroVotesAreHandledSafely` | No voters, no votes | 0 votes, 0 eligible, turnout 0.0, `hasVotes=false`, no tie, no winners, text "No votes recorded", label "No result yet", every candidate 0 / 0.0% | PASS |
| R02 | `candidateCountsAndPercentages` | 3 voters: C001, C001, C002 | 3 votes, 3 eligible, turnout 100.0%, C001 2 (66.6667%), C002 1 (33.3333%), C003 0; sorted C001 first | PASS |
| R03 | `winnerIsDeterminedCorrectly` | C002×3, C003×1 | No tie; winner "Candidate Beta"; text "Winner: Candidate Beta"; label "Winner" | PASS |
| R04 | `tieIsDeterminedCorrectly` | C001×1, C003×1 | `tie=true`, two winner names, label "Tie", text starts "Tie between ", each 50.0% | PASS |
| R05 | `turnoutPercentageIsCorrect` | 3 eligible + 1 ineligible; 2 votes | Eligible = 3, votes = 2, turnout 66.6667% (ineligible excluded from denominator) | PASS |
| R06 | `resultsAreSortedByVotes` | C003×2, C001×1, C002×0 | Order C003, C001, C002 | PASS |
| R07 | `resultStatusUsesInjectedClock` | Clock = end + 5 minutes | Status CLOSED; `generatedAt` equals that instant | PASS |

### 2.4 ReportExporterTest

Common fixture: 2 voters, C001 and C002 with one vote each (a tie).

| ID | Test method | Inputs | Expected result | Actual |
| --- | --- | --- | --- | --- |
| E01 | `textReportIsWritten` | Export TXT to a new `reports/` subfolder with notice "AUTOMATED TEST REPORT" | File exists; contains header, "Election: Test Election", notice, both candidate names, "Votes cast (valid): 2", "Turnout: 100.0%", "Tie between Candidate Alpha, Candidate Beta", disclaimer and "Report generated:" | PASS |
| E02 | `csvReportIsWritten` | Export CSV | File starts with bytes `EF BB BF`; contains candidate header, `C001,Candidate Alpha,Test Party,1,50.0`, `C002,Candidate Beta,Test Party,1,50.0`, eligible 2, votes 2, turnout 100.0, tie text and disclaimer | PASS |
| E03 | `csvEscapesSpecialCharacters` | Candidate "Patel, Asha" / "Unity, Party" | CSV contains `"Patel, Asha","Unity, Party",0,0.0` (fields quoted because of commas) | PASS |
| E04 | `zeroVoteReportIsSafe` | Separate empty environment with one candidate; TXT and CSV | No division by zero: "Turnout: 0.0%", `Turnout (%),0.0`, candidate row `...,0,0.0` | PASS |
| E05 | `exportFailureIsReportedGracefully` | Destination path whose parent is a regular file | `ExportException` (friendly), no crash | PASS |
| E06 | `textReportUsesUtf8` | Notice "Café — résumé" | File begins with UTF-8 "VOTESECURE" bytes; non-ASCII notice round-trips | PASS |

### 2.5 AdminServiceTest

| ID | Test method | Inputs | Expected result | Actual |
| --- | --- | --- | --- | --- |
| D01 | `voterRegistrationAndSearch` | Register V100 Ananya Rao, V200 Vikram Singh; search "ananya" and "V2" | One match each (V100, V200); registered count 2 | PASS |
| D02 | `duplicateVoterIdIsRejected` | Register V101 twice | `DuplicateEntityException`; count stays 1 | PASS |
| D03 | `duplicateCandidateIdIsRejected` | Add C101 twice | `DuplicateEntityException`; candidate count stays 1 | PASS |
| D04 | `invalidVoterInputIsRejected` | ID `bad id!`; name `X`; PIN `12` | `ValidationException` for each | PASS |
| D05 | `invalidElectionTimeRangeIsRejected` | End before start; end equal to start | `ValidationException` both times; stored schedule unchanged | PASS |
| D06 | `electionSettingsCanBeUpdatedBeforeVoting` | New name, start +5 min, end +2 days | Returned and re-read schedule match; name updated | PASS |
| D07 | `configurationLocksAfterFirstVote` | Vote V104→C104, then add candidate, update settings, delete C104, update C104 | Every operation throws `ConfigurationLockedException` | PASS |
| D08 | `candidateLifecycleBeforeVoting` | Add C106, update, delete | Updated name read back; record absent after delete | PASS |
| D09 | `administratorPasswordCanBeChanged` | `Admin@123` → `NewPass@456` | Old password fails; new password authenticates | PASS |
| D10 | `administratorPasswordChangeRequiresCurrentPassword` | Wrong current password; weak new password; same-as-old password | `AuthenticationException`; `ValidationException`; `ValidationException` | PASS |
| D11 | `dataPersistsAfterReconnecting` | Register voter + candidate, vote, reopen context | Voter `hasVoted`, 1 ballot total, candidate present; second vote via reopened context is rejected | PASS |

### 2.6 PasswordHasherTest

| ID | Test method | Inputs | Expected result | Actual |
| --- | --- | --- | --- | --- |
| H01 | `hashVerification` | Hash "Admin@123"; verify the same password, a wrong password and a wrong salt | True for the correct credential; false for the wrong password and for the correct password with a different salt | PASS |
| H02 | `saltsAreUnique` | Two salts, same password 4826 | Salts differ; hashes differ | PASS |
| H03 | `hashDoesNotContainRawPassword` | Hash "4826" | Hash ≠ "4826", does not contain it, length > 40 | PASS |
| H04 | `pinValidation` | `4826` accepted; `12`, `abcd`, `12 34` rejected | Accept/reject as listed | PASS |
| H05 | `passwordValidation` | `Strong@123` accepted; `weak`, `alllowercase1!`, `NoDigits!!` rejected | Accept/reject as listed | PASS |

### 2.7 ElectionTest

| ID | Test method | Inputs | Expected result | Actual |
| --- | --- | --- | --- | --- |
| M01 | `statusBoundaries` | Window 09:00–17:00; times 08:59:59, 09:00:00, 16:59:59, 17:00:00 and the next day | NOT_STARTED, OPEN, OPEN, CLOSED, CLOSED | PASS |
| M02 | `invalidRangeIsRejectedByTheModel` | Construct Election with end equal to start and with end before start | `IllegalArgumentException` in both cases | PASS |
| M03 | `invalidRangeIsRejectedByValidation` | `Validation.requireChronologicalWindow` with end equal to start and end one year before | `ValidationException` both times | PASS |
| M04 | `windowUpdateKeepsIdentity` | `withWindow("Renamed Election", start+1 day, end+2 days)` | Same election ID `E1`; new name returned | PASS |

### 2.8 DemoDataTest

Fixed clock: 28 October 2026 10:00; empty temporary database.

| ID | Test method | Expected result | Actual |
| --- | --- | --- | --- |
| S01 | `firstLaunchCreatesCompleteDemoData` | DB file exists; 10 voters; 1 election; 3 candidates for E2026; 0 votes; 10 eligible | PASS |
| S02 | `demoElectionIsOpen` | Name "Student Council Election 2026"; start < end; status OPEN; voting open at the fixed clock | PASS |
| S03 | `demoCredentialsWork` | `admin`/`Admin@123` authenticates; `V001`/`1234` authenticates as "Ananya Rao"; 10 voters | PASS |
| S04 | `demoPasswordIsHashed` | `admins.password_hash` ≠ "Admin@123", does not contain it; salt not blank | PASS |

---

## 3. UI verification checks

These checks were performed against the real Swing application launched by
`com.votesecure.tools.ScreenshotTool` on an isolated demonstration database. Navigation was
driven through the application's own public navigation methods; the resulting screens were
captured with `java.awt.Robot` and visually inspected. They are **not** a substitute for the
automated service tests above.

| ID | Check | Method / evidence | Observed result | Status |
| --- | --- | --- | --- | --- |
| U01 | Application starts and creates an isolated database | `ScreenshotTool` run; console output | Window opened; 12 captures written | PASS |
| U02 | Welcome screen shows both roles and demo notice | `01_welcome.png` | Voter and Administrator cards visible; prototype disclaimer shown | PASS |
| U03 | Voter sign-in screen fields and demo hint | `02_voter_login.png` | ID and PIN fields, sign-in button, back link visible | PASS |
| U04 | Administrator sign-in screen | `03_admin_login.png` | Username/password fields and demo hint visible | PASS |
| U05 | Administrator dashboard statistics | `04_admin_dashboard.png` | Cards show counts, election window and status | PASS |
| U06 | Voter management list and search | `05_voter_management.png` | 10 voters listed with eligible/voted columns; registration form present | PASS |
| U07 | Candidate management (locked state) | `06_candidate_management.png` | 3 candidates with vote counts; lock notice; action buttons disabled | PASS |
| U08 | Election settings (locked state) | `07_election_settings.png` | Window, current time, lock reason; Save/Open-now disabled | PASS |
| U09 | Export report screen | `08_export_report.png` | Preview, TXT/CSV buttons and disclaimer visible | PASS |
| U10 | Results screen live label | `09_results.png` | "LIVE PREVIEW" badge; 6 votes, 60.0% turnout, winner shown | PASS |
| U11 | Voter dashboard after sign-in | `10_voter_dashboard.png` | Voter name/ID, election window, voting status | PASS |
| U12 | Ballot lists all candidates and disables submit until selection | `11_ballot.png` | 3 candidates; "No candidate selected yet."; submit button disabled | PASS |
| U13 | Success screen after a saved ballot | `12_vote_success.png` | Green check, "Vote recorded successfully", timestamp, no candidate name | PASS |
| U14 | Sample TXT report content | Open `reports/Sample_Results.txt` | SAMPLE notice, 10 eligible, 6 votes, 60.0%, winner, disclaimer | PASS |
| U15 | Sample CSV report encoding | Open `reports/Sample_Results.csv` in a UTF-8 editor | BOM present, quoted fields, candidate table, disclaimer | PASS |

### What was not verified through mouse/keyboard automation

- The confirmation dialog's **Cancel** path and the file chooser's **Cancel** path were not
  driven by an automated UI test. The underlying service behaviour (no vote when the service is
  not called; no file when no destination is chosen) is covered by design and by tests.
- Voter results while the election is open were not exercised through a live voter session;
  the visibility rule is implemented in `ResultsPanel` and was inspected in code, while the
  administrator live-preview variant is shown in `09_results.png`.
- Keyboard-only navigation and screen-reader behaviour were not formally tested.

These are stated as limitations rather than claimed as passed.

---

## 4. Manual end-to-end script (recommended before the viva)

Use a fresh database so no votes exist:

```bat
java -Dvotesecure.db=C:\temp\viva.db -jar target\VoteSecure.jar
```

| Step | Action | Expected |
| --- | --- | --- |
| 1 | Sign in as `admin` / `Admin@123` | Dashboard with 10 voters, 3 candidates, 0 votes |
| 2 | Register `V011` / "Demo Student" / PIN `2468` | Row appears in voter table |
| 3 | Add candidate `C004` / "Demo Candidate" / "Demo Party" | Candidate appears in table |
| 4 | Election settings, set end to "now + 1 day", save | Success message; window updated |
| 5 | Sign out, sign in as `V011` / `2468` | Voter dashboard shows "Voting open" |
| 6 | Cast vote → select C001 → Review and submit → Confirm | Success screen with timestamp |
| 7 | Return to dashboard | "You have already voted"; ballot disabled |
| 8 | Try to open the ballot again | Blocked with "already cast a vote" |
| 9 | Sign out; admin sign in; open Results | 1 vote; LIVE PREVIEW; percentages consistent |
| 10 | Export TXT and CSV to Desktop | Files saved; contents match the screen |
| 11 | Candidate management / Election settings | Both locked with the first-vote explanation |
| 12 | Close and restart; sign in as V011 | Still marked as voted; results unchanged |

Delete `C:\temp\viva.db*` afterwards to repeat.

---

## 5. Notes on test quality

- Boundary conditions are tested at the exact start instant, one second before the end and the
  exact end instant, matching the documented `start <= now < end` rule.
- Duplicate prevention is tested twice: through the service and directly against the database
  with the `has_voted` flag deliberately corrupted.
- Concurrency is tested with two independent application contexts and two threads; the test
  asserts exactly one success and one row.
- Rollback is forced with a temporary SQLite trigger rather than a mock, so the real transaction
  path is exercised.
- Report tests read the written bytes, not just the in-memory string, and verify the UTF-8 BOM,
  quoting and a non-ASCII notice.
- Test assertions compare exact messages/counts where the message is part of the user contract,
  and use tolerances only for floating-point percentages.

---

*Executed test evidence: `target/surefire-reports/` (generated by Maven; not included in the ZIP
because `target/` is a build artifact and is excluded by `.gitignore`).*
