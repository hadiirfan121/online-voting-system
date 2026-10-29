# Online Voting System Using Object-Oriented Programming in Java

## Project Report

**Project title:** VoteSecure — Online Voting System
**Course:** Object-Oriented Programming in Java — Semester 3
**Academic year:** 2026–27
**Submission date:** 30 October 2026

| Field | Details |
| --- | --- |
| Student name | **[Your Full Name]** |
| Roll number / registration number | **[Your Roll Number]** |
| Programme / branch | **[B.Tech / BCA / B.Sc. — Branch]** |
| Semester / section | 3 / **[Section]** |
| College / institute | **[College Name]** |
| University | **[University Name]** |
| Internal guide | **[Guide Name and Designation]** |
| Department | **[Department Name]** |

> Replace the bracketed placeholders with your personal details before printing or submitting.
> The technical chapters are complete and describe the application exactly as implemented.

---

## Declaration

I hereby declare that this project report entitled **"VoteSecure — Online Voting System"** is
my own work, carried out as part of the Semester 3 Object-Oriented Programming course. The
application, tests and documentation were written by me. Where external libraries are used
(SQLite JDBC, JUnit 5, Maven, Swing/Java standard library), they are acknowledged in the
references. This project has not been submitted for any other course or degree.

**Place:** [City]
**Date:** 30 October 2026
**Signature:** ____________________  ([Your Full Name])

---

## Certificate

This is to certify that **[Your Full Name]**, bearing roll number **[Your Roll Number]**, of
**[College Name]**, has satisfactorily completed the project **"VoteSecure — Online Voting
System"** for the Semester 3 Object-Oriented Programming course during the academic year
2026–27.

**Internal Guide:** ____________________
**Head of Department:** ____________________
**External Examiner:** ____________________
**Date:** ____________________

---

## Acknowledgement

I thank my internal guide, **[Guide Name]**, for guidance on object-oriented design and for
reviewing the class structure. I also thank the department for providing the laboratory
environment, and my classmates who tested the voter workflow and gave feedback on the screens.
Any remaining shortcomings are my own.

---

## Abstract

VoteSecure is a desktop application that conducts a single election on one computer. It replaces
paper ballots and manual counting with a Java 17 / Swing interface backed by an embedded SQLite
database. The administrator registers voters, prepares candidates, schedules the voting window
and exports results. Voters sign in with an ID and PIN, choose one candidate, confirm the choice
and receive a receipt. The application prevents duplicate voting with a service-level check, a
synchronized submission method, a transactional database write and a
`UNIQUE(election_id, voter_id)` constraint. Time rules use an injected `java.time.Clock`, so
the boundary condition `start <= now < end` is enforced consistently and tested precisely.

The design follows a layered architecture with model, repository, service, database and UI
packages. Repository interfaces abstract persistence, a common exception hierarchy separates
expected failures from programming errors, and immutable value objects keep results consistent.
Credentials are stored as salted PBKDF2-HMAC-SHA256 hashes; SQL uses prepared statements; the UI
shows generic messages for database errors.

The project includes 59 automated JUnit 5 tests using temporary databases and a mutable clock,
genuine screenshots of all screens, sample TXT/CSV reports and user documentation. The report
discusses the results, limitations (notably that ballot secrecy cannot be guaranteed against
full database access) and future work. VoteSecure is an academic local prototype, not a
production-grade Internet voting system.

**Keywords:** online voting, Java, Swing, SQLite, JDBC, object-oriented programming, JUnit,
authentication, transactional integrity.

---

## Table of contents

1. Introduction
2. Literature and background
3. System analysis
4. System design
5. Implementation
6. Testing and results
7. Screenshots
8. Limitations and future work
9. Conclusion
10. References
Appendix A — Project structure
Appendix B — How to run
Appendix C — Test summary

---

## 1. Introduction

### 1.1 Background

Small elections in colleges and clubs are frequently conducted with paper slips or shared
spreadsheets. Both approaches break down when the number of voters grows: duplicate entries are
hard to spot, eligibility is informal, counting is manual and the result cannot be reproduced.
A small database application can enforce the rules mechanically and produce a consistent report.

### 1.2 Problem statement

There is a need for a simple, offline tool that:

- identifies each voter uniquely,
- accepts exactly one ballot per voter,
- accepts ballots only during a defined window,
- calculates results reliably, and
- produces an exportable record.

### 1.3 Objectives

1. Build a role-based Java desktop application with voter and administrator screens.
2. Enforce eligibility, one-vote-per-voter and the voting window in the service and the database.
3. Protect credentials with salted hashing.
4. Freeze election configuration once voting begins.
5. Generate accurate results and TXT/CSV reports.
6. Demonstrate object-oriented principles, JDBC, exception handling and Swing events.
7. Verify the behaviour with automated tests.

### 1.4 Scope

The application manages **one current election** on a single computer. It does not use a network,
does not provide remote voting and does not attempt cryptographic end-to-end verifiability. These
boundaries are deliberate and are discussed in Chapter 8.

### 1.5 Organisation of the report

Chapter 2 reviews related concepts. Chapter 3 analyses requirements and risks. Chapter 4 presents
the architecture, schema and workflows. Chapter 5 describes implementation details. Chapter 6
reports testing and results. Chapter 7 shows screenshots. Chapters 8 and 9 discuss limitations,
future work and conclusions.

---

## 2. Literature and background

### 2.1 Electronic voting categories

Electronic voting systems range from **poll-site machines** with paper trails, through
**kiosk-based electronic ballot marking**, to **Internet voting** with remote authentication.
The security requirements differ sharply: Internet voting must resist network attackers,
malware on voter devices and coercion, and normally requires end-to-end cryptographic
verification. A local classroom application can reasonably demonstrate the *workflow* and
*database integrity* aspects while explicitly not claiming cryptographic guarantees.

### 2.2 Why Java and Swing

Java offers a mature standard library for desktop interfaces (Swing), date/time handling
(`java.time`), cryptography (`javax.crypto`), file I/O (`java.nio.file`) and database access
(JDBC). Swing is included in the JDK, so the project has no UI framework installation
requirement beyond Java itself. The same code base runs on any desktop with a JDK, which suits a
course submission.

### 2.3 Why SQLite

SQLite is an embedded relational database stored in a single file. It requires no server, no
separate installation and no configuration, yet supports transactions, foreign keys, unique
constraints and SQL indexes. For a single-machine election it provides real relational integrity
without the deployment burden of a database server. The Xerial `sqlite-jdbc` driver exposes it
through standard JDBC, which is the technology specified in the course.

### 2.4 Related academic concepts

- **ACID transactions:** a ballot insert and the voter's `has_voted` flag must succeed or fail
  together.
- **Defence in depth:** a rule should not rely on a single check; the UI, service and database
  each contribute.
- **Salt and key stretching:** storing a fast hash of a short PIN is insufficient; PBKDF2 with a
  random salt and many iterations raises the cost of offline guessing.
- **Dependency inversion:** services depend on repository interfaces rather than concrete SQL
  classes.

---

## 3. System analysis

### 3.1 Existing system and drawbacks

| Existing approach | Drawback |
| --- | --- |
| Paper ballot box | Manual counting; no automatic duplicate detection; easy to lose slips |
| Spreadsheet shared on one PC | Accidental edits; formulas can be changed; no audit of who voted |
| Online form without controls | Duplicate submissions; time-zone ambiguity; results visible early |

### 3.2 Proposed system

VoteSecure stores voters, candidates, one election and ballots in SQLite. The administrator
controls the roll and the ballot. Voters authenticate with an ID and PIN issued by the
administrator. The application decides on the server-side rules (in the service layer) whether a
ballot is acceptable, then writes it in a transaction. Results are computed from stored rows and
exported on demand.

### 3.3 Functional requirements

| ID | Requirement |
| --- | --- |
| FR1 | The system shall provide separate voter and administrator sign-in. |
| FR2 | The administrator shall register, list and search voters with unique IDs and hashed PINs. |
| FR3 | The administrator shall add, edit and delete candidates before voting starts. |
| FR4 | The administrator shall set the election name, start and end date/time. |
| FR5 | A voter shall see the ballot only when signed in. |
| FR6 | A voter shall select exactly one candidate and confirm before submission. |
| FR7 | The system shall accept at most one ballot per voter per election. |
| FR8 | The system shall accept ballots only when `start <= now < end`. |
| FR9 | The system shall freeze schedule and candidate changes after the first ballot. |
| FR10 | The system shall calculate counts, percentages, turnout, winner and tie. |
| FR11 | The system shall export results as TXT and CSV with UTF-8 encoding. |
| FR12 | The administrator shall be able to change the administrator password. |
| FR13 | Public results shall be hidden while voting is open. |
| FR14 | The system shall display clear, non-sensitive error messages. |

### 3.4 Non-functional requirements

| ID | Requirement |
| --- | --- |
| NFR1 | Runs on Windows 10/11 with Java 17+ and no server. |
| NFR2 | Database created automatically on first launch. |
| NFR3 | Ballot save completes within ordinary desktop response time. |
| NFR4 | UI remains usable at 1280×720 through scrolling. |
| NFR5 | No credential or raw SQL is logged or shown to users. |
| NFR6 | Code is organised into packages with a clear dependency direction. |
| NFR7 | Behaviour is covered by repeatable automated tests. |
| NFR8 | Documentation states clearly that this is an academic prototype. |

### 3.5 Feasibility

- **Technical:** all components are standard Java or a single JDBC dependency; no server hosting.
- **Economic:** zero licensing cost; runs on existing laboratory computers.
- **Operational:** one administrator and voters use a familiar desktop UI; no training beyond the
  user manual.
- **Schedule:** the project was completed within the semester timeline; the final build and test
  run are reproducible with two Maven commands.

### 3.6 Risk analysis

| Risk | Likelihood | Impact | Mitigation |
| --- | --- | --- | --- |
| Duplicate ballot through double-click or second window | Medium | High | Synchronized method + unique constraint + rollback |
| Time-zone confusion at the deadline | Medium | Medium | Local-time display everywhere; explicit time-zone note; end-exclusive rule documented |
| Weak demonstration PINs used in a real setting | High if misused | High | Prominent disclaimers; password change feature; documented limitation |
| Database file deleted or corrupted | Low | High | Reports can be exported; reset instructions; no silent destructive button |
| SQL injection through search/forms | Low | High | Prepared statements and validation |
| Ballot secrecy complaint | Medium | Medium | Limitation stated; voter screens never show choice; reports aggregate only |

---

## 4. System design

### 4.1 Architecture

The application is layered:

- **UI (`ui`)** — Swing screens, dialogs, theme, table models. Calls services; contains no SQL.
- **Service (`service`)** — use cases and rules: authentication, election status, voting,
  administration, results and reporting.
- **Repository (`repository`)** — interfaces plus JDBC implementations; the only code that
  executes SQL.
- **Database (`database`)** — connection creation, pragmas, schema and demonstration seed.
- **Model, exception, util** — domain objects, failure types and helpers.

`AppContext` wires the graph at startup. `VoteSecureApp` configures logging and the look and
feel, initialises the database and opens `MainFrame` on the Event Dispatch Thread.

### 4.2 Class responsibilities

| Class | Responsibility |
| --- | --- |
| `Voter` | Immutable voter record; exposes eligibility and voted status |
| `Candidate` | Candidate in an election |
| `Election` | Window and `statusAt` rule (`start <= now < end`) |
| `Vote` | A saved ballot record |
| `ResultsSummary` / `CandidateResult` | Result snapshot and per-candidate figures |
| `Administrator` | Admin identity with hashed password |
| `VotingManager` | Orchestrates the voting use case and report export |
| `ElectionService` | Current election, status, configuration lock |
| `ResultsService` | Counts, percentages, turnout, winner/tie |
| `AdminService` | Voter/candidate management, schedule, password change |
| `AuthenticationService` | PIN/password verification with generic failures |
| `ReportExporter` | Builds and writes TXT/CSV |
| `DatabaseManager` | Database path, connection, schema, seed |
| `Jdbc*Repository` | Prepared-statement persistence |
| `PasswordHasher` | PBKDF2 hashing and constant-time verification |
| `Validation` | Central input rules |
| `DateTimeUtils` | Display/input/storage formats |
| `MainFrame` | Card-based navigation and session state |
| 12 panels | Screens listed in Chapter 7 |
| `Theme`, `UiFactory`, `FlatButton`, `CardPanel`, `StatusBadge`, … | Consistent presentation |

### 4.3 Database schema

Five tables: `elections`, `voters`, `candidates`, `votes`, `admins`. Key constraints:

- Primary keys on every table.
- Foreign keys from `candidates` and `votes` to `elections`; from `votes` to `voters` and
  `candidates`.
- `NOT NULL` on all required columns.
- `CHECK (eligible IN (0,1))`, `CHECK (has_voted IN (0,1))`,
  `CHECK (must_change_password IN (0,1))`.
- `UNIQUE (election_id, voter_id)` on `votes`.
- `ON DELETE RESTRICT` for ballots; `ON DELETE CASCADE` for candidates when an election is
  deleted (not exposed in the UI).
- Indexes on `candidates(election_id)`, `votes(election_id)`, `votes(candidate_id)`.

A diagram and full column list are in the Design Document (Chapter 8).

### 4.4 Voting sequence

1. Voter signs in; `AuthenticationService` compares the PIN hash.
2. Ballot screen lists candidates for the current election.
3. Voter selects one candidate, presses **Review and submit vote** and confirms.
4. `VotingManager.castVote` normalises IDs and loads the election and voter.
5. It checks eligibility, election status at the injected clock, the duplicate flag and the
   candidate's election.
6. `JdbcVoteRepository.castVote` starts a transaction, inserts the ballot, marks the voter and
   commits; on any SQL error it rolls back.
7. The success screen shows the timestamp but not the chosen candidate.

### 4.5 Configuration freeze

`ElectionService.isConfigurationLocked` returns true when a ballot exists **or** when the end
time has passed. The UI disables schedule/candidate controls and the service throws
`ConfigurationLockedException` if called anyway.

### 4.6 Results logic

- Counts come from the `votes` table.
- Turnout denominator is eligible voters.
- Percentages are per valid vote; zero votes yield 0.0 without division by zero.
- The highest non-zero count wins; several highest counts produce a tie.
- Zero ballots produce "No votes recorded".
- Sorting is descending by votes, then candidate ID.

### 4.7 Reporting

The exporter receives a `ResultsSummary` snapshot plus an optional notice. Text output uses a
fixed-width table; CSV output uses quoting and a UTF-8 BOM. Both include the academic
disclaimer. The destination is chosen through `JFileChooser`; the exporter creates parent
directories and reports an `ExportException` if writing fails.

---

## 5. Implementation

### 5.1 Technology stack

Java 17 language level (built with JDK 25), Swing, JDBC, SQLite 3 via `sqlite-jdbc 3.46.1.0`,
Maven 3.10.0, JUnit 5.10.2, `java.time`, `java.nio.file`, `javax.crypto`.

### 5.2 Project layout

```
src/main/java/com/votesecure/
  VoteSecureApp.java  AppContext.java
  model/ exception/ util/ database/ repository/ service/ ui/ tools/
src/test/java/com/votesecure/
  support/ (TestEnvironment, MutableClock)
  database/ model/ service/ util/ (59 tests)
docs/ (this report, design, manual, test cases, viva, presentation, screenshots)
reports/ (sample TXT and CSV)
```

### 5.3 Database initialisation

`DatabaseManager.createDefault` uses `-Dvotesecure.db` when supplied, otherwise
`~/.votesecure/votesecure.db`. `initialize()` creates the schema, ensures the `admin` account and
seeds the demonstration election, candidates and ten voters only when the database has no
election. Every connection enables `PRAGMA foreign_keys = ON` and a 5-second busy timeout; WAL
mode is set when the schema is created.

### 5.4 Password and PIN handling

`PasswordHasher` generates a 16-byte salt from `SecureRandom`, derives a 256-bit key with
PBKDF2-HMAC-SHA256 and 120,000 iterations, and stores Base64 strings. Verification re-derives
the key and compares with `MessageDigest.isEqual`, which avoids timing leaks. The raw PIN exists
only in the registration request and is never logged or stored.

### 5.5 Transactional voting

`JdbcVoteRepository.castVote` uses a single connection, disables auto-commit, inserts into
`votes`, updates `voters.has_voted`, commits, and restores auto-commit. `SQLException` triggers
`rollback()`. `SqlErrors` translates a unique-constraint failure into `DuplicateVoteException`
and other failures into `DataAccessException`, so the service and UI never parse SQL text.

### 5.6 Swing implementation

- `MainFrame` holds the `CardLayout`, the signed-in voter/administrator and a `showScreen`
  method. Screens implement `Refreshable` and reload when shown.
- The content panel is a custom `Scrollable` container whose preferred size follows the visible
  card, avoiding the "tallest screen" scrollbar problem with `CardLayout`.
- `UiFactory` builds labels, fields, info boxes and buttons; wrapped text uses an HTML
  single-cell table with an explicit pixel width to avoid clipping.
- `FlatButton` and `CardPanel` paint rounded backgrounds and hover/focus states.
- `StatusBadge` shows NOT STARTED / VOTING OPEN / CLOSED / LIVE PREVIEW with semantic colours.
- Tables use a non-editable `SimpleTableModel` and a striped renderer.
- Every user action is handled on the Event Dispatch Thread; long-running database work in this
  small application completes quickly enough to run synchronously.

### 5.7 Error handling strategy

- Expected rule violations use specific checked exceptions and are shown in a message area or
  dialog in plain language.
- Unexpected database failures are logged with a stack trace to the console/log file and shown
  as a generic message.
- Startup failures (for example an unwritable data folder) show a dialog that names the
  application-data folder and the log file, not a stack trace.
- No catch block silently ignores an error; cleanup blocks use best-effort deletion only for
  temporary test directories.

### 5.8 Sample data and tools

`SampleReportsGenerator` creates an isolated database with six ballots and exports the sample
reports under `reports/`. `ScreenshotTool` launches the real UI against another isolated database
and captures 12 screenshots with `Robot`. Neither tool is part of the normal user workflow; they
exist so the submitted evidence can be reproduced.

---

## 6. Testing and results

### 6.1 Strategy

- Unit tests for hashing, validation and the election window.
- Service tests for authentication, voting, results, administration and reporting.
- Database tests for schema creation, demo data and persistence across connections.
- Isolated temporary databases; the real database is never touched.
- `MutableClock` for exact boundary tests.
- Real SQLite triggers for rollback and direct SQL for the duplicate-constraint test.
- Two application contexts and two threads for the concurrency test.

### 6.2 Execution result

Command:

```bat
mvn clean test
```

Observed result on 28 October 2026 (JDK 25.0.4.1, Maven 3.10.0):

```
Tests run: 59, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

| Class | Tests | Result |
| --- | ---: | --- |
| AuthenticationServiceTest | 8 | All passed |
| VotingWorkflowTest | 14 | All passed |
| ResultsServiceTest | 7 | All passed |
| ReportExporterTest | 6 | All passed |
| AdminServiceTest | 11 | All passed |
| PasswordHasherTest | 5 | All passed |
| ElectionTest | 4 | All passed |
| DemoDataTest | 4 | All passed |

Detailed inputs, expected values and actual outcomes are in `docs/Test_Cases.md`.

### 6.3 Build result

`mvn package` produced `target/VoteSecure.jar` (approximately 14 MB including the SQLite
driver). The application was launched from this JAR to capture the screenshots, confirming that
the packaged artifact — not only the IDE classpath — works.

### 6.4 Notable defects found and fixed during development

| Defect | Cause | Fix |
| --- | --- | --- |
| Text clipped inside cards | Swing HTML `<body width>` is a hint and rendered ~130% wide | Wrapped text in a one-cell table with an explicit `td width` |
| Horizontal clipping at 1280 px | Card panel's preferred width exceeded the viewport | Added a `Scrollable` card container tracking viewport width |
| Excessive scrolling on every screen | `CardLayout` preferred height was the tallest card | Preferred size now follows the visible card |
| Status badge stretched vertically | `BoxLayout` expanded the badge to fill height | Capped the badge holder's maximum height |
| Screenshots captured another window | The app was not always-on-top during `Robot` capture | Maximised the frame, set always-on-top, captured content-pane bounds |
| Tests failed on fixture names | Digits in "Voter V001" violated the name rule | Realistic fixture names and matching assertions |

These fixes are part of the delivered code, not planned improvements.

### 6.5 Sample outputs

- `reports/Sample_Results.txt` — 10 eligible voters, 6 sample ballots, 60.0% turnout, winner
  Aarav Sharma, explicit SAMPLE/DEMONSTRATION notice.
- `reports/Sample_Results.csv` — same data, UTF-8 BOM, quoted fields.

The sample ballots exist only in the isolated demonstration database created by
`SampleReportsGenerator`; the normal first launch starts with **zero** votes.

---

## 7. Screenshots

All images are genuine captures from the running application (`docs/screenshots/`).

| Figure | Screen | File |
| --- | --- | --- |
| 7.1 | Welcome and role selection | `01_welcome.png` |
| 7.2 | Voter sign-in | `02_voter_login.png` |
| 7.3 | Administrator sign-in | `03_admin_login.png` |
| 7.4 | Administrator dashboard | `04_admin_dashboard.png` |
| 7.5 | Voter management | `05_voter_management.png` |
| 7.6 | Candidate management | `06_candidate_management.png` |
| 7.7 | Election settings | `07_election_settings.png` |
| 7.8 | Export report | `08_export_report.png` |
| 7.9 | Results (live preview) | `09_results.png` |
| 7.10 | Voter dashboard | `10_voter_dashboard.png` |
| 7.11 | Ballot / candidate listing | `11_ballot.png` |
| 7.12 | Vote success receipt | `12_vote_success.png` |

The screenshots use an isolated demonstration database containing sample voters and six sample
ballots, so the results pages show non-zero figures. The success screen deliberately omits the
candidate name.

---

## 8. Limitations and future work

### 8.1 Limitations

1. **Ballot secrecy vs. duplicate prevention.** The `votes` table links voter and candidate, so
   someone with full database access can reconstruct individual choices. The application hides
   this mapping from all voter screens and exports only aggregates, but confidentiality against
   a database administrator is not guaranteed.
2. **Local trust boundary.** The database and reports are protected only by Windows file
   permissions. Malware or another user account with access to the file can read or alter it.
3. **Single election.** The UI always operates on the most recently created election.
4. **Single administrator.** There is no role hierarchy or multi-admin approval.
5. **No account lockout.** Repeated PIN guessing is slowed by PBKDF2 but not blocked.
6. **No cryptographic receipts.** The success screen is a UI confirmation, not a verifiable
   receipt.
7. **Time zone.** All times use the computer's local zone; the UI states this but does not
   normalise to UTC.
8. **Accessibility.** Keyboard navigation works, but screen-reader labelling is minimal.
9. **Demo credentials.** `admin` / `Admin@123` and PIN `1234` are seeded for demonstration and
   are unsuitable for real use.
10. **Not for real elections.** No independent audit, certification or legal compliance is
    claimed.

### 8.2 Future work

- Separate anonymous ballot storage with cryptographic receipts and a mixing/verification step.
- Multiple elections, each with its own roll, ballot and results.
- Append-only audit log for administrative actions.
- Account lockout and optional two-factor administrator sign-in.
- PDF export and charts.
- Migration to a server database with TLS for multi-machine polling stations.
- Accessibility audit and localisation.

---

## 9. Conclusion

The project delivered a complete, runnable online voting application that satisfies the
functional requirements: 12 usable screens, administrator-controlled registration and ballot
setup, a voter workflow with confirmation, database-enforced single voting, correct results and
TXT/CSV export. Object-oriented design is visible throughout: encapsulated models, repository
abstraction, an exception hierarchy, composition of services and polymorphic UI components.
Persistence uses JDBC with prepared statements, foreign keys and a transaction; credentials are
salted and stretched with PBKDF2.

Verification is reproducible: 59 automated tests pass with `mvn test`, and the packaged JAR was
used to capture the submitted screenshots and sample data. The limitations — especially ballot
secrecy against database access and the local trust boundary — are documented rather than
concealed. Within its stated scope, VoteSecure is a faithful academic demonstration of
object-oriented programming applied to a realistic problem.

---

## 10. References

1. Oracle. *Java Platform, Standard Edition 17 API Specification.*
   <https://docs.oracle.com/en/java/javase/17/docs/api/>
2. Oracle. *The Swing Tutorial.* <https://docs.oracle.com/javase/tutorial/uiswing/>
3. Oracle. *JDBC API.* <https://docs.oracle.com/javase/8/docs/technotes/guides/jdbc/>
4. SQLite Consortium. *SQLite Documentation — Foreign Keys, Transactions, Constraints.*
   <https://www.sqlite.org/docs.html>
5. Xerial. *sqlite-jdbc.* <https://github.com/xerial/sqlite-jdbc>
6. JUnit Team. *JUnit 5 User Guide.* <https://junit.org/junit5/docs/current/user-guide/>
7. Apache Software Foundation. *Apache Maven — Introduction.*
   <https://maven.apache.org/guides/getting-started/>
8. NIST. *Special Publication 800-132: Recommendation for Password-Based Key Derivation.*
   <https://csrc.nist.gov/publications/detail/sp/800-132/final>
9. OWASP Foundation. *Password Storage Cheat Sheet.*
   <https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html>
10. OWASP Foundation. *SQL Injection Prevention Cheat Sheet.*
    <https://cheatsheetseries.owasp.org/cheatsheets/SQL_Injection_Prevention_Cheat_Sheet.html>

---

## Appendix A — Project structure

```
VoteSecure/
├─ pom.xml
├─ README.md
├─ run.bat / run.sh
├─ src/main/java/com/votesecure/   (models, exceptions, utils, database, repositories,
│                                   services, 12 Swing screens, tools)
├─ src/test/java/com/votesecure/   (59 JUnit 5 tests + support)
├─ docs/                           (design, manual, test cases, report, viva, pptx,
│                                   screenshots/)
├─ reports/                        (Sample_Results.txt, Sample_Results.csv)
└─ .gitignore
```

## Appendix B — How to run

```bat
cd C:\path\to\VoteSecure
mvn clean test
mvn package
java -jar target\VoteSecure.jar
```

Or double-click `run.bat`. Demo administrator: `admin` / `Admin@123`. Demo voter: `V001` /
`1234`. Database: `%USERPROFILE%\.votesecure\votesecure.db`.

## Appendix C — Test summary

`mvn test` → 59 tests, 0 failures, 0 errors, 0 skipped. Detailed cases in
`docs/Test_Cases.md`.
