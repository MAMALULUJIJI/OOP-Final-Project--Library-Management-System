# Library Management System

Web application for managing a library's catalog, members, and borrowing
activity. Spring Boot with server-rendered Thymeleaf templates, JPA over SQLite
locally and PostgreSQL in production.

See [`docs/proposal.md`](docs/proposal.md) for the full proposal.

## Requirements

- JDK 21 or newer
- Maven 3.9 or newer

Nothing else to install locally — SQLite is a Maven dependency, not a server.

## Build and run

```bash
mvn test              # run the test suite
mvn spring-boot:run   # run at http://localhost:8080
mvn package           # build target/library-management-system-1.0-SNAPSHOT.jar
java -jar target/library-management-system-1.0-SNAPSHOT.jar
```

On first run Hibernate creates `library.db` in the working directory from the
entity definitions. Delete the file to start over — it is gitignored and never
committed.

Until a `SecurityFilterChain` exists in `config/`, Spring Boot's default security
applies: every URL requires login as user `user`, with a password generated and
printed at startup.

## Layout

```
src/main/java/com/library/
  LibraryApplication.java   entry point; component scan starts here
  controller/               HTTP endpoints and form handling
  service/                  business rules and @Transactional operations
  repository/               Spring Data repositories — all queries live here
  domain/                   Book, Member, Loan, WaitlistEntry (JPA entities)
  config/                   security and application configuration
src/main/resources/
  application.properties        local development (SQLite)
  application-prod.properties   production (PostgreSQL, all values from env)
  schema.sql                    readable schema reference — not executed
  templates/                    Thymeleaf views
  static/                       CSS and images
src/test/java/                  tests, mirroring the main package layout
```

Dependencies point one way: `controller → service → repository → database`, with
`domain` visible to all. Two rules keep that honest:

1. A controller never touches a repository directly. It goes through a service.
2. No queries outside the repository layer.

## Ground rules

From the proposal, worth keeping in front of you while building:

- **Multi-step writes are `@Transactional`.** Borrowing inserts a loan *and*
  decrements `availableCopies`; returning increments it *and* promotes the front
  of the waitlist to `READY`. Each set commits or rolls back together.
- **Availability is a count, not a flag.** No `isAvailable` boolean — it drifts.
- **Overdue is computed at read time** from the due date. No `overdue` column.
- **Passwords are BCrypt hashes.** Never plain text. Email is the login identifier.
- **Uniqueness is enforced twice** — a database constraint on ISBN and email, plus
  service-layer validation that produces a readable message. Same for the "one
  waitlist spot per member per title" rule, which has a partial unique index on
  `ACTIVE` entries as its backstop.
- **No secrets in the repository.** Production credentials arrive as environment
  variables.

`schema.sql` is documentation, not migration. Hibernate builds the real schema
from the entities; when the two disagree, the entities win and `schema.sql` is
the thing to fix.

## Database

| Environment | Engine | Configured by |
|---|---|---|
| Local development | SQLite file (`library.db`) | `application.properties`, active by default |
| Production | Managed PostgreSQL | `application-prod.properties`, via `SPRING_PROFILES_ACTIVE=prod` |

SQLite is not viable in production: hosting platforms give containers an
ephemeral filesystem, so the database file disappears on every redeploy. Because
access goes through JPA, switching engines is configuration rather than code.

The `prod` profile expects `JDBC_DATABASE_URL`, `JDBC_DATABASE_USERNAME`, and
`JDBC_DATABASE_PASSWORD` in the environment, and binds to `PORT` if the platform
sets it. It runs `ddl-auto=validate`, so a schema mismatch fails startup instead
of quietly altering the production database.

## Build order

1. Book CRUD
2. Member registration and login
3. Borrow and return
4. Search
5. Waitlist
6. Borrowing history
7. Deploy

Deploy is last, but worth proving early — push a skeleton that only lists books
so the pipeline works, then redeploy as features land. Discovering a deployment
problem the night before submission is the failure mode this ordering avoids.

## Still open

Decisions the team has not made. The scaffold commits to none of them:

- **Loan period** — proposal suggests 14 days.
- **`READY` hold duration** — how long a held copy waits before passing to the
  next member on the waitlist.
- **Librarian view of all loans**, or is per-member history enough?
- **Waitlist notifications** — email needs a mail service; an in-app notice on
  login does not.
- **Hosting platform** — Render, Railway, or Fly.io.
- **First librarian account** — self-registration or seeded manually?

The first two are service-layer constants. Put them in one place when you get
there rather than scattering literals through `LoanService`.
