# Expanded FITOS Demo Data Implementation Plan

**Goal:** Add two receptionist accounts and useful repeatable demo records across CRM, members, sales, classes, bookings, PT and POS, then reload all FITOS services.

**Architecture:** Extend the local-only API seeder; preserve existing data and use stable account emails, order notes, SKU and POS request IDs to avoid duplicates. Keep the generated demo password in the ignored local credentials file. Run the seed against studio 1, then rebuild/restart only FITOS processes and verify API/UI counts.

**Tech Stack:** Python 3.12, Spring Boot, Next.js, PostgreSQL, Docker Compose.

## Constraints

- Do not reset or replace the database or existing baseline dump.
- Use localhost API only and keep account credentials out of Git.
- Avoid touching unrelated Docker containers or other projects.
- Verify every generated account can log in and each business record remains in the current studio.

## Tasks

- [x] Add two receptionist and additional member definitions; preserve/update `.dsh/demo-accounts.json` metadata without changing its password.
- [x] Add repeatable leads, paid plan orders, sessions, bookings, PT sessions, catalog products, stock entries and paid POS orders.
- [x] Extend integration checks with minimum counts and key relationships; run seed twice to prove no duplicates.
- [x] Restart FITOS frontend/backend; verify PostgreSQL, Redis, AI services, backend and frontend health plus browser data. No application code changed, so existing build artifacts were reused.
