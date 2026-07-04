# Daftar (دفتر)

A ledger app for a real used-clothing shop, built for a single real user: the shop's owner.
Arabic-only, RTL, offline-first Android.

**The one test every feature passes: the owner records a sale, unaided, in under 15 seconds.**

## What it does

- Customers, debts and partial payments — the paper notebook (دفتر), digitized
- Trial items (أمانة) and salary-day payment reminders with one-tap snooze
- Wholesale bales (بالة): cost in USD, price points per type, estimated per-bale profit, aging-driven markdowns
- Works fully offline; syncs opportunistically over a low-bandwidth delta protocol for analytics

## Stack

- **Android:** Kotlin + Jetpack Compose + Room + Hilt (planned)
- **Backend:** ASP.NET Core + PostgreSQL, single-writer timestamp-cursor delta sync (planned)

## Process — design first, documented end to end

1. **Brainstorming & context** — understanding the shop before designing anything
2. **Decision log** — every product decision numbered, with its why
3. **UX prototype before code** — 10 annotated Arabic RTL screens, reviewed with the real user
4. **Event storming → domain model (DDD)** — flows as domain events, then aggregates, then code built as vertical slices around a shared domain kernel (D16)

Work is tracked in ClickUp; each task gets its own branch and merges to `main` as a pull request, with commits referencing their ticket. The design documents (journal, decision log, prototype) are maintained privately and published here with official versions.

## Status

Design phase — prototype v2 awaiting user review.
