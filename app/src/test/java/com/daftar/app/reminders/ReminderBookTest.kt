package com.daftar.app.reminders

import com.daftar.app.kernel.db.CustomerEntity
import com.daftar.app.kernel.db.LedgerEntryEntity
import com.daftar.app.kernel.db.ReminderEntity
import com.daftar.app.kernel.ledger.EntryKind
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderBookTest {

    private val today = LocalDate.of(2026, 7, 4)

    private fun customer(id: String, name: String) = CustomerEntity(id, name, null, 0, 0)

    private fun ledger(customerId: String, kind: EntryKind, amount: Long, voided: Boolean = false) =
        LedgerEntryEntity("e-${customerId}-${kind}-$amount", kind.name, customerId, amount, 0, 0, voided)

    private fun reminder(customerId: String, due: LocalDate) =
        ReminderEntity("r-$customerId", customerId, due.toEpochDay(), 0, 0)

    @Test
    fun `only customers who owe appear`() {
        val result = ReminderBook.build(
            customers = listOf(customer("a", "دائنة"), customer("b", "مسددة")),
            entries = listOf(
                ledger("a", EntryKind.OPENING_BALANCE, 10_000),
                ledger("b", EntryKind.OPENING_BALANCE, 5_000),
                ledger("b", EntryKind.PAYMENT, 5_000), // settled → dropped
            ),
            reminders = emptyList(),
            today = today,
        )
        assertEquals(listOf("دائنة"), result.map { it.customer.name })
        assertEquals(10_000, result.single().balance)
    }

    @Test
    fun `missing reminder falls back to the default due date (D6)`() {
        val result = ReminderBook.build(
            customers = listOf(customer("a", "أ")),
            entries = listOf(ledger("a", EntryKind.OPENING_BALANCE, 10_000)),
            reminders = emptyList(),
            today = today,
        )
        assertEquals(LocalDate.of(2026, 8, 1), result.single().due)
    }

    @Test
    fun `stored reminder overrides the default and drives sort order`() {
        val result = ReminderBook.build(
            customers = listOf(customer("a", "أ"), customer("b", "ب")),
            entries = listOf(
                ledger("a", EntryKind.OPENING_BALANCE, 1_000),
                ledger("b", EntryKind.OPENING_BALANCE, 1_000),
            ),
            reminders = listOf(
                reminder("a", today.plusDays(5)),
                reminder("b", today.minusDays(1)), // overdue → sorts first
            ),
            today = today,
        )
        assertEquals(listOf("ب", "أ"), result.map { it.customer.name })
    }

    @Test
    fun `due returns only today-or-overdue entries`() {
        val entries = ReminderBook.build(
            customers = listOf(customer("a", "متأخرة"), customer("b", "اليوم"), customer("c", "لاحقاً")),
            entries = listOf(
                ledger("a", EntryKind.OPENING_BALANCE, 1_000),
                ledger("b", EntryKind.OPENING_BALANCE, 1_000),
                ledger("c", EntryKind.OPENING_BALANCE, 1_000),
            ),
            reminders = listOf(
                reminder("a", today.minusDays(2)),
                reminder("b", today),
                reminder("c", today.plusDays(4)),
            ),
            today = today,
        )
        val due = ReminderBook.due(entries)
        assertEquals(listOf("متأخرة", "اليوم"), due.map { it.customer.name })
        assertTrue(due.all { it.daysUntil <= 0 })
    }
}
