# VoteSecure — Viva Questions and Answers

**Purpose:** preparation for the Semester 3 project viva.
**Scope:** questions are answered against the code and documentation actually delivered.

---

## Part A — Five-minute explanation (spoken script)

> "My project is VoteSecure, an online voting system built in Java with a Swing desktop
> interface and an embedded SQLite database. It runs entirely on one Windows computer, with no
> server, and it is an academic prototype, not a production voting platform.
>
> The application has two roles. An administrator signs in and registers eligible voters, giving
> each a unique voter ID and an initial PIN. The administrator also prepares the candidate list
> and sets the election's start and end time. A voter signs in with the ID and PIN, sees only the
> candidates for the current election, selects one, confirms the choice and receives an on-screen
> receipt.
>
> The main design problem is trust: how do we guarantee that a person votes only once, only
> while voting is open, and that the count is correct? I used defence in depth. The service
> layer checks eligibility, the voting window, whether the voter already voted, and whether the
> candidate belongs to the current election. The database then enforces a unique constraint on
> election plus voter, so even a second window or a double-click cannot create two ballots. The
> insert and the voter's 'has voted' flag are written in one transaction, and any failure rolls
> both back.
>
> The code is organised in layers: Swing screens call services, services use repository
> interfaces, and only the JDBC repositories contain SQL. That separation let me test the
> business rules without a GUI. I injected a Clock so tests can jump to the exact start time,
> one second before the end, and the exact end time. The rule is start-inclusive and
> end-exclusive, which is stated in the documentation and on screen.
>
> Security-wise, PINs and passwords are never stored in plain text. I use PBKDF2 with SHA-256,
> 120,000 iterations and a random 16-byte salt per credential, and constant-time comparison.
> All SQL uses prepared statements. Database errors are logged, but users see only a generic
> message. No credential is logged.
>
> Results are calculated from the stored ballots, including turnout over eligible voters,
> percentages, ties and the zero-vote case. The administrator can export a TXT or CSV report
> through a file chooser; the CSV is UTF-8 with a BOM so Excel shows names correctly. While
> voting is open, results are marked 'live preview' for the administrator and hidden from
> voters.
>
> I verified the behaviour with 59 automated JUnit tests using isolated temporary databases. All
> 59 passed with Maven on this machine. I also captured genuine screenshots of every screen and
> generated sample reports from an isolated demonstration database.
>
> The main limitation is ballot secrecy. Because I store which voter cast which ballot in order
> to prevent duplicates, someone with full database access could reconstruct individual choices.
> That is documented honestly, along with the absence of cryptographic receipts and the fact
> that the demo credentials are not suitable for a real election. In future work I would
> separate anonymous ballots from voter eligibility with cryptographic receipts, add an audit
> log and account lockout, and support multiple elections."

**If asked to shorten to two minutes:** cover the problem, the three enforcement layers for one
vote, the layered design, PBKDF2, the 59 tests and the ballot-secrecy limitation.

---

## Part B — Questions and answers

### Object-oriented programming

**Q1. What are the four pillars of OOP, and where are they in your project?**
Encapsulation: `Voter`, `Candidate` and `Election` keep fields private and expose only meaningful
operations, and `VotingManager` hides the multi-step vote workflow behind one `castVote` method.
Abstraction: repository interfaces such as `VoterRepository` describe persistence without SQL
details. Inheritance: all expected failures extend `VoteSecureException`, and UI components such
as `FlatButton` extend Swing classes. Polymorphism: the services use repository interfaces, so a
different implementation could replace the JDBC one; custom components override painting
methods.

**Q2. Give an example of encapsulation that prevents a bug.**
`Election` refuses an end time that is not after the start time, and `statusAt` is the only place
that decides NOT_STARTED / OPEN / CLOSED. Because every screen and service uses that method, the
boundary rule cannot drift between screens. Another example is that `Voter` has no setter for
`hasVoted`; a caller must use `withVotedStatus`, which returns a new object, so a snapshot used
in results cannot be mutated accidentally.

**Q3. Why is your exception hierarchy useful?**
Expected failures — wrong PIN, duplicate vote, closed window, locked configuration, invalid
input, export failure — all extend `VoteSecureException`. The UI can catch that base type and show
a message, while specific catch blocks or tests can assert the exact type. `DataAccessException`
is separate because a database failure should produce a generic message and be logged, not shown
verbatim to the user. Catching `Exception` everywhere would blur these distinctions.

**Q4. What is the difference between an interface and an abstract class here?**
`VoterRepository` is an interface: there is no shared state or implementation, only a contract,
and Java allows a class to implement several interfaces. An abstract class would be appropriate if
JDBC repositories shared concrete template code, but the shared SQL helpers are small and are
kept in `SqlErrors`/each class. The `Refreshable` interface lets unrelated Swing panels share the
"reload when shown" capability without a common superclass.

**Q5. Where do you use composition, and why prefer it to inheritance?**
`VotingManager` is composed of repositories, `ElectionService`, `AuthenticationService`,
`ResultsService` and `ReportExporter`. Composition makes dependencies explicit in the
constructor and easy to replace in tests. Inheriting from a repository would couple the service
to a concrete persistence implementation, which is exactly what the layered design avoids.

**Q6. How does your design follow the single-responsibility principle?**
`ResultsService` only computes a result snapshot; `ReportExporter` only formats and writes; the
repository only persists; the panel only presents. `VotingManager` orchestrates the voting use
case but does not contain SQL or Swing code. Each class has a small reason to change.

**Q7. What is an immutable object, and which classes are immutable?**
An immutable object's state cannot change after construction. `Voter`, `Candidate`, `Vote`,
`Election`, `ResultsSummary`, `CandidateResult` and `Administrator` hold final fields and expose
no mutators. `Election.withWindow` and `Voter.withVotedStatus` return new instances. This avoids
aliasing bugs when the same object is used by a table model and a service at the same time.

**Q8. Why did you use enums?**
`ElectionStatus` has exactly three valid values, so an enum removes invalid strings and booleans.
Switch expressions over the enum are checked by the compiler. Similarly, the status badge maps an
enum to a colour and text in one place.

### Java language and Swing

**Q9. Why Java 17 if the installed JDK is newer?**
The project sets `maven.compiler.release` to 17, so the compiler targets the Java 17 API and
bytecode. This keeps it runnable on any Java 17+ machine while allowing the build on a newer JDK.
The code uses Java 17 features such as text blocks for the SQL schema and `var` in local
declarations, and it avoids preview features.

**Q10. How does the application start?**
`VoteSecureApp.main` configures logging in the application-data folder, checks for a graphical
desktop, installs the look and feel and theme, creates and initialises `DatabaseManager`, builds
`AppContext` and then creates `MainFrame` on the Event Dispatch Thread via
`SwingUtilities.invokeLater`. Database and startup exceptions show a friendly dialog and are
logged.

**Q11. What is the Event Dispatch Thread, and why does it matter?**
Swing components must be created and modified on the EDT; otherwise repaint and event ordering
can break. The application starts the window on the EDT, and the screenshot tool uses
`invokeAndWait` for every UI change. Service calls are quick local database operations, so they
are executed in the action listener on the EDT.

**Q12. How does navigation work without opening many windows?**
`MainFrame` uses a `CardLayout` with one card per screen. `showScreen` switches cards and calls
`refresh()` on screens implementing `Refreshable`, so data is re-read when the screen appears.
The custom `ScrollableCards` panel reports the visible card's preferred size, and the frame wraps
it in a `JScrollPane`, so each screen scrolls independently.

**Q13. How does the ballot screen prevent submitting without a selection?**
The table's selection listener enables the submit button only when a row is selected. The panel
also checks the selection before opening the confirmation dialog, and `VotingManager` rejects a
blank candidate ID. This is a UI convenience on top of the service rule, not the only guard.

**Q14. What Swing event mechanisms did you use?**
`ActionListener` for buttons, `ListSelectionListener` for tables, `DocumentListener` for live
search, `WindowAdapter` for closing, and a modal `JOptionPane` confirmation dialog. The
confirmation returns an integer that the panel compares with `YES_OPTION`; canceling performs no
service call.

### Database and JDBC

**Q15. Why SQLite rather than MySQL or PostgreSQL?**
The brief requires a local Windows application with no server. SQLite is embedded in the JAR via
the JDBC driver, stores everything in one file, requires no installation and still supports
transactions, foreign keys and unique constraints. MySQL would add a service to install and
administer with no benefit for one machine.

**Q16. How is the schema created?**
`DatabaseManager.createSchema` executes `CREATE TABLE IF NOT EXISTS` statements and indexes
inside a single connection, then sets `PRAGMA user_version = 1`. `initialize()` calls
`createSchema`, ensures the demo administrator and seeds demo records only if the elections table
is empty. It is safe to run on every launch.

**Q17. Where are foreign keys enforced, and why does it matter?**
SQLite disables foreign-key enforcement by default, so `DatabaseManager.getConnection` executes
`PRAGMA foreign_keys = ON` on every connection. Without it, a vote could reference a missing
candidate. Ballots use `ON DELETE RESTRICT`, so deleting a candidate or voter with a ballot is
refused; candidates cascade only if an election row is deleted, which the UI does not offer.

**Q18. Explain the unique constraint on votes.**
The `votes` table declares
`CONSTRAINT one_vote_per_voter_per_election UNIQUE (election_id, voter_id)`. The pair, not the
voter alone, is unique, so the schema could support several elections. The repository catches the
SQLite constraint violation and translates it into `DuplicateVoteException`. A test manually
resets `voters.has_voted` and still cannot insert a second ballot, proving that the guarantee
does not depend on the flag.

**Q19. Why keep `has_voted` if the votes table already proves it?**
It makes list screens cheap and expresses the voter's state directly. It is a denormalised
convenience field, and the votes table remains authoritative. The transaction updates both, and
the test in Q18 demonstrates the database constraint protects correctness even if the flag is
wrong.

**Q20. Show how you prevent SQL injection.**
Every query uses `PreparedStatement` with bound parameters, for example
`SELECT ... FROM voters WHERE voter_id = ?` and the search
`WHERE UPPER(voter_id) LIKE ? OR UPPER(name) LIKE ?` with the pattern built as a bound value.
No user input is concatenated into SQL. Identifiers such as table names are fixed literals in the
code. The OWASP SQL Injection Cheat Sheet is referenced in the report.

**Q21. How does the vote transaction work, and what triggers a rollback?**
`JdbcVoteRepository.castVote` opens a connection, sets auto-commit false, inserts the vote,
updates the voter's flag, commits and restores auto-commit. A `SQLException` anywhere in the try
block calls `rollback()`. A test creates a trigger that aborts the flag update and asserts that
neither the vote row nor the flag remains, proving the insert is rolled back too.

**Q22. Why is `VotingManager.castVote` synchronized if the database already has a constraint?**
The unique constraint is the final guarantee, but it throws an exception for the losing request.
Synchronizing serialises submissions within one JVM so the normal path gives a clean success or
a clean "already voted" message instead of relying on a constraint violation. Two separate
processes are still protected by the database.

**Q23. How do you handle database errors without leaking information?**
Repositories log the `SQLException` with context at SEVERE level, then throw
`DataAccessException` with a generic message. `SqlErrors` maps known cases such as unique
constraint and foreign-key failure to specific domain exceptions. The UI shows the exception's
message only for expected rule violations; for `DataAccessException` it shows a generic
"could not save" style message. Stack traces never appear in the interface.

### Security

**Q24. Why not store a SHA-256 hash of the PIN?**
A four-digit PIN has only 10,000 possibilities. A fast hash can be brute-forced instantly. PBKDF2
with 120,000 iterations and a per-user random salt makes each guess expensive and prevents
precomputed tables. The salt also ensures two voters with PIN 1234 have different hashes.

**Q25. How is the hash verified, and why constant-time comparison?**
`PasswordHasher.verify` decodes the stored salt, re-derives the key from the entered credential
and compares the bytes with `MessageDigest.isEqual`. That method avoids early-exit comparison
timing leaks. A wrong salt or malformed Base64 returns false rather than throwing into the UI.

**Q26. Are credentials ever logged or displayed?**
No. Loggers receive event descriptions, not credentials. `Voter.toString` excludes hash and salt,
which a test asserts. The registration screen uses a password field; the PIN cannot be viewed
after registration because the database stores only the hash. The demo credentials are documented
because they are intentionally seeded for assessment, with warnings that they are unsuitable for
real use.

**Q27. What are the privacy limitations of your design?**
The `votes` row contains `voter_id` and `candidate_id`, so an administrator with the database
file can see individual choices. This is necessary for the simple duplicate-prevention design.
Voter-facing screens never show the mapping, and reports contain aggregates only, but secrecy
against full database access is not guaranteed. The documentation states this clearly and
proposes anonymous ballots with receipts as future work.

**Q28. Is this system suitable for a real election?**
No. It is an academic local prototype. It lacks cryptographic receipts, an independent audit
trail, protection against a malicious administrator, network security, account lockout and legal
certification. The README, user manual, results screen and reports all state this. Using it for a
real election would be inappropriate.

### Time, validation and results

**Q29. Explain the voting window boundary rule.**
Voting is allowed when `start <= now < end`. At exactly the start instant it is open; at exactly
the end instant it is closed. `Election.statusAt` implements this with `now.isBefore(start)` and
`now.isBefore(end)`. Tests cover one second before start, the start instant, one second before
end, the end instant and a later time. The rule is also stated on the election settings screen.

**Q30. Why inject a Clock instead of calling `LocalDateTime.now()` everywhere?**
A direct call makes boundary tests depend on real time and forces sleeps. With an injected
`Clock`, the test sets the exact instant it needs, including the end instant. Production passes
`Clock.systemDefaultZone()`. `ElectionService`, `VotingManager`, `ResultsService` and
`DatabaseManager` all use the injected clock.

**Q31. How is turnout calculated, and why exclude ineligible voters?**
Turnout is `votesCast / eligibleVoters × 100`. Ineligible records are registered people who may
not vote, so including them would understate participation. A test registers three eligible and
one ineligible voter, casts two votes and expects 66.6667%, not 50%. Division by zero is guarded
and yields 0.0.

**Q32. How do you handle a tie and a zero-vote election?**
`ResultsService` finds the highest non-zero count and collects every candidate with that count.
More than one name sets `tie = true` and the outcome text begins "Tie between …". With no votes,
the winner list is empty and the outcome text is "No votes recorded" with label "No result yet".
Percentages are 0.0, never `NaN`. Tests cover all three cases.

**Q33. How does the configuration freeze work?**
`ElectionService.isConfigurationLocked` returns true if the end time has been reached or if the
election has at least one ballot. `AdminService` calls it before adding, editing or deleting a
candidate and before changing the schedule. The UI disables the controls and displays the reason.
This prevents changing the ballot after a result exists.

**Q34. Why are results hidden from voters while voting is open?**
Publishing early counts can influence later voters and undermine the election. Voters see a
notice that results will appear after voting closes. The administrator can see live results,
clearly labelled LIVE PREVIEW, because the administrator needs operational awareness. The label
makes clear the figures are not final.

### Testing and process

**Q35. How did you test time-dependent behaviour deterministically?**
`TestEnvironment` creates an election from 09:00 to 17:00 and a `MutableClock` fixed at 10:00.
Tests call `setNow` or `advance` to move to exact boundary instants. No test sleeps. The clock is
also used by the report timestamp and result snapshot tests.

**Q36. How do you ensure tests do not touch real data?**
Each test class creates a new temporary directory and database file via
`TestEnvironment`, builds the schema and a test election, and closes/deletes the directory in
`@AfterEach`. The production path `~/.votesecure/votesecure.db` is never referenced by tests.
The concurrency test creates two `AppContext` objects over the same temporary database.

**Q37. What is your most important test, and why?**
The duplicate-vote test that resets `has_voted = 0` and then attempts a direct repository insert.
It proves that the database constraint, not a UI flag, is the final protection. The concurrent
test is also important because it exercises two application contexts and two threads, which is
the realistic double-submission scenario.

**Q38. How do you know the reports contain real data and not hard-coded values?**
The exporter takes a `ResultsSummary` built by `ResultsService` from repository counts, and tests
assert the written file contains the expected names, counts and percentages after votes are cast.
The sample reports are generated by `SampleReportsGenerator` from an isolated database, and both
files carry an explicit SAMPLE/DEMONSTRATION notice.

**Q39. What defects did you find during testing, and how were they fixed?**
The report lists six: HTML label width clipping (fixed with a table cell width), horizontal
clipping at 1280 px (custom Scrollable card container), excessive scrolling from the tallest
card (preferred size follows the visible card), badge stretching (maximum height), screenshots
capturing the wrong window (always-on-top and content bounds), and test fixtures using digits in
names (realistic names). Each fix is in the delivered code.

**Q40. What would you do differently with more time?**
Separate anonymous ballots from the eligibility list with cryptographic receipts, add an
append-only audit log and account lockout, support multiple elections, and add UI automation for
the dialog and file-chooser paths. I would also do an accessibility review with a screen reader.

### Design and scope

**Q41. Why support only one current election?**
The brief describes one election workflow, and a single current election keeps the voter
experience and the rules unambiguous. The repository chooses the latest `created_at`, and the
schema already allows several election rows, so adding a selector later would not require a
schema redesign.

**Q42. Why not use an ORM such as Hibernate?**
The course requires JDBC and the project is small. Explicit SQL makes the constraints,
transactions and index usage visible, which is valuable for demonstrating database design. An ORM
would hide the very transaction and constraint behaviour the project is meant to show.

**Q43. Where is the "database" layer different from the "repository" layer?**
`DatabaseManager` owns the file path, connections, pragmas, schema and seed. Repositories know
the queries for one aggregate (voters, candidates, votes, elections, admins). Keeping connection
policy separate from query policy means pragmas cannot be forgotten by an individual repository.

**Q44. What happens if two administrators use the same database file?**
SQLite allows multiple readers and one writer; the 5-second busy timeout makes a second writer
wait briefly. Ballot uniqueness is still enforced. The UI is designed for one administrator at a
time, and a multi-user deployment is explicitly out of scope. This is documented rather than
claimed as supported.

**Q45. How would you extend this to a web application?**
Keep the model and service layers, replace the Swing UI with a web controller and templates, and
replace the JDBC repositories with a server database and connection pool. The service rules,
exceptions and tests largely carry over. The hard parts would be authentication, session security,
CSRF protection, TLS and the ballot-secrecy redesign, none of which are solved by this prototype.

**Q46. Summarise the project in one sentence.**
VoteSecure is a layered Java Swing and SQLite application that enforces eligibility, a precise
voting window and one ballot per voter through service rules, a transaction and a database
constraint, calculates and exports results, and is verified by 59 automated tests while
documenting its limits as an academic prototype.

---

## Part C — Quick facts sheet

| Question | Answer |
| --- | --- |
| Language level | Java 17 |
| GUI | Swing (`CardLayout`, custom theme) |
| Database | SQLite via `sqlite-jdbc 3.46.1.0` |
| Build | Maven 3.10.0; shaded JAR `target/VoteSecure.jar` |
| Tests | 59 JUnit 5 tests, all passing |
| Hashing | PBKDF2WithHmacSHA256, 120,000 iterations, 16-byte salt, 256-bit key |
| Vote uniqueness | Service check + synchronized method + transaction + `UNIQUE(election_id, voter_id)` |
| Window rule | `start <= now < end` (end exclusive) |
| Configuration freeze | Any ballot exists OR `now >= end` |
| Turnout denominator | Eligible voters |
| Results order | Votes descending, then candidate ID |
| Report encodings | TXT UTF-8; CSV UTF-8 with BOM |
| Demo admin | `admin` / `Admin@123` |
| Demo voter | `V001` / `1234` |
| Database path | `%USERPROFILE%\.votesecure\votesecure.db` |
| Main limitation | Ballot secrecy vs. duplicate prevention; local trust boundary |
