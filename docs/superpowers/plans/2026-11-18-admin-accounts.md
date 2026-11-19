# Admin Account Management Implementation Plan

**Goal:** Admin sees only account management, can create, edit, change roles, lock and delete accounts; other roles cannot use account APIs.

**Architecture:** Keep account operations in `/api/users`, require `ROLE_ADMIN` at the controller, and verify target accounts belong to the selected studio. Hard deletion removes the login identity while existing business records remain through existing `ON DELETE SET NULL` foreign keys. The frontend routes Admin to `/dashboard/users` and hides other workspaces.

**Tech stack:** Spring Boot, PostgreSQL, Next.js, Playwright.

**Review focus:** Manager API denial; self/last-admin protection; duplicate email; linked customer/trainer profile consistency; deletion preserving orders and memberships.

- [ ] Add failing browser/API tests for Admin-only access and account create/edit/delete.
- [ ] Restrict `/api/users` to Admin and implement validated edit/delete with studio ownership checks.
- [ ] Make Admin's frontend navigation and landing page account-only; add edit/delete controls.
- [ ] Build backend/frontend, run relevant tests, verify the running app and inspect the diff.
