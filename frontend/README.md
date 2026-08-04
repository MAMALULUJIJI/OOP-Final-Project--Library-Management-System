# Library Management System — Frontend Prototype

A static frontend for the Library Management System described in the project
proposal. No build step, no dependencies.

**Run it (Java):** from the project root, `java Server.java` (JDK 18+), then
open http://localhost:8000. Alternatively, open `index.html` directly in a
browser.

All names, books, and numbers on the pages are dummy data. The "signed in as"
identity, the member card, loans, and waitlists get populated by the backend
(the authenticated session and the database) once the Spring Boot application
exists.

## Flow

1. `index.html` — entry screen: choose **Member** or **Librarian**.
2. Each role has its own sign-in page (`login-member.html`, `login-librarian.html`).
3. Members land in the catalog and only ever see member pages (Catalog,
   My Account). Librarians land at the Librarian's Desk and only see staff
   tools. Sign-out lives in the "signed in as" identity area at the top right
   of every page, not in the navigation.

In the Spring Boot build the role separation is enforced by Spring Security
(roles LIBRARIAN and MEMBER, section 3.3 of the proposal); these pages are the
corresponding views.

## Pages

| File | Screen | Proposal section |
|---|---|---|
| `index.html` | Role selection (Member / Librarian) | 3.3 Authentication |
| `login-member.html` | Member sign-in | 3.3 Authentication |
| `login-librarian.html` | Staff sign-in | 3.3 Authentication |
| `catalog.html` | Search the catalog with live availability | 2.3 Catalog Access |
| `book.html` | Title detail, borrow / join waitlist, queue view | 2.4–2.5 Circulation & Waitlist |
| `account.html` | Member library card, current loans, waitlists, history | 2.4 Circulation |
| `admin.html` | Staff dashboard: stats and the catalog ledger | 2.1 Catalog Management |
| `members.html` | Staff: register of all members | 2.2 Member Management |
| `add-book.html` | Staff: add a new title (separate page) | 2.1 Catalog Management |
| `register-member.html` | Staff: register a new member (separate page) | 2.2 Member Management |

## Design

- **Palette** — pastels on warm off-white: sage green, dusty rose, powder
  blue, soft terracotta; defined as CSS variables at the top of
  `css/style.css`. Staff pages use a terracotta top rail so the two areas
  are visually distinct.
- **Type** — Cormorant Garamond (display), Jost (interface), IBM Plex Mono
  (ISBNs and dates), loaded from Google Fonts.
- **Details from the spec are visible in the UI** — overdue is shown as a
  computed stamp, availability is a copy count ("3 of 5 on shelf"), Remove is
  disabled while copies are on loan, waitlist entries show ACTIVE / READY
  states and positions.

## Moving to Thymeleaf

Each page is plain HTML, so the migration path is:

1. Copy the pages into `src/main/resources/templates/` and the `css/`/`js/`
   folders into `src/main/resources/static/`.
2. Replace the hard-coded sample rows (book cards, table rows, queue entries)
   with `th:each` loops over model attributes.
3. Point forms at controller endpoints with `th:action` and `th:object`;
   the sign-in forms become Spring Security's form login, and `/admin/**`
   routes are restricted to the LIBRARIAN role.
4. Extract the shared masthead/nav/footer into a Thymeleaf fragment, with the
   identity area populated from the authenticated principal.

The small script in `js/app.js` (client-side search filter, tabs) can stay
as-is or be replaced by server-side queries once real data exists.
