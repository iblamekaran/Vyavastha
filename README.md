# VYAVASTHA (व्यवस्था)
### Intelligent Examination Seating & Invigilation Management System

Vyavastha is an examination seating and invigilator allocation management desktop application built using **Core Java, Swing, JDBC, and MySQL**. It automates the generation of conflict-free seating plans, room-wise invigilation schedules, and tabular seating charts with CSV export capabilities.

---

## 🏛️ System Architecture & Layered Design

The system adheres to a strict 4-tier separation of concerns:
```
ui          Swing screens (DashboardFrame, Panels)
 |          - FormTablePanel base template
 |          - Only communicates with Service layer
 v
service     Business logic and validation (AllocationService, ReportService, etc.)
 | \        - Validates inputs before DAO / Seating calls
 |  \       - Coordinates SeatingAlgorithm and ConflictChecker
 v   v
dao  allocation  
 |   - dao: Direct JDBC queries using PreparedStatement & ACID transactions
 |   - allocation: Pure algorithmic seating logic (No DB dependencies)
 v
database    DBConnection (JDBC Connection Manager with externalized config.properties)
 |
 v
MySQL       Database constraints (PRIMARY KEY, UNIQUE, FOREIGN KEY CASCADE)
```

### Architectural Principles:
1. **Zero UI Leaks**: UI components never call DAOs directly; all communication routes through Services.
2. **Pure Seating Engine**: The `allocation` package has no dependency on SQL or database drivers.
3. **Database Integrity as Safety Net**: Database schema contains `UNIQUE (exam_date, slot, roll_no)` and `UNIQUE (exam_date, slot, room_id, row_no, col_no)` constraints, preventing double booking at the database level even if code fails.
4. **ACID Transactions**: Allocations and invigilator assignments are saved atomically via `c.setAutoCommit(false)`, `executeBatch()`, `commit()`, and `rollback()`.

---

## 📁 Project Structure

```
Vyavastha/
├── README.md
├── .gitignore
├── config.properties           (Real DB credentials, git-ignored)
├── config.example.properties   (Template DB credentials)
├── lib/
│   └── mysql-connector-j.jar   (MySQL JDBC Driver)
├── sql/
│   ├── schema.sql              (DDL table definitions & constraints)
│   └── sample_data.sql         (Seed data for students, rooms, exams, invigilators)
├── reports/                    (Auto-generated CSV files, git-ignored)
└── src/
    ├── Main.java               (Swing Desktop entry point)
    ├── ConsoleDemo.java        (Part 1 Standalone Console Engine Demo)
    ├── model/                  (POJOs: Student, Room, Exam, Invigilator, Seat, Allocation)
    ├── exception/              (ValidationException, AllocationException, RoomFullException, DatabaseException)
    ├── allocation/             (SeatingAlgorithm - greedy largest-group-first)
    ├── database/               (DBConnection - Singleton connection factory)
    ├── dao/                    (StudentDAO, RoomDAO, ExamDAO, InvigilatorDAO, AllocationDAO)
    ├── service/                (ConflictChecker, StudentService, RoomService, ExamService, InvigilatorService, AllocationService, ReportService)
    └── ui/                     (FormTablePanel, StudentPanel, RoomPanel, ExamPanel, InvigilatorPanel, AllocationPanel, ReportPanel, DashboardFrame)
```

---

## ⚡ Seating Algorithm Explanation

The seating algorithm (`SeatingAlgorithm.java`) uses a greedy, largest-group-first heuristic:
1. **Capacity Validation**: Calculates total capacity across all rooms. If total students > total seats, immediately throws `RoomFullException`.
2. **Subject Queuing**: Sorts students by `rollNo` (for deterministic, reproducible output) and groups them into queues keyed by subject in a `TreeMap`.
3. **Room & Seat Iteration**: Iterates room by room, row by row, column by column.
4. **Adjacency Check**: Inspects the subject of the immediate left neighbour `grid[r][c-1]` and upper neighbour `grid[r-1][c]`.
5. **Greedy Selection**: Selects the subject with the largest remaining student pool that does not conflict with the left or upper neighbour. If all available subjects conflict, falls back to the largest group to avoid deadlock (flagged as a warning by `ConflictChecker`).
6. **Time Complexity**: $\mathcal{O}(S \times K)$ where $S$ is total seats and $K$ is number of distinct subjects. Execution takes < 1 second even for thousands of students.

---

## 🛠️ Setup & Execution

### Prerequisites
- JDK 17 or newer (Tested on JDK 25)
- MySQL Server 8.0+
- IntelliJ IDEA or Command Line

### 1. Database Configuration
1. Initialize the database schema and sample data:
   ```bash
   mysql -u root -p < sql/schema.sql
   mysql -u root -p < sql/sample_data.sql
   ```
2. Configure `config.properties` in the project root:
   ```properties
   db.url=jdbc:mysql://localhost:3306/vyavastha
   db.user=root
   db.password=your_mysql_password
   ```

### 2. Compilation
Compile all Java source files with the MySQL JDBC driver:
```bash
javac -cp "lib/mysql-connector-j.jar" -d "out/production/Vyavastha" src/**/*.java
```

### 3. Run Standalone Console Engine Demo (Milestone 1)
```bash
java -cp "out/production/Vyavastha" ConsoleDemo
```

### 4. Run Full Swing Desktop Application
```bash
java -cp "out/production/Vyavastha;lib/mysql-connector-j.jar" Main
```

---

## 📋 Comprehensive Test Plan Verification

| # | Test Scenario | Steps | Expected Result | Verified Status |
|---|---|---|---|:---:|
| 1 | Sample Allocation | Run allocation for `2026-12-10` `MORNING` | 12 seated, 3 rooms, 3 invigilators, 0 errors/warnings | Passed |
| 2 | Idempotence | Run allocation a second time for same session | Previous records cleared, fresh allocation saved, exactly 12 records | Passed |
| 3 | Duplicate Roll No | Add student with existing roll number (e.g. 101) | Warning: "Roll number 101 already exists" | Passed |
| 4 | Empty Student Name | Add student with blank name | Warning: "Name cannot be empty" | Passed |
| 5 | Non-numeric Roll No | Add student with string as roll number | Warning: "Roll number must be a number" | Passed |
| 6 | Room Capacity Limit | Delete rooms until seats < registered students | Error: "12 students but only 4 seats available" | Passed |
| 7 | Invigilator Shortage | Fewer invigilators than required rooms | Error: "X rooms are in use but only Y invigilators exist" | Passed |
| 8 | Unscheduled Exam | Allocate date/slot with no exams scheduled | Warning: "No exams are scheduled on..." | Passed |
| 9 | Unallocated Report | View seating chart for session not yet allocated | Warning: "No allocation is saved for..." | Passed |
| 10 | Scale Test (500+ Students) | Insert 500 students, multiple rooms, run allocation | Allocation succeeds in < 1 second | Passed |

---

## 🎓 Viva Questions & Answers

- **Q: Why separate ConflictChecker from SeatingAlgorithm?**  
  *A:* Single responsibility and independent validation. An algorithm should not be the sole validator of its own output, ensuring algorithmic bugs are caught.

- **Q: Why are same-subject neighbours treated as warnings instead of hard errors?**  
  *A:* If a session contains only one exam subject, or one subject heavily outnumbers others, adjacent placement is mathematically unavoidable. Hard errors represent invalid physical states (e.g. duplicate seatings).

- **Q: Why are database transactions essential in AllocationDAO?**  
  *A:* A session allocation modifies both `allocation` and `invigilator_assignment` tables. If one insert fails midway, `rollback()` guarantees the database is not left in a partial, corrupted state.

- **Q: What prevents SQL Injection in Vyavastha?**  
  *A:* All queries use `PreparedStatement` with parameterized placeholders (`?`), ensuring user input is never concatenated directly into SQL statements.
