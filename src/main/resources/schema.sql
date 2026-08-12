-- Library Management System — schema reference.
--
-- NOT executed by the application. Hibernate generates the real schema from the
-- JPA entities in com.library.domain. This file is the readable statement of
-- what those entities are meant to produce, in PostgreSQL syntax.
--
-- Keep it in step with the entities. Where the two disagree, the entities win at
-- runtime and this file is the bug.

CREATE TABLE book (
    id               BIGSERIAL PRIMARY KEY,
    isbn             VARCHAR(20)  NOT NULL UNIQUE,
    title            VARCHAR(255) NOT NULL,
    author           VARCHAR(255) NOT NULL,
    category         VARCHAR(100),
    total_copies     INTEGER      NOT NULL CHECK (total_copies >= 0),
    available_copies INTEGER      NOT NULL CHECK (available_copies >= 0),
    -- Optimistic lock, maintained by Hibernate (@Version on Book).
    version          BIGINT,
    CHECK (available_copies <= total_copies)
);

CREATE TABLE member (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(255) NOT NULL,
    -- Doubles as the login identifier.
    email         VARCHAR(255) NOT NULL UNIQUE,
    -- BCrypt hash. Never a plain-text password.
    password_hash VARCHAR(60)  NOT NULL,
    role          VARCHAR(20)  NOT NULL DEFAULT 'MEMBER' CHECK (role IN ('MEMBER', 'LIBRARIAN')),
    join_date     DATE         NOT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'SUSPENDED'))
);

CREATE TABLE loan (
    id          BIGSERIAL PRIMARY KEY,
    book_id     BIGINT NOT NULL REFERENCES book(id),
    member_id   BIGINT NOT NULL REFERENCES member(id),
    borrow_date DATE   NOT NULL,
    due_date    DATE   NOT NULL,
    -- NULL means the loan is still open. Overdue is derived from due_date at
    -- read time, never stored.
    return_date DATE
);

CREATE TABLE waitlist_entry (
    id        BIGSERIAL PRIMARY KEY,
    book_id   BIGINT      NOT NULL REFERENCES book(id),
    member_id BIGINT      NOT NULL REFERENCES member(id),
    -- Queue position is first-come, first-served on this timestamp.
    joined_at TIMESTAMP   NOT NULL,
    status    VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
              CHECK (status IN ('ACTIVE', 'READY', 'FULFILLED', 'CANCELLED')),
    -- Set when the entry is promoted to READY; the hold expires a fixed number
    -- of days after this (CirculationPolicy.HOLD_PERIOD_DAYS).
    ready_at  TIMESTAMP
);

-- Open loans per book: blocks removing a title while a copy is out.
-- There is no per-member loan cap, so no equivalent index is needed for members.
CREATE INDEX ix_loan_book_open ON loan(book_id) WHERE return_date IS NULL;
CREATE INDEX ix_loan_member ON loan(member_id);

-- The waitlist is read in join order per title.
CREATE INDEX ix_waitlist_queue ON waitlist_entry(book_id, joined_at);

-- "One spot per member per title" — the database backstop to the service-layer
-- check. Covers both open states, matching WaitlistService.OPEN_STATUSES, so a
-- promoted READY hold still occupies the member's one spot. Closed entries are
-- excluded, so a member may rejoin after an entry is fulfilled or cancelled.
CREATE UNIQUE INDEX ux_waitlist_open ON waitlist_entry(book_id, member_id)
    WHERE status IN ('ACTIVE', 'READY');
