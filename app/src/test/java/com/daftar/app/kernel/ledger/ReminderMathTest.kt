package com.daftar.app.kernel.ledger

import com.daftar.app.kernel.ledger.ReminderMath.Snooze
import com.daftar.app.kernel.ledger.ReminderMath.Urgency
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderMathTest {

    @Test
    fun `default due date is the first of next month (D6)`() {
        assertEquals(LocalDate.of(2026, 8, 1), ReminderMath.defaultDue(LocalDate.of(2026, 7, 4)))
    }

    @Test
    fun `default due rolls the year over in December`() {
        assertEquals(LocalDate.of(2027, 1, 1), ReminderMath.defaultDue(LocalDate.of(2026, 12, 15)))
    }

    @Test
    fun `snooze pushes forward from today, not from the old due date (D14)`() {
        val today = LocalDate.of(2026, 7, 4)
        assertEquals(LocalDate.of(2026, 7, 11), ReminderMath.snoozed(today, Snooze.WEEK))
        assertEquals(LocalDate.of(2026, 7, 18), ReminderMath.snoozed(today, Snooze.TWO_WEEKS))
        assertEquals(LocalDate.of(2026, 8, 4), ReminderMath.snoozed(today, Snooze.MONTH))
    }

    @Test
    fun `days until is negative when overdue`() {
        val today = LocalDate.of(2026, 7, 4)
        assertEquals(3, ReminderMath.daysUntil(LocalDate.of(2026, 7, 7), today))
        assertEquals(0, ReminderMath.daysUntil(today, today))
        assertEquals(-2, ReminderMath.daysUntil(LocalDate.of(2026, 7, 2), today))
    }

    @Test
    fun `urgency buckets past, soon and later`() {
        val today = LocalDate.of(2026, 7, 4)
        assertEquals(Urgency.OVERDUE, ReminderMath.urgency(LocalDate.of(2026, 7, 3), today))
        assertEquals(Urgency.DUE_SOON, ReminderMath.urgency(today, today))
        assertEquals(Urgency.DUE_SOON, ReminderMath.urgency(LocalDate.of(2026, 7, 7), today))
        assertEquals(Urgency.UPCOMING, ReminderMath.urgency(LocalDate.of(2026, 7, 8), today))
    }
}
