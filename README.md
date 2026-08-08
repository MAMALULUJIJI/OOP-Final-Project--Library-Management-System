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

On startup, if no librarian account exists one is seeded from
`library.librarian.email` / `library.librarian.password` (development defaults:
`librarian@library.local` / `changeme123`). Sign in at `/login` with those to
reach the staff area; register members from there. The catalog is public;
borrowing, waitlists, and the account page require sign-in; `/admin/**`
requires the `LIBRARIAN` role.

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

1. Book CRUD — **done**
2. Member registration and login — **done**
3. Borrow and return — **done**
4. Search — **done**
5. Waitlist — **done**
6. Borrowing history — **done**
7. Deploy — remaining

Deploy is last, but worth proving early — push a skeleton that only lists books
so the pipeline works, then redeploy as features land. Discovering a deployment
problem the night before submission is the failure mode this ordering avoids.

## Decisions made along the way

- **Loan period: 14 days. `READY` hold duration: 3 days.** Both live in
  `CirculationPolicy` — change them there, nowhere else.
- **First librarian account is seeded** at startup by `LibrarianSeeder` when no
  librarian exists; credentials come from configuration (see "Build and run").
- **Hold expiry is lazy.** Stale `READY` holds are expired at the top of borrow
  and join operations rather than by a scheduler — the moment staleness matters
  is the moment someone wants the copy.
- **Removing a title removes its circulation records.** Deletion is still
  blocked while a copy is out; once allowed, the title's closed loans and
  waitlist entries go with it, since they reference the book row.
- The partial unique index on `ACTIVE` waitlist entries (see `schema.sql`) is
  not generated by Hibernate — the service-layer check is currently the only
  enforcement. Add the index manually when a real migration step exists.

## Still open

- **Librarian view of all loans**, or is per-member history enough?
- **Waitlist notifications** — email needs a mail service; an in-app notice on
  login does not.
- **Hosting platform** — Render, Railway, or Fly.io.
