# Library Management System

Spring Boot library app (Thymeleaf + JPA). SQLite locally, PostgreSQL in production.

## Live deployment

The system is deployed and publicly reachable:

**https://library-management-system-54sk.onrender.com**

Sign in as a librarian to reach the staff area (catalog management and member
management); the catalog itself is browsable without an account.

| | |
|---|---|
| Email | `Librarian@OOP.com` |
| Password | `OOP2026` |

Demonstration account for the deployed instance. The email is treated
case-insensitively at sign-in.

Hosted on Render (Docker) against a managed PostgreSQL database. The free
instance sleeps after a period of inactivity, so the **first request after an
idle spell can take up to a minute** while it wakes; subsequent pages are
immediate.

## Requirements

- JDK 21+
- Maven 3.9+ (or use `./mvnw`)

## Run locally

```bash
./mvnw spring-boot:run
```

Open http://localhost:8080

Default librarian (created on first start if none exists):

- Email: `librarian@library.local`
- Password: `changeme123`

Data is stored in `library.db` (gitignored). Delete it for a clean database.

```bash
./mvnw test
./mvnw package
java -jar target/library-management-system-1.0-SNAPSHOT.jar
```

## Production

Set `SPRING_PROFILES_ACTIVE=prod` and:

| Variable | Purpose |
|---|---|
| `JDBC_DATABASE_URL` | PostgreSQL URL, in `jdbc:postgresql://host:5432/database` form |
| `JDBC_DATABASE_USERNAME` | Database user |
| `JDBC_DATABASE_PASSWORD` | Database password |
| `LIBRARY_LIBRARIAN_EMAIL` | First librarian email |
| `LIBRARY_LIBRARIAN_PASSWORD` | First librarian password |

Prod uses `ddl-auto=validate`. On a fresh database, run `src/main/resources/schema.sql` once before the first start.

The image is built from the `Dockerfile` at the repository root; `render.yaml`
describes the web service and its database for Render's Blueprint.

Note that `LibrarianSeeder` creates the first librarian **only when no account
with the `LIBRARIAN` role exists**. Changing the two environment variables on a
database that already holds a librarian has no effect — the existing row must be
removed first.
