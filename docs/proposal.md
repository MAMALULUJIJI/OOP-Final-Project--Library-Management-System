# Library Management System — Project Proposal

**Prepared by:** Magnus Ma
**Status:** Draft for team review
**Revision:** v2 — web application, waitlist model, borrowing cap removed

---

## 1. Project Overview

We will build and deploy a **web application** that helps a library manage its books, its members, and the borrowing activity between them. The system is reachable from a browser and runs on a hosted server rather than on each user's machine.

The system serves two kinds of users. **Librarians** maintain the catalog and register members. **Members** search the catalog, borrow and return books, join the waitlist for titles that are currently out, and review their own borrowing history.

The central piece of state the whole system revolves around is **book availability** — how many copies of a title are on the shelf right now. Every borrow decreases it, every return increases it, and the waitlist exists precisely because it can reach zero. Rather than store availability as a separate flag that can drift out of sync, we derive it from a copy count that is updated in the same transaction as the loan itself.

**Scope note.** Deploying a real website adds two things a desktop application did not need: user accounts with login, and a hosting environment. Both are accounted for in sections 3 and 5. This is a meaningful increase in scope over a local desktop app, and the build order in section 2.6 is arranged so that a working system exists early and deployment concerns come last.

---

## 2. Functional Requirements

### 2.1 Catalog Management (Librarian)

| Function | Description |
|---|---|
| Add Book | Register a new title with ISBN, title, author, category, and number of copies. |
| Edit Book | Update the details or copy count of an existing title. |
| Remove Book | Delete a title from the catalog. Blocked while any copy is on loan. |

### 2.2 Member Management (Librarian)

| Function | Description |
|---|---|
| Register Member | Create a member record with name and email. Assigns a member ID and active status. |

### 2.3 Catalog Access (Member, Librarian)

| Function | Description |
|---|---|
| Search Books | Search by title, author, or category. Results display current availability. |

### 2.4 Circulation (Member)

| Function | Description |
|---|---|
| Borrow Book | Borrow an available copy. Records the loan, sets a due date, decrements available copies. |
| Return Book | Close an open loan, increment available copies, promote the next member on the waitlist. |
| Join Waitlist | Join the queue for a title with no available copies. See section 2.5. |
| View Borrowing History | List the member's past and current loans with borrow, due, and return dates. |

### 2.5 Waitlist Model

Each title has its own waitlist. When a member requests a book with no available copies, they are added to that title's waitlist and shown their position in line.

**Rules:**

- **One spot per member per title.** A member cannot occupy two positions on the same waitlist. Attempting to join a waitlist they are already on is rejected.
- **A member may be on many different waitlists at once.** There is no limit on the number of distinct titles a member can be waiting for.
- **Ordering is first-come, first-served**, by the timestamp the member joined.
- **Joining is only permitted when no copies are available.** If a copy is on the shelf, the member borrows it instead.
- **On return, the member at the front of the waitlist is promoted** to `READY`, meaning a copy is being held for them. This promotion happens inside the same transaction as the return.
- A `READY` hold that is not collected expires after a set number of days and passes to the next member. *(Duration is an open question — see section 6.)*

**Waitlist entry states:**

| State | Meaning |
|---|---|
| `ACTIVE` | Waiting in line |
| `READY` | A copy is being held for this member |
| `FULFILLED` | The member borrowed the held copy |
| `CANCELLED` | The member left the waitlist, or the hold expired |

The "one spot per title" rule is enforced twice: in the service layer with a clear error message, and in the database with a partial unique index on `(book_id, member_id)` restricted to `ACTIVE` entries. The database constraint is the backstop — it makes the rule impossible to violate even if a service-layer check is missed.

### 2.6 Validation Rules

- ISBN must be well-formed and unique across the catalog.
- Member email must be well-formed and unique. Email doubles as the login identifier.
- Borrowing is refused when available copies is zero, or the member is suspended.
- **There is no cap on the number of books a member may have on loan at once.**
- Waitlist entry is permitted only when available copies is zero, and only once per member per title.
- Due date is set to borrow date plus 14 days. Overdue status is computed at read time from the due date, never stored.
- A book cannot be removed while any copy is on loan.

### 2.7 Build Order

Core functionality first, deployment last:

1. Book CRUD
2. Member registration and login
3. Borrow and return
4. Search
5. Waitlist
6. Borrowing history
7. Deploy

---

## 3. Language and Architecture

**Language:** Java

**Framework:** Spring Boot

**Interface:** Server-rendered HTML using Thymeleaf templates.

### 3.1 Why Spring Boot

A deployed website needs HTTP routing, session handling, form binding, login, and a packaging story for the server. Spring Boot provides all of these, and it is the standard Java choice for this kind of application — which also means the team can find answers when stuck.

Server-rendered Thymeleaf is proposed over a separate JavaScript frontend because it keeps the project to one language and one deployable artifact. A React frontend would mean a second build pipeline, CORS configuration, and a second thing to host, for no gain the requirements ask for.

### 3.2 Layered Architecture

```
Controller Layer  →  Service Layer  →  Repository Layer  →  Database
(HTTP, templates)    (business rules)   (persistence)
```

| Package | Responsibility |
|---|---|
| `com.library.controller` | HTTP endpoints and form handling. No business rules. |
| `com.library.service` | Validation and multi-step operations. All business rules live here. |
| `com.library.repository` | Data access. All queries live here. |
| `com.library.domain` | `Book`, `Member`, `Loan`, `WaitlistEntry` — JPA entities |
| `com.library.config` | Security and application configuration |

This is the same layering as the desktop design, with the GUI layer replaced by controllers and templates. The service layer — where all the business rules live — carries over essentially unchanged.

**Two rules that keep the layers honest:**

1. A controller never touches a repository directly. It goes through a service.
2. No queries outside the repository layer.

### 3.3 Authentication

Because this is a public website rather than a single-user desktop app, accounts are required.

- Spring Security with session-based form login.
- Two roles: `LIBRARIAN` and `MEMBER`. Catalog and member management are restricted to `LIBRARIAN`.
- Passwords stored as BCrypt hashes. Never in plain text.
- Login identifier is the member's email address.

This is a new requirement introduced by moving to the web. It was not needed in the desktop design.

### 3.4 Data Model

```
Book(id, isbn, title, author, category, totalCopies, availableCopies)
Member(id, name, email, passwordHash, role, joinDate, status)
Loan(id, bookId, memberId, borrowDate, dueDate, returnDate)
WaitlistEntry(id, bookId, memberId, joinedAt, status)
```

Two design decisions worth knowing before writing code against this:

**Availability is a count, not a flag.** `availableCopies` is decremented on borrow and incremented on return, always inside the same transaction as the loan write. Never add a separate `isAvailable` boolean — it will drift out of sync.

**Overdue is computed, never stored.** Overdue status compares the due date to today. There is no `overdue` column and there should not be one.

---

## 4. Database

**Decision: a SQL relational database, accessed through Spring Data JPA.**

The reasoning from the earlier revision stands. `Loan` and `WaitlistEntry` each reference both a book and a member, so the data is inherently relational; borrowing requires two coordinated writes and therefore needs transactions; and search maps directly onto a query. A flat-file approach would mean hand-writing joins and losing transactional safety — the two areas most likely to produce bugs.

### 4.1 Which SQL database

Moving from a desktop app to a deployed website changes this answer, and it is worth being explicit about why.

| Environment | Database | Reason |
|---|---|---|
| Local development | SQLite (file) | Zero setup. Every teammate runs the app immediately after cloning. |
| Deployed production | **PostgreSQL** | Survives restarts and redeploys. Handles concurrent users. |

**Why not SQLite in production.** SQLite stores the database in a file next to the application. Most hosting platforms give containers an *ephemeral* filesystem — when the app restarts or redeploys, that file is deleted and every book, member, and loan goes with it. SQLite also serializes writes, which is fine for one desktop user and not for concurrent web traffic.

This is the single most common way a student web project loses its data the week it is demonstrated, and it is worth avoiding deliberately rather than discovering.

**Why this costs us almost nothing.** Because we access the database through JPA, the same entity classes and the same repository interfaces work against both engines. Switching is a configuration change — a different JDBC URL and driver — not a code change. We develop against SQLite for convenience and deploy against PostgreSQL for durability.

If the team would rather avoid running two engines, we use PostgreSQL in both places via Docker locally. That is more setup on each machine but removes any development-versus-production difference.

### 4.2 What this commits us to

- Dependencies: `spring-boot-starter-data-jpa`, plus the `sqlite-jdbc` and `postgresql` drivers.
- Schema generated from the JPA entity definitions, with `schema.sql` kept as the readable reference.
- All multi-step writes (borrow, return) annotated `@Transactional`.
- Uniqueness on ISBN and member email enforced by database constraints in addition to service-layer validation.
- The "one waitlist spot per member per title" rule enforced by a partial unique index.
- The generated development database file is **not** committed to the repository.

---

## 5. Deployment

**Target:** a platform-as-a-service with a managed PostgreSQL add-on — Render, Railway, or Fly.io. All three have free or low-cost tiers adequate for this project and can build a Spring Boot application directly from a GitHub repository.

**Approach:**

- The application is packaged as a single executable JAR (`mvn package`).
- Database credentials and any secrets are supplied as environment variables, never committed to the repository.
- Pushing to `main` triggers a redeploy.
- The database is a managed PostgreSQL instance provided by the platform, not a file inside the container.

**Deployment is scheduled last in the build order deliberately.** It should happen once the core features work, but not so late that a deployment problem is discovered the night before submission. A reasonable target is to deploy a skeleton version early — even one that only lists books — so the pipeline is proven, then redeploy as features land.

---

## 6. Open Questions for the Team

1. Loan period — is 14 days reasonable?
2. How long should a `READY` waitlist hold last before it expires and passes to the next member?
3. Do we need a librarian view of *all* loans, or is per-member history sufficient?
4. Should members be notified when their waitlist hold becomes ready? Email adds a mail service dependency; an in-app notice on login does not.
5. Which hosting platform?
6. Do librarians self-register, or is the first librarian account seeded manually?

---

## Appendix — Changes from v1

| Change | Effect |
|---|---|
| Desktop app → deployed website | JavaFX replaced by Spring Boot and Thymeleaf. Adds authentication and hosting. |
| Database confirmed as SQL | Now via Spring Data JPA. PostgreSQL in production, SQLite for local development. |
| Reservations → explicit waitlist model | One spot per member per title; unlimited distinct titles. States and promotion rules defined in section 2.5. |
| Borrowing cap removed | The five-book limit is gone. Suspended status and copy availability remain the only borrowing restrictions. |
| Phone number removed | Registration captures name and email only. |
