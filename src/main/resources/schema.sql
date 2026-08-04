-- Library Management System — database schema.
--
-- Applied automatically on first run by com.library.persistence.Database.
-- Every statement is idempotent, so re-running against an existing library.db
-- is safe. The generated library.db is NOT committed; this file is.

CREATE TABLE IF NOT EXISTS book (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    isbn             TEXT    NOT NULL UNIQUE,
    title            TEXT    NOT NULL,
    author           TEXT    NOT NULL,
    category         TEXT,
    total_copies     INTEGER NOT NULL CHECK (total_copies >= 0),
    available_copies INTEGER NOT NULL CHECK (available_copies >= 0),
    CHECK (available_copies <= total_copies)
);

CREATE TABLE IF NOT EXISTS member (
    id        INTEGER PRIMARY KEY AUTOINCREMENT,
    name      TEXT NOT NULL,
    email     TEXT NOT NULL UNIQUE,
    join_date TEXT NOT NULL,
    status    TEXT NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'SUSPENDED'))
);

CREATE TABLE IF NOT EXISTS loan (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    book_id     INTEGER NOT NULL REFERENCES book(id),
    member_id   INTEGER NOT NULL REFERENCES member(id),
    borrow_date TEXT    NOT NULL,
    due_date    TEXT    NOT NULL,
    -- NULL means the loan is still open. Overdue is computed from due_date at
    -- read time, never stored.
    return_date TEXT
);

CREATE TABLE IF NOT EXISTS reservation (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    book_id       INTEGER NOT NULL REFERENCES book(id),
    member_id     INTEGER NOT NULL REFERENCES member(id),
    reserved_date TEXT    NOT NULL,
    status        TEXT    NOT NULL DEFAULT 'WAITING'
                  CHECK (status IN ('WAITING', 'READY', 'FULFILLED', 'CANCELLED'))
);

-- Open loans per member (active-loan cap) and per book (blocks book removal).
CREATE INDEX IF NOT EXISTS ix_loan_member_open ON loan(member_id) WHERE return_date IS NULL;
CREATE INDEX IF NOT EXISTS ix_loan_book_open   ON loan(book_id)   WHERE return_date IS NULL;

-- The reservation queue is read in reservation-date order per title.
CREATE INDEX IF NOT EXISTS ix_reservation_queue ON reservation(book_id, reserved_date);

-- "Only once per member per title" applies to live reservations only; a member
-- may reserve the same title again after an earlier one is fulfilled or cancelled.
CREATE UNIQUE INDEX IF NOT EXISTS ux_reservation_live
    ON reservation(book_id, member_id) WHERE status IN ('WAITING', 'READY');
