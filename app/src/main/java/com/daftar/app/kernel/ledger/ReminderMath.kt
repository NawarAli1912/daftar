package com.daftar.app.kernel.ledger

import java.time.LocalDate
import java.time.temporal.ChronoUnit

// Reminders (Flow 2) — the pure date logic behind the المواعيد book.
// A debt gets a due date; mom reminds, then snoozes it forward. Settling drops the
// customer off the list (balance ≤ 0), so "cancel on settle" (FR-3.3) is structural.
// java.time is available natively at minSdk 26 — no desugaring.
object ReminderMath {

    // D14: one tap pushes the due date forward by mom's choice.
    enum class Snooze { WEEK, TWO_WEEKS, MONTH }

    enum class Urgency { OVERDUE, DUE_SOON, UPCOMING }

    // Anything due within this many days is "soon" (amber, not yet late).
    const val SOON_DAYS = 3

    // D6: customers pay when salaries land at the start of the month, so the
    // default due date is the 1st of next month — one tap to change.
    fun defaultDue(today: LocalDate): LocalDate = today.plusMonths(1).withDayOfMonth(1)

    // Snooze is measured from today ("remind me again in a week"), never from an
    // already-overdue date — so a week's snooze is always a full week away.
    fun snoozed(today: LocalDate, snooze: Snooze): LocalDate = when (snooze) {
        Snooze.WEEK -> today.plusWeeks(1)
        Snooze.TWO_WEEKS -> today.plusWeeks(2)
        Snooze.MONTH -> today.plusMonths(1)
    }

    // Negative = overdue by that many days.
    fun daysUntil(due: LocalDate, today: LocalDate): Long = ChronoUnit.DAYS.between(today, due)

    fun urgency(due: LocalDate, today: LocalDate): Urgency {
        val days = daysUntil(due, today)
        return when {
            days < 0 -> Urgency.OVERDUE
            days <= SOON_DAYS -> Urgency.DUE_SOON
            else -> Urgency.UPCOMING
        }
    }
}
