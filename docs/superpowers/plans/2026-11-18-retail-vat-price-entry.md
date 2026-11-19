# Retail VAT Price Entry Implementation Plan

> **For agentic workers:** Implement the steps in order and verify each result. This plan uses the user's requested flow as its specification.

**Goal:** Let managers enter cost, selling price before VAT, and VAT rate; show and save the calculated customer price including VAT.

**Architecture:** Keep the existing `ProductDto.sellPrice` API and database value as the VAT-inclusive price used by POS. Convert the value only at the product form boundary. Preserve an unchanged existing gross price exactly when editing other product details.

**Tech Stack:** Next.js 14, React, TypeScript, Playwright.

**Spec:** User request in the current conversation: enter cost, selling price, VAT; calculate the selling price including VAT.

## Global Constraints

- Keep existing product prices and POS totals unchanged until a manager edits the price or VAT rate.
- Round monetary API values to two decimal places to match the database and backend pricing.
- Show the final customer price before saving.
- Reject invalid negative prices and VAT rates outside 0–100%.

## Review Focus

- Editing a seeded product without changing its price must not apply VAT a second time.
- Changing VAT on an existing product must recalculate the customer price from the displayed pre-VAT price.
- Blank and invalid input must not become zero silently.
- The preview and saved API payload must match.

## Tasks

- [x] Add failing price conversion tests for create, edit, and unchanged gross preservation.
- [x] Add pure conversion helpers and pass the tests.
- [x] Update the product form labels, preview, validation, and API payload.
- [x] Verify a browser create/edit flow, run build and tests, then restart the frontend.
