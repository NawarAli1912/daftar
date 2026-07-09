# Daftar (دفتر) — project guide

Onboarding for a human or a new agent. Read this top to bottom before touching code.

---

## 1. What this is

**Daftar (دفتر, "notebook") is an Android ledger app for one real user: the owner of a
used‑clothing shop (the maintainer's mother).** It digitizes her paper notebook — sales,
customers, debts, partial payments, trial items, and salary‑day reminders — in Arabic, RTL,
and fully offline.

**The golden rule, every feature passes it:** *the owner records a sale, unaided, in under 15
seconds.* When a design choice trades power for speed/clarity, speed wins. She is
non‑technical and elderly; the app must never make her feel it.

**Design north star:** the canonical UI is the V2 prototype at
`docs/design-sessions/V2/daftar-app-v2.html`. The running app is a 1:1 port of it. When in
doubt about layout/copy/interaction, the prototype is the spec — not this file.

---

## 2. Current status (2026-07)

- **rc9 is installed on the owner's phone** (with the adaptive launcher icon) — the 1.0 gate,
  a **real‑world trial on her actual stock**, is now actually running. Feedback from her use
  drives the work; it's not a feature checklist. (The committed `README.md` is stale — it still
  describes a planning phase and a planned .NET backend; ignore that framing.)
- Main line: **`869dzugy6-walking-skeleton`** (pushed to `origin`); `main` is still at the
  initial commit. Current focus branches off it — as of 2026‑07‑09 a **general‑UX polish pass**
  (shared motion vocabulary + normalized type/radius token scales) on
  `ux-motion-standardize-tab-segment-day-transitions`.
- Tagged `v1.0.0-rc1` … `v1.0.0-rc8`; `versionName` is **1.0.0-rc9**. Sideload APKs land on
  `~/Desktop/daftar-1.0.0-*.apk`.
- Persists locally with Room (DB **v15** — a real `MIGRATION_14_15` preserves the ledger across
  updates; destructive fallback remains only for pre‑trial versions). JSON backup/restore via
  the share sheet. Daily debt‑digest notification via `RemindersWorker`. An **optional one‑way
  sync bridge** (`sync/SyncWorker`, FR‑8.3) opportunistically pushes the backup JSON to the
  maintainer's owner‑tools API — never blocks the app.

Architecture question already settled: **client‑only / local‑first is the right foundation**
for a single‑user offline shop ledger. A backend/sync (FR‑8/9 below) is deferred (D18) and only
becomes worthwhile if it ever goes multi‑device; even then "local‑first + optional cloud sync"
beats a full backend.

---

## 3. The domain, in shop language

The model tracks **money and people, not 500 garments** (explicit non‑goal D1). Key terms:

| Term | Meaning |
|------|---------|
| **دفتر اليوم** | The day book — the home tab; today's records, newest first, day totals on top. Flip ‹ › to past days (read‑only). |
| **البضاعة / الرف** | The shelf — what's for sale. A sale suggests **only** from here. Each item has a تسعيرة. |
| **التسعيرة** | The offer / asking price of a shelf item (editable in place; a markdown is just this edit). |
| **المصادر** | Sources = provenance + cost, so she can see which source made money. Three kinds: **قبل التطبيق** (pre‑app stock, no cost basis), **بالة** (a wholesale bale, cost entered in **USD** × today's rate), **شراء من السوق** (market pickings, per‑unit local cost). |
| **غير محدد** | "Unspecified" source — the red dot. Always its own bucket; never nagged as incomplete (FR‑7.2). |
| **أمانة** | A trial: goods lent to a customer to try. **Not a firm sale and not hard debt** — tracked separately (`trialAmount`), followed up, then either converted to a sale or returned. |
| **دين / دفعة / إرجاع** | Debt / a payment against balance / a return (credits the balance, optionally restocks). |
| **دفعت الكل / جزءاً** | Paid in full / paid part now (asks how much; remainder becomes debt). |

Balances are **derived**, never stored: `balance = openingDebt + Σ debtDelta of her entries`.
A balance can go negative (the shop owes her → shown **لها**, not **عليها**). Nothing is ever
deleted — voiding removes the row and its derived effects; edits amend.

---

## 4. Requirements (condensed)

Full contract: **`docs/REQUIREMENTS.md`** (FR‑1…FR‑9, NFR‑1…11, non‑goals, agreement log).
It's AGREED (partial) — pending items in `docs/SESSIONS.md §1` are never implemented. Highlights:

- **FR‑1 Sales:** tap shelf chips to add lines; per‑line price (− / + steppers) with the
  suggested price struck through; a sale needs no customer and a price of 0 is valid; a 10‑second
  **undo bar** replaces confirmation dialogs (undo voids, never deletes).
- **FR‑2 Customers/debts:** name‑only creation; payments/returns hit the **balance**, not an
  invoice; balances may go negative; opening debt (دين قديم) for walk‑ins.
- **FR‑3 Reminders:** debt due date defaults to the 1st of next month; one‑tap snooze
  أسبوع/أسبوعان/شهر; a daily offline notification digest; settling cancels the reminder.
- **FR‑4 Stock/pricing:** sources with optional effort ladder (cost → revenue; +counting →
  estimated profit *تقريباً*); attribution is shelf‑based and correctable after the fact.
- **FR‑5 أمانة:** requires a customer; resolves by convert‑to‑sale or return.
- **FR‑6/7 Nav/stats:** four tabs **اليوم / الزبائن / المواعيد / الحساب**; owner‑facing totals,
  debts, per‑source profit.
- **FR‑8/9 Sync & owner tools:** **P3, deferred** — phone is the source of truth, 100% offline;
  optional single‑writer delta sync to Postgres + a read‑only web dashboard *if ever built*.

**NFRs that constrain everything:** Arabic‑only RTL, no i18n framework (NFR‑2); **offline is a
hard requirement, no feature may block on the network** (NFR‑4); undo/void so no action destroys
data (NFR‑6); minSdk 26 (NFR‑8); vertical slices + shared kernel + Hilt, English identifiers,
every ledger rule has a test named after it (NFR‑10); every change references a ticket (NFR‑11).

**Non‑goals:** no per‑item inventory, no cash‑drawer accounting, no multi‑user, no
multi‑language, no confirmation dialogs.

**Two owner overrides applied after the doc:** numerals render **Western** (0123, not ٠١٢٣);
the USD→local **rate is editable** ("today's rate" on المصادر), not hardcoded.

---

## 5. Architecture

**Vertical slices + a shared kernel** (D16), MVVM with Compose + `StateFlow`, Hilt DI (D17).
A slice reads frontend‑to‑data in one folder. The whole running app is one slice:

**`com.daftar.app.store` — the live app (the V2 prototype port).** `MainActivity` → `StoreApp()`.

- `StoreModel.kt` — pure model + all derivations (`customerBalance`, `sourceViews`, `dueStatus`,
  `normalizeDues`, `followUps`, encode/decode of stock & sale lines). No Android imports; unit‑tested.
- `StoreViewModel.kt` — `@HiltViewModel`; holds `StoreState` (mirrors the prototype's
  `Component.state` 1:1). Every mutation goes through `set { }`, which re‑normalizes due dates.
- `StoreApp.kt` — chrome, tabs, the four screens.
- `StoreSheets.kt` — every bottom sheet / full‑screen overlay (sale, pay, return, add item/source,
  package, customer/entry detail, pickers, confirm, undo toast).
- `StoreUi.kt` — primitives, colors, **design tokens** (type scale `fCaption…fHead`, radius
  scale `rXs…rLg` — use these, never raw sp/dp steps), the spring‑motion helpers
  (`Modifier.tap`, `SlideUp`, `appearProgress`, `riseFade`) and screen‑level transitions
  (`Swap` for tab/segment switches, `PageFlip` for the day book's ‹ › page‑turn).
- `StoreRepository.kt` + `BackupJson.kt` — persistence and JSON import/export.

**`com.daftar.app.kernel`** — cross‑cutting: `db/` (Room — `StoreEntities`, `StoreDao`,
`DaftarDatabase`), `theme/`, `ui/`, `format/`, `i18n/`, and pure `ledger/` math.

**`reminders/RemindersWorker`** — scheduled from `DaftarApp` (`@HiltAndroidApp`) to post the
daily debt digest. **`sync/SyncWorker`** — the opportunistic backup push (§2); both are retained
non‑store pieces.

> **Legacy to retire:** `today/ sales/ payments/ customers/ stock/` and the reminders *screens*
> (plus older `kernel/ledger/*` math and `kernel/db/Entities.kt`+`Daos.kt`) are the pre‑V2 first
> build — **unrouted since the rebuild**. Don't extend them; they're slated for removal. The
> store slice is authoritative.

**Persistence model:** the whole snapshot (seeded, usdRate, sources, shelf, entries, customers)
is saved as one unit — `state.map { snapshot }.distinctUntilChanged().drop(1).collect { repo.save }`.
Transient UI state (open sheets, steppers, `editingId`, `confirm`) is not persisted.

---

## 6. Theme

Light **"ledger paper"** design language (D43/D44 — this reverses the earlier dark‑only D15 that
older docs still mention). Palette in `kernel/theme/Theme.kt`:
paper bg `#E9E5DC`, cards `#FBFAF7`, ink `#211E1A`, dim `#8C857A`, hairline `#E0DACE`,
oxblood `#B23124` (wordmark + margin rule + debt), green `#2F6B3D` (paid), amber `#996410`
(aging / أمانة). Body face **IBM Plex Sans Arabic**; the **دفتر** wordmark is **Amiri** serif.

---

## 7. Build, run, verify

```bash
# Build + unit tests (JAVA_HOME must point at Android Studio's JBR)
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
  ./gradlew :app:testDebugUnitTest :app:assembleDebug --no-daemon
```

**iCloud gotcha:** the repo now lives at `~/dev/daftar` (moved off the iCloud‑synced Desktop),
but the `build.nosync/` redirect (see `app/build.gradle.kts`) is kept, and stray iCloud
conflict‑copy directories from the Desktop era can still lurk in the tree — e.g. empty
`mipmap-xhdpi 216 96`‑style dirs under `res/` break `mergeDebugResources` ("Invalid resource
directory name"); delete them. APK ends up at
`app/build.nosync/outputs/apk/debug/app-debug.apk`.

**Device/emulator:** Pixel 10 Pro emulator (`emulator-5554`) + a physical Samsung over Wi‑Fi ADB
are both used — **pass `-s <serial>` to every adb call** (multiple devices attached). adb path:
`$HOME/Library/Android/sdk/platform-tools/adb`.

**Maintainer tools on device:** «بيانات تجريبية», «مسح الكل» and the sync-URL field are NOT in
الملخّص — they open via a **~2s long-press on the دفتر wordmark** in the app bar (SPEC F4;
the owner's screens carry nothing destructive).

**Verifying UI on device (adb caveats):**
- adb **cannot type Arabic** — drive taps by coordinates. Use `uiautomator dump /sdcard/ui.xml`
  then parse `text="…" bounds="[x1,y1][x2,y2]"` to tap precise element centers.
- Spring/slide **motion cannot be seen in a still screenshot** — verify it compiles, runs, and
  doesn't crash; confirm state changes via the dump, not the animation.

---

## 8. Conventions

- **One branch per unit of work** `<slug>` (historically `<ticket-id>-slug`); **single‑line
  kebab‑case commits** (`fix-edit-cancel-data-loss-...`); merge to `main` as a PR.
  **ClickUp was dropped as the tracker (owner decision, 2026‑07‑09** — see
  `.claude/se-config.md`): specs/plans live in `docs/`, so NFR‑11's "references a ticket"
  is satisfied by descriptive branch/commit slugs, not tracker IDs.
- **Commit/push only when asked.** Don't bump the version or tag a release unprompted.
- **Decisions** are numbered in `docs/DECISIONS.md` (D1…D61+) with their *why*; significant
  choices get a decision entry, not just a commit.
- **Grooming style:** batch owner decisions into `AskUserQuestion` multi‑choice with a short
  description per option and a clear recommendation. The owner is the only stakeholder.
- **Memory:** a persistent per‑project memory lives under
  `~/.claude/projects/-Users-nawarali-dev-daftar/memory/` (indexed by `MEMORY.md`) —
  check it at session start; it records non‑obvious project facts across sessions.
- The binding engineering contract is the framework `RULES.md` referenced in the global
  `~/.claude/CLAUDE.md`; the weekend "session types" agenda is in `docs/SESSIONS.md`.

---

## 9. Local documentation (gitignored — `docs/` is not in the repo)

Rich history lives locally only (published with official versions later). Most useful:

- **`REQUIREMENTS.md`** — the agreed FR/NFR contract (start here for scope).
- **`DECISIONS.md`** — numbered decision log with rationale (the *why* of everything).
- **`GLOSSARY.md`** / **`PROBLEMS.md`** — domain terms and the shop pains (P‑numbers) each FR traces to.
- **`EVENTS.md`** — event‑storming ledger linking design → code → test.
- **`design-sessions/V2/daftar-app-v2.html`** — the canonical UI spec (a bundled runtime page;
  the real design is gzip+base64 inside `<script type="__bundler/*">`).
- `STATE.md`, `README.md` (committed) — **partially stale**; they predate the build‑out and the
  light‑theme + Western‑numeral + editable‑rate decisions. Trust the code and `DECISIONS.md` over them.

---

## 10. Known follow‑ups

- Retire the legacy pre‑V2 slices (§5) + the now‑dead `Onboarding`/`SETUP_CHIPS`/`ONB` in
  `StoreSheets.kt`.
- `today` isn't refreshed on foreground → after midnight (app left open) new entries stamp the
  real day but the day book still shows the launch day; add a lifecycle‑resume refresh.
- دفعة defaults to نقدي — a debt payment recorded without picking the customer silently reduces
  no balance; consider requiring/nudging a customer.
- Optional next architecture step (not a backend): auto‑backup the JSON/DB to the owner's Google
  Drive so nothing is lost if the phone breaks.
- Refresh the committed `README.md` to match reality; a signed release build for 1.0.
