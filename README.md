# Library Management System

Spring Boot library app (Thymeleaf + JPA). SQLite locally, PostgreSQL in production.

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
| `JDBC_DATABASE_URL` | PostgreSQL URL |
| `JDBC_DATABASE_USERNAME` | Database user |
| `JDBC_DATABASE_PASSWORD` | Database password |
| `LIBRARY_LIBRARIAN_EMAIL` | First librarian email |
| `LIBRARY_LIBRARIAN_PASSWORD` | First librarian password |

Prod uses `ddl-auto=validate`. On a fresh database, run `src/main/resources/schema.sql` once before the first start.
