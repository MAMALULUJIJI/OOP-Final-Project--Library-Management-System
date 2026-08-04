# Library Management System

Desktop application for managing a library's catalog, members, and borrowing
activity. Java + Swing on the front, SQLite via JDBC underneath.

See [`docs/proposal.md`](docs/proposal.md) for the full project proposal.

## Requirements

- JDK 21 or newer
- Maven 3.9 or newer

Nothing else to install — SQLite ships as a Maven dependency, not a server.

## Build and run

```bash
mvn test          # run the test suite
mvn exec:java     # run the app from source
mvn package       # build target/library-management-system-1.0-SNAPSHOT.jar
java -jar target/library-management-system-1.0-SNAPSHOT.jar
```

On first run the app creates `library.db` in the working directory by applying
`src/main/resources/schema.sql`. Delete the file to start over — it is
gitignored and never committed. The schema is; that is the file to edit when the
data model changes.

## Layout

```
src/main/java/com/library/
  Main.java              entry point — initializes the DB, opens the window
  ui/                    Swing windows and event handlers
  service/               business rules and transactions
  dao/                   one DAO per entity, all SQL lives here
  domain/                Book, Member, Loan, Reservation
  persistence/           Database — connections and schema bootstrap
src/main/resources/
  schema.sql             table definitions, constraints, indexes
src/test/java/           JUnit 5 tests, mirroring the main package layout
```

The dependency direction is one-way: `ui → service → dao → persistence`, with
`domain` available to every layer. A class that reaches backwards — SQL in a
panel, a `JOptionPane` in a service — is in the wrong package.

## Ground rules

These come from the proposal and are worth keeping in front of you while
building:

- **All SQL uses `PreparedStatement`.** Never concatenate values into a query.
- **Multi-step writes run in one transaction.** Borrowing inserts a loan *and*
  decrements `available_copies`; both succeed or neither does. The service layer
  owns `setAutoCommit(false)` / `commit()` / `rollback()`; DAO methods that take
  part accept the `Connection` as a parameter.
- **Availability is derived, not flagged.** `available_copies` is the truth; there
  is no separate "is available" boolean to drift out of sync.
- **Overdue is computed at read time** from `due_date`. It is never a stored column.
- **Uniqueness is enforced twice** — a `UNIQUE` constraint on ISBN and email, plus
  validation in the service layer that produces a readable error message.

## Build order

1. Book CRUD
2. Member registration
3. Borrow and return
4. Search
5. Reservations
6. Borrowing history

## Still open

Decisions the team has not made yet. The scaffold does not commit to any of them:

- **UI toolkit** — scaffolded with Swing because it is in the JDK and needs no
  setup. Only `ui/` and the pom depend on that choice, so switching to JavaFX
  stays cheap while `ui/` is thin.
- **Active-loan cap per member** — proposal suggests 5.
- **Loan period** — proposal suggests 14 days.
- **Librarian view of all loans**, or is per-member history enough?
- **Reservation expiry** — should a `READY` reservation lapse after N days?

The last four are service-layer constants and rules; put them in one place when
you get there rather than scattering literals through `LoanService`.
