# Four-tab statistics implementation plan

> **For agentic workers:** Implement each checkbox in this workspace and verify before marking it done.

**Goal:** Build one manager statistics page with four tabs for date range invoices, period income and expense, employee/customer revenue, and product revenue.

**Architecture:** Reuse the existing `/api/analytics/overview` aggregates and Excel/PDF exports. Add a focused transaction list API for the date tab and a metadata API for data-driven years. Keep each tab's filters above its results and exports below. The backend owns date boundaries, studio scoping, counts, and money; the frontend handles display, sorting, and row selection.

**Tech Stack:** Spring Boot/JdbcTemplate/PostgreSQL, Next.js/React/TypeScript, Playwright.

**Spec:** User's four-tab statistics layout and behavior in this conversation.

## Files

- `backend/src/main/java/com/fitos/analytics/AnalyticsDtos.java`: range counts and transaction/years contracts.
- `backend/src/main/java/com/fitos/analytics/AnalyticsController.java`: manager-only list and metadata endpoints.
- `backend/src/main/java/com/fitos/analytics/AnalyticsService.java`: scoped date range transaction query and dynamic year bounds.
- `frontend/src/app/dashboard/reports/page.tsx`: tab shell, filters, cards, sortable tables, chart, drill-down, exports.
- `frontend/tests/analytics_workflow.spec.ts`: browser behavior.
- `docs/INVOICES_PURCHASE_REPORTS.md`: report definitions and API rules.

## Tasks

- [x] Add a failing browser test for all four tabs, their filters, totals and export controls.
- [x] Verify date range, sale/purchase reconciliation, manager access, and year bounds against local demo APIs.
- [x] Add paginated transaction rows including confirmed purchase receipts and refunded POS documents.
- [x] Add data-driven available years for employee/customer columns.
- [x] Implement tab (a): validated dates, summary cards, transaction list and selected details.
- [x] Implement tabs (b), (c), (d) using the overview API, including period switch, employee/customer switch, year selectors, sorting, total row and product search.
- [x] Add grouped income/expense chart, cell drill-down, Excel/PDF actions, empty/loading/error states and responsive layout.
- [x] Build, test, inspect the running page, and update documentation.

## Review focus

- Date range is inclusive in Vietnam time and invalid dates never reach the API.
- Returns reduce POS totals only on their refund date.
- Purchase receipts appear only after confirmation and never in sales.
- All queries remain scoped to the current studio and manager role.
- Sorting and product filtering do not move or corrupt the TC row.
