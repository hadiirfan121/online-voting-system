# VoteSecure — Online Voting System

**Semester 3 Object-Oriented Programming in Java — academic project**

VoteSecure is a desktop voting application built with Java 17, Swing, JDBC and an embedded
SQLite database. It runs entirely on one Windows computer: no server, no internet connection
and no cloud account are required after the first Maven build.

> **Academic prototype notice.** VoteSecure is a teaching project, not a production-grade
> Internet voting platform. It does not provide voter-verifiable paper trails, end-to-end
> cryptographic verification, or protection against an administrator who has full access to the
> database. Do not use it for a real public election.

---

## Contents

1. [Features](#features)
2. [Screens](#screens)
3. [Requirements](#requirements)
4. [Build and run on Windows](#build-and-run-on-windows)
5. [Demo accounts](#demo-accounts)
6. [How to use the application](#how-to-use-the-application)
7. [Where data is stored](#where-data-is-stored)
8. [Project structure](#project-structure)
9. [Tests](#tests)
10. [Documentation](#documentation)
11. [Security and privacy limits](#security-and-privacy-limits)
12. [Troubleshooting](#troubleshooting)

---

## Features

- 12 working screens: welcome, voter sign-in, voter dashboard, ballot, confirmation, success,
  results, administrator sign-in, administrator dashboard, voter management, candidate
  management, election settings and report export.
- Object-oriented layered design: models, repositories, services, database, Swing UI,
  utilities and custom exceptions.
- SQLite schema created automatically on first launch with primary keys, foreign keys,
  `NOT NULL`, `CHECK` constraints and `UNIQUE(election_id, voter_id)`.
- Salted PBKDF2 password/PIN hashes (120,000 iterations, SHA-256) — no plain-text credentials.
- One vote per voter enforced in the service layer **and** by the database.
- Transactional vote insert with rollback on failure.
- Election window uses `start <= now < end` (the end time is exclusive).
- Configuration (schedule, candidates) freezes once the first ballot is recorded.
- Results calculated from stored ballots, including turnout, percentages, ties and zero votes.
- TXT and CSV report export with UTF-8 encoding and an explicit disclaimer.
- 59 automated JUnit 5 tests using isolated temporary databases and an injectable clock.

## Screens

Genuine screenshots captured from the running application are in
[`docs/screenshots/`](docs/screenshots/):

| File | Screen |
| --- | --- |
| `01_welcome.png` | Welcome / role selection |
| `02_voter_login.png` | Voter sign-in |
| `03_admin_login.png` | Administrator sign-in |
| `04_admin_dashboard.png` | Administrator dashboard and statistics |
| `05_voter_management.png` | Register and search voters |
| `06_candidate_management.png` | Candidate management |
| `07_election_settings.png` | Election schedule and configuration lock |
| `08_export_report.png` | Export report screen |
| `09_results.png` | Results with live-preview label |
| `10_voter_dashboard.png` | Voter dashboard |
| `11_ballot.png` | Candidate listing / ballot |
| `12_vote_success.png` | Vote confirmation receipt |

The screenshots were produced by `com.votesecure.tools.ScreenshotTool` from an isolated
demonstration database, so they contain sample names and sample ballots only.

## Requirements

| Item | Version used for this submission |
| --- | --- |
| Operating system | Windows 10/11 (64-bit) |
| JDK | 25.0.4.1 (compiles to Java 17 bytecode; Java 17+ required) |
| Apache Maven | 3.10.0 |
| Libraries | SQLite JDBC 3.46.1.0, JUnit Jupiter 5.10.2 (downloaded by Maven) |

The first build needs an internet connection so Maven can download these libraries. After that
the application runs offline.

## Build and run on Windows

Open **Command Prompt** in the `VoteSecure` folder (the folder containing `pom.xml`).

```bat
cd C:\path\to\VoteSecure

mvn clean test
mvn package
java -jar target\VoteSecure.jar
```

`mvn package` runs the tests again and produces the executable fat JAR
`target\VoteSecure.jar` (about 14 MB, includes the SQLite driver).

### One-click launcher

Double-click **`run.bat`**. It checks for Java, builds the JAR on first launch if Maven is
available, and then starts the application with `javaw`. A Linux/macOS shell equivalent is
provided as `run.sh`.

To start the application from the JAR directly:

```bat
java -jar target\VoteSecure.jar
```

To run against a specific database file (useful for a fresh demonstration):

```bat
java -Dvotesecure.db=C:\temp\demo.db -jar target\VoteSecure.jar
```

## Demo accounts

These accounts are seeded **only when the database is empty**. They are demonstration
credentials and must never be used for a real election.

| Role | Username / Voter ID | Password / PIN |
| --- | --- | --- |
| Administrator | `admin` | `Admin@123` |
| Voter 1 | `V001` | `1234` |
| Voter 2 | `V002` | `1234` |
| … | `V003` … `V010` | `1234` |

Ten sample voters (`V001`–`V010`), three candidates (`C001` Aarav Sharma / Progress Party,
`C002` Meera Iyer / Unity Party, `C003` Rohan Verma / Reform Party) and one open election
(`E2026`, Student Council Election 2026) are created on first launch. **No sample votes are
inserted** — the live database starts with zero ballots.

The administrator can change the demo password from the dashboard. Voter PINs are created by
the administrator when a voter is registered; PINs are stored only as salted hashes and cannot
be displayed again.

## How to use the application

### Administrator

1. On the welcome screen choose **Administrator**.
2. Sign in with `admin` / `Admin@123`.
3. **Voter management** — register voters (voter ID, full name, initial 4–12 digit PIN,
   eligibility) and search the roll. Duplicate IDs are rejected by the database.
4. **Candidate management** — add, edit or delete candidates while the ballot is unlocked.
5. **Election settings** — set the election name, start and end date/time. The
   **Open now for 7 days** button fills in a convenient demonstration window. Once the first
   vote is recorded (or the end time passes) these controls are locked.
6. **Results** — live counts labelled `LIVE PREVIEW` while voting is open, or the final
   outcome after the election closes.
7. **Export report** — choose TXT or CSV, then pick a destination with the file chooser.
8. **Change password** — change the demonstration administrator password.

### Voter

1. Choose **Voter** on the welcome screen and sign in with an ID and PIN issued by the
   administrator.
2. The dashboard shows the election window, the voter's status and whether results are
   published.
3. Choose **Cast vote**, select exactly one candidate and press **Review and submit vote**.
4. Confirm the choice in the dialog. The success screen shows the saved ballot time but never
   the selected candidate.
5. Trying to vote again is rejected with "This voter has already cast a vote."

### Results visibility

Public results are hidden while voting is open; the voter sees a notice instead. The
administrator can always view live results, clearly labelled as a preview. After the election
ends, the results screen shows the winner, a tie notice, or a "no votes" notice.

## Where data is stored

By default the SQLite database is created at:

```
C:\Users\<your-name>\.votesecure\votesecure.db
```

The directory is created automatically. The database file is **not** part of the submission and
is excluded by `.gitignore`; it appears only after the application runs.

To reset the demonstration (delete all voters, candidates and votes), close the application and
delete that file. The next launch recreates the schema and the demo records. Back up the file
first if you want to keep a record of a demonstration.

## Project structure

```
VoteSecure/
├─ pom.xml
├─ README.md
├─ run.bat, run.sh
├─ src/main/java/com/votesecure/
│  ├─ VoteSecureApp.java          application entry point
│  ├─ AppContext.java             dependency wiring
│  ├─ model/                      Voter, Candidate, Vote, Election, ResultsSummary, …
│  ├─ exception/                  custom checked exceptions
│  ├─ util/                       PasswordHasher, Validation, DateTimeUtils, LoggingConfig
│  ├─ database/                   DatabaseManager, schema, demo seed
│  ├─ repository/                 interfaces + JDBC implementations
│  ├─ service/                    authentication, election, voting, admin, results, export
│  ├─ ui/                         Swing screens, theme and reusable components
│  └─ tools/                      report/screenshot generators (not part of the UI flow)
├─ src/test/java/com/votesecure/  JUnit 5 tests and test support
├─ docs/                          design document, manual, test cases, report, viva notes,
│                                 presentation and screenshots
└─ reports/                       generated sample TXT and CSV reports
```

## Tests

```bat
mvn test
```

The suite covers authentication, the voting window boundaries, duplicate votes, database-level
uniqueness, transaction rollback, results/ties/turnout, report content and encoding, voter and
candidate management, configuration freezing, password hashing and demo-data creation. Each
test class uses its own temporary database and a `MutableClock`, so tests never touch the real
application database.

Latest executed result on this machine (JDK 25.0.4.1, Maven 3.10.0):

```
Tests run: 59, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

See [`docs/Test_Cases.md`](docs/Test_Cases.md) for the detailed test-case table.

## Documentation

| Document | Purpose |
| --- | --- |
| [`docs/Design_Document.md`](docs/Design_Document.md) | Architecture, class design, schema, workflows |
| [`docs/User_Manual.md`](docs/User_Manual.md) | Step-by-step setup and usage guide |
| [`docs/Test_Cases.md`](docs/Test_Cases.md) | Test cases, inputs, expected/actual results |
| [`docs/Project_Report.md`](docs/Project_Report.md) | Full academic report |
| [`docs/Viva_Questions.md`](docs/Viva_Questions.md) | 25+ viva questions with answers and a short explanation script |
| [`docs/Project_Presentation.pptx`](docs/Project_Presentation.pptx) | 9-slide presentation with real screenshots |
| [`reports/Sample_Results.txt`](reports/Sample_Results.txt) | Sample text report (labelled demonstration data) |
| [`reports/Sample_Results.csv`](reports/Sample_Results.csv) | Sample CSV report (UTF-8 with BOM) |

The sample reports were generated from an isolated demonstration database containing six
sample ballots and are clearly marked as sample output.

## Security and privacy limits

Implemented:

- Salted PBKDF2 hashes for voter PINs and administrator passwords; constant-time comparison.
- Prepared statements for all SQL; database errors are logged and replaced by generic messages.
- Foreign-key enforcement, unique constraints and transactional vote insertion.
- One vote per voter checked in the service and enforced by the database.
- Ballot choices are not shown on voter-facing screens.

Limits an examiner should know:

- The database stores which voter cast which ballot in order to prevent duplicate voting, so the
  application cannot guarantee ballot secrecy against someone with database access.
- Results are computed live from the single database; there is no independent audit log.
- Local files and the SQLite database are protected only by operating-system permissions.
- The administrator account is a single local account with a seeded demonstration password.
- There is no network transport, so no encryption in transit is applicable or provided.

## Troubleshooting

| Symptom | Cause and fix |
| --- | --- |
| `java` is not recognised | Install JDK 17+ and enable "Add to PATH"; reopen the terminal. |
| `mvn` is not recognised | Install Maven, or run `run.bat` after building once on another machine. |
| Build cannot download libraries | The first build needs internet access. |
| "The application database could not be opened." | The data folder is read-only or another process holds the file. Close other copies and retry. |
| "Voting has closed." | The current time is at or past the election end time. An administrator can set a new window only if no vote exists. |
| "This voter has already cast a vote." | Each voter may vote once per election; this is expected. |
| Window is larger than the screen | The screens scroll; maximise the window or use a larger display scale. |

---

*VoteSecure is submitted as an academic local prototype for Semester 3 Object-Oriented
Programming in Java. It is not an official election system.*
