# VoteSecure — User Manual

**Audience:** students, demonstrators and examiners running the project on a Windows computer.
**Version:** 1.0 (Semester 3 submission, 30 October 2026)

> VoteSecure is an academic local prototype. Do not use it for a real public election.

---

## 1. What you need

| Item | Notes |
| --- | --- |
| Windows 10 or 11 (64-bit) | The launcher `run.bat` is written for Windows. |
| Java 17 or newer | Tested with JDK 25.0.4.1, compiled to Java 17. |
| Apache Maven 3.6+ | Only needed to build the JAR. |
| Internet access for the first build | Maven downloads SQLite JDBC and JUnit. |
| About 60 MB free disk space | JAR (~14 MB), database and reports. |

The project folder must contain `pom.xml`, `run.bat` and the `src` folder. Keep the whole folder
together; do not move only the JAR unless you do not need the source or documents.

---

## 2. Install Java (one time)

1. Open <https://adoptium.net/> in a browser.
2. Download **Temurin 17 (LTS)** or newer for Windows x64.
3. Run the installer and tick **Set JAVA_HOME variable** and **Add to PATH**.
4. Close and reopen the Command Prompt.
5. Check the installation:

```bat
java -version
```

Expected output begins with `openjdk version "17..."` (or a newer version). If Windows says
`'java' is not recognized`, the PATH step was missed or the terminal was not restarted.

## 3. Install Maven (one time)

1. Download Apache Maven from <https://maven.apache.org/download.cgi> (binary ZIP).
2. Extract it to `C:\Program Files\Apache\maven` (any folder without spaces is easiest).
3. Add `...\apache-maven-3.10.0\bin` to the **Path** environment variable.
4. Open a new Command Prompt and check:

```bat
mvn -version
```

Both `mvn` and the Java version should be printed.

---

## 4. Build the application

Open Command Prompt and change to the project folder:

```bat
cd C:\Users\<your-name>\Documents\Qoder\2026-10-28\bce4440b\VoteSecure
mvn clean test
mvn package
```

- `mvn clean test` compiles and runs all 59 automated tests. A successful run ends with
  `BUILD SUCCESS`.
- `mvn package` builds `target\VoteSecure.jar`, a self-contained JAR including the SQLite driver.

The first build takes 1–3 minutes because Maven downloads dependencies.

## 5. Start the application

### Option A — one click

Double-click **`run.bat`** in the project folder. If the JAR does not exist yet, the script builds
it first (when Maven is installed) and then opens the application.

### Option B — Command Prompt

```bat
java -jar target\VoteSecure.jar
```

### Option C — fresh temporary database

Useful for a clean demonstration without touching your normal data:

```bat
java -Dvotesecure.db=C:\temp\votesecure-demo.db -jar target\VoteSecure.jar
```

Delete `C:\temp\votesecure-demo.db` afterwards to reset again.

---

## 6. First launch

The first launch creates:

- the SQLite database at `C:\Users\<your-name>\.votesecure\votesecure.db`,
- one administrator (`admin` / `Admin@123`),
- ten voters (`V001`–`V010`, PIN `1234`),
- three candidates (`C001`–`C003`),
- one open election (`E2026`, Student Council Election 2026) that started 30 minutes ago and
  ends in 30 days.

**No votes are created.** Results therefore start at 0 votes, 0.0% turnout and the notice
"No votes have been cast yet."

The welcome screen offers **Voter** and **Administrator** entry points. Screenshots of every
screen are in `docs/screenshots/`.

---

## 7. Administrator walkthrough

### 7.1 Sign in

1. Click **Administrator** on the welcome screen.
2. Enter username `admin` and password `Admin@123`.
3. Click **Sign in**.

A wrong password produces a single generic message and does not reveal whether the username
exists. There is no account lockout in this prototype; this is a documented limitation.

### 7.2 Dashboard

The dashboard shows registered voters, candidate count, votes cast, the election window and its
status (Not started / Voting open / Closed). The toolbar provides **Voter management**,
**Candidate management**, **Election settings**, **Results** and **Export report**. Use
**Change password** to replace the demonstration password and **Sign out** to end the session.

### 7.3 Register a voter

1. Open **Voter management**.
2. Enter a voter ID (3–20 characters, letters/numbers/`_`/`-`; stored uppercase), full name and an
   initial PIN of 4–12 digits.
3. Leave **Eligible to vote** ticked unless the person must be blocked.
4. Click **Register voter**.

The new voter appears in the table immediately. Hand the ID and PIN to the voter in person; the
PIN cannot be viewed again because only its salted hash is stored. Registering the same ID twice
shows "A voter with this ID already exists."

Use the search box to filter by partial ID or name. A blank search lists everybody. Running
totals show how many of the registered voters are eligible.

### 7.4 Manage candidates

1. Open **Candidate management**.
2. Fill in candidate ID, full name, party and an optional description (≤ 300 characters).
3. Click **Add candidate**.
4. Select a row to load it into the form, edit the fields and click **Save changes**.
5. Select a row and click **Delete selected** to remove a candidate.

All changes are possible only while the ballot is unlocked. Once the first vote is recorded, the
screen displays "At least one vote has been recorded; the ballot is locked." and the buttons are
disabled. Deleting a candidate is refused by the database if a ballot already references it.

### 7.5 Set the election schedule

1. Open **Election settings**.
2. Enter the election name, start and end date/time in the format shown
   (`dd MMM yyyy, hh:mm a`, for example `30 Oct 2026, 09:00 AM`).
3. Click **Save schedule**.
4. The **Open now for 7 days** button fills a window that starts now and ends seven days later;
   press **Save schedule** to apply it.

Rules:

- The end must be later than the start.
- Voting is allowed from the start instant **up to but not including** the end instant.
- If a ballot exists or the end time has passed, the configuration is locked and saving is
  rejected. The screen states the reason.

All times use the computer's local time zone, shown on the screen.

### 7.6 View results

Open **Results**. While voting is open the screen shows a **LIVE PREVIEW** badge; this warns that
counts are not final. After the end time the badge becomes **CLOSED** and the outcome is final.
The screen shows eligible voters, votes cast, turnout, the winner or tie, and a candidate table
with counts and percentages. Zero votes are displayed as 0 and 0.0%, not as an error.

### 7.7 Export a report

1. Open **Export report**.
2. Review the on-screen preview and the note explaining which database is used.
3. Click **Export as TXT** or **Export as CSV**.
4. Choose a folder and file name in the file chooser and confirm.
5. A confirmation appears with the file path. Open the file in Notepad (TXT) or Excel (CSV).

The CSV is UTF-8 with a byte-order mark, so Excel displays names correctly. The export always
contains the disclaimer that VoteSecure is an academic prototype. The report contains aggregate
data only; it does not list which voter chose which candidate.

### 7.8 Change the administrator password

1. Click **Change password** on the dashboard.
2. Enter the current password, the new password twice, and confirm.
3. The new password must be 8–64 characters and contain uppercase, lowercase, a digit and a
   special character. It must not contain spaces.
4. Sign in with the new password next time. There is no password-recovery feature; if it is lost,
   delete the database file to restore the demonstration account (this also deletes all data).

---

## 8. Voter walkthrough

### 8.1 Sign in

1. On the welcome screen click **Voter**.
2. Enter the voter ID exactly as issued (case does not matter) and the PIN.
3. Click **Sign in**.

Wrong IDs and wrong PINs receive the same generic message. An unregistered voter cannot sign in.
Eligibility is checked when the voter tries to cast a ballot, so an ineligible voter can sign in
but is blocked from voting with a clear message.

### 8.2 Dashboard

The dashboard shows the voter's name and ID, the election name and window, whether voting is open
and whether this voter has already voted. From here the voter can open the ballot, view published
results (only after the election closes) or sign out.

### 8.3 Cast a vote

1. Click **Cast vote**.
2. Select exactly one candidate row. The **Review and submit vote** button becomes active.
3. Click **Review and submit vote**.
4. A confirmation dialog shows the candidate's name and party. Choose **Cancel** to go back, or
   **Confirm vote** to save.
5. On success the screen shows **Vote recorded successfully**, the ballot time, and three actions:
   **Return to dashboard**, **View results** (if published) and **Sign out**.

The success screen deliberately does not repeat the candidate choice, so someone looking over the
voter's shoulder does not see it on the receipt.

### 8.4 Voting twice

After voting, the dashboard changes to "You have already voted" and the ballot button is
disabled. If a second attempt reaches the service (for example from a second window), it is
rejected with "This voter has already cast a vote. Each voter may vote only once." The database
unique constraint provides the final guarantee.

### 8.5 Results visibility

While the election is open, the voter sees a notice that results will be published after voting
closes. This prevents early results from influencing later voters. After the end time, the
aggregate results are shown.

---

## 9. Where everything is stored

| Item | Location |
| --- | --- |
| Database | `C:\Users\<your-name>\.votesecure\votesecure.db` |
| Log files | `C:\Users\<your-name>\.votesecure\logs\votesecure-*.log` |
| Built JAR | `target\VoteSecure.jar` |
| Exported reports | Wherever you choose in the file chooser |
| Sample reports | `reports\Sample_Results.txt`, `reports\Sample_Results.csv` |

The database is created automatically and is **not** included in the submission ZIP. The
`.gitignore` excludes `*.db`, `*.db-wal`, `*.db-shm` and the `data/` folder to avoid committing
voter data.

### Reset the demonstration

1. Close VoteSecure.
2. Delete `C:\Users\<your-name>\.votesecure\votesecure.db` (and any `.db-wal` / `.db-shm` files).
3. Start the application again.

The schema, administrator, sample voters, candidates and open election are recreated. All previous
votes, registered voters and password changes are gone.

---

## 10. Common messages and what to do

| Message | Meaning | Action |
| --- | --- | --- |
| "Invalid voter ID or PIN." | The credentials do not match. | Check the ID and PIN with the administrator. |
| "Invalid administrator username or password." | Wrong admin credentials. | Use `admin` / `Admin@123` or the changed password. |
| "This voter is not eligible to vote in this election." | Eligibility flag is off. | Ask the administrator to correct the roll (a fresh database is needed if votes already exist). |
| "Voting has not started yet…" | Current time is before the start. | Wait until the scheduled start. |
| "Voting has closed…" | Current time is at or after the end. | An administrator may set a new window only if no votes exist. |
| "This voter has already cast a vote…" | Duplicate attempt. | Expected; each voter votes once. |
| "Please select a candidate before submitting." | No row selected. | Select one candidate row. |
| "Election configuration is locked…" | A vote exists or the election ended. | Configuration is intentionally frozen. |
| "The selected candidate does not exist." | The candidate was deleted or the ID is wrong. | Refresh the ballot screen. |
| "A voter with this ID already exists." | Duplicate registration. | Use a unique ID. |
| "PIN must be 4-12 digits." | PIN format is wrong. | Enter digits only. |
| "The report could not be saved." | Destination is read-only or the file is open elsewhere. | Choose another folder or close the file. |
| "The application database could not be opened." | Permissions or a locked file. | Close other copies; check folder permissions. |
| "No election is configured." | The database has no election row. | Reset the database (section 9). |

---

## 11. Keyboard and accessibility notes

- `Tab` / `Shift+Tab` move between fields; `Enter` activates the default button on forms.
- Tables support arrow keys and `Enter`/`Space` for selection.
- The application was tested at 1280×720. On smaller or high-DPI screens, maximise the window;
  each screen scrolls vertically.
- Screen-reader labelling is basic and listed as future work.

---

## 12. Quick demonstration script (for an examiner)

1. Start with a **fresh database** (Option C) so no votes exist.
2. Sign in as `admin` / `Admin@123`.
3. Show voter management, candidate management and election settings.
4. Sign out, sign in as `V001` / `1234`, cast a vote for `C001`, confirm, and show the success
   screen.
5. Sign out, sign in as `V001` again and try to vote: the ballot is disabled and a second attempt
   is rejected. This demonstrates duplicate-vote protection.
6. Sign in as admin, show **Results** (live preview, 1 vote) and export a TXT and CSV report.
7. Open `reports\Sample_Results.txt` to show the pre-generated sample report, clearly labelled as
   demonstration data.
8. Sign out and close the application.

To demonstrate a closed election, set the end time to a past moment **before** any vote, then try
to vote. After a vote exists the schedule cannot be changed; use a fresh database for a different
scenario.

---

*End of User Manual.*
