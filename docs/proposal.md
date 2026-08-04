# Library Management System — Project Proposal

**Prepared by:** Magnus Ma
**Status:** Draft for team review

---

## 1. Project Overview

We will build a desktop application that helps a library manage its books, its members, and the borrowing activity between them.

The system serves two kinds of users with different needs. **Librarians** maintain the catalog and register members. **Members** search the catalog, borrow and return books, reserve titles that are currently out, and review their own borrowing history.

The central piece of state the whole system revolves around is **book availability** — how many copies of a title are on the shelf right now. Every borrow decreases it, every return increases it, and reservations exist precisely because it can reach zero. Rather than store availability as a separate flag that can drift out of sync, we derive it from a copy count that is updated in the same operation as the loan itself.

Scope for this project is a single-user desktop application with local persistence. Multi-user access, networked deployment, and authentication beyond a simple role field are out of scope.

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
| Return Book | Close an open loan, increment available copies, promote the next reservation if one exists. |
| Reserve Book | Join the queue for a title with no available copies. Ordered by reservation date. |
| View Borrowing History | List the member's past and current loans with borrow, due, and return dates. |

### 2.5 Validation Rules

- ISBN must be well-formed and unique across the catalog.
- Member email must be well-formed and unique.
- Borrowing is refused when available copies is zero, the member is suspended, or the member has reached the active-loan cap (proposed: 5).
- Reservation is permitted only when available copies is zero, and only once per member per title.
- Due date is set to borrow date plus 14 days. Overdue status is computed at read time from the due date, never stored.
- A book cannot be removed while any copy is on loan.

### 2.6 Build Order

Core functionality first, then extensions as time allows:

1. Book CRUD
2. Member registration
3. Borrow and return
4. Search
5. Reservations
6. Borrowing history

---

## 3. Language and Architecture

**Language:** Java

**UI:** JavaFX, or Swing if the team is already comfortable with it. Either satisfies the requirement; the choice should follow whichever the team can build fastest.

### 3.1 Layered Architecture

```
GUI Layer      →  Service Layer  →  DAO Layer  →  Storage
(JavaFX/Swing)    (business rules)  (persistence)
```

- **GUI Layer** — windows and event handlers. Collects input, displays results. Contains no business rules.
- **Service Layer** — `BookService`, `MemberService`, `LoanService`, `ReservationService`. All validation and multi-step operations live here. This is the only layer that knows a borrow means *both* inserting a loan *and* decrementing a copy count.
- **DAO Layer** — one DAO per entity: `BookDAO`, `MemberDAO`, `LoanDAO`, `ReservationDAO`. Each exposes plain methods such as `findById`, `findByCriteria`, `insert`, `update`, `delete`.
- **Domain classes** — `Book`, `Member`, `Loan`, `Reservation`. Plain data objects with fields, constructors, getters, and `toString()`.

### 3.2 Why the DAO layer matters here

The DAO interface is identical whether it reads from a database or a text file. If we build against the interface, changing our persistence decision later costs one class per entity rather than a rewrite of the application. This is also what lets the two options in the next section stay genuinely open rather than becoming a commitment we cannot walk back.

### 3.3 Data Model

```
Book(id, isbn, title, author, category, totalCopies, availableCopies)
Member(id, name, email, joinDate, status)
Loan(id, bookId, memberId, borrowDate, dueDate, returnDate)
Reservation(id, bookId, memberId, reservedDate, status)
```

`Loan` and `Reservation` each reference both a book and a member. This relational shape is what drives the persistence discussion below.

---

## 4. Persistence: Two Proposed Approaches

The project brief leaves the persistence approach to the team. Two options are realistic. Both are workable; they differ mainly in where the effort lands.

### Option A — SQLite via JDBC (recommended)

A single-file relational database accessed through `java.sql`. No server to install or configure, and the database is one file that can be committed to the repository alongside the code.

**Advantages**

- **Relationships are handled by the database.** A member's borrowing history is one `JOIN` between `Loan` and `Book`. With files, we write that lookup by hand in Java.
- **Transactions.** Borrowing writes to two places: a new loan row and a decremented copy count. In a transaction these either both succeed or both roll back. This is the single strongest argument for Option A — without it, a crash between the two writes leaves availability permanently wrong with no way to detect it.
- **Search is a query.** `SELECT ... WHERE title LIKE ?` covers search by title, author, or category. No hand-written filtering loop.
- **Constraints are declared, not coded.** `UNIQUE` on ISBN and email enforces uniqueness at the storage layer as a backstop to our validation code.

**Costs**

- Roughly two hours of JDBC learning for anyone on the team who has not used it.
- One external dependency (`sqlite-jdbc`) to add to the project.
- Requires care with `PreparedStatement` and closing connections.

### Option B — Delimited Text Files

One plain-text file per entity, loaded into memory at startup and written back on change.

```
books.txt         id|isbn|title|author|category|totalCopies|availableCopies
members.txt       id|name|email|joinDate|status
loans.txt         id|bookId|memberId|borrowDate|dueDate|returnDate
reservations.txt  id|bookId|memberId|reservedDate|status
```

**Advantages**

- No dependencies and no setup. Works with `BufferedReader` and `PrintWriter` from the standard library.
- Files are human-readable, which makes debugging and grading straightforward.
- Every team member can start contributing immediately with no new API to learn.

**Costs**

- **Joins are written by hand.** Displaying borrowing history means loading all loans, loading all books, and matching IDs in a nested loop we write and maintain ourselves. This is where correctness bugs tend to appear.
- **No transactions.** Borrow updates two files. A failure between them corrupts availability silently.
- **Search is a linear scan** over an in-memory list. Acceptable at project scale, but it is code we own rather than a query the database optimizes.

**Implementation notes if we choose Option B**

- Use pipe (`|`) as the delimiter, not comma — commas appear inside names such as `Smith, John`.
- Include a header row in each file.
- Load all four files into `ArrayList` collections at startup; write the entire file on change rather than appending. A partial append corrupts the file.
- Write all affected files from a single method so that the two updates in a borrow happen together.

### Comparison

| | Option A — SQLite | Option B — Text Files |
|---|---|---|
| Setup cost | ~2 hours JDBC learning | None |
| Dependencies | `sqlite-jdbc` | None |
| Joins (history, reports) | `JOIN` query | Hand-written matching loops |
| Transactions | Yes | No |
| Search | SQL query | Linear scan in Java |
| Uniqueness enforcement | Declared constraint | Validation code only |
| Debuggability | Requires a DB viewer | Open the file |
| Effort over project lifetime | Front-loaded | Spread across every service method |

### Decision

**The team has selected Option A: SQLite via JDBC.**

The setup cost is paid once and is small; the cost of Option B recurs in every service method that touches more than one entity. Given that `Loan` and `Reservation` both reference two other entities, and that borrowing requires two coordinated writes, the relational option removes the two areas most likely to produce bugs we would otherwise spend time debugging.

Option B remains documented above as the analysis behind this decision, not as a live alternative.

**What this commits us to:**

- Dependency: `org.xerial:sqlite-jdbc` in `pom.xml`.
- A single database file, `library.db`, generated on first run from `schema.sql`. The generated database is **not** committed to the repository; the schema is.
- All multi-step writes (borrow, return) run inside an explicit JDBC transaction.
- Uniqueness on ISBN and member email is enforced by `UNIQUE` constraints in addition to service-layer validation.
- All SQL uses `PreparedStatement`. No string concatenation into queries.

The DAO layer still isolates persistence from the rest of the application, so this decision remains reversible if it proves wrong.

---

## 5. Open Questions for the Team

1. JavaFX or Swing?
2. Active-loan cap per member — is 5 reasonable?
3. Loan period — is 14 days reasonable?
4. Do we need a librarian view of *all* loans, or is per-member history sufficient?
5. Should reservations expire after some number of days once the book becomes available?
