package com.daftar.app.reminders

import com.daftar.app.kernel.db.CustomerEntity
import com.daftar.app.kernel.db.LedgerEntryEntity
import com.daftar.app.kernel.db.ReminderEntity
import com.daftar.app.kernel.ledger.EntryKind
import com.daftar.app.kernel.ledger.LedgerLine
import com.daftar.app.kernel.ledger.LedgerMath
import com.daftar.app.kernel.ledger.ReminderMath
import java.time.LocalDate

// The reminder book: who owes, when they're due. Pure so the tab (RemindersViewModel)
// and the daily notification worker (RemindersWorker) compute it identically.
object ReminderBook {

    data class Entry(
        val customer: CustomerEntity,
        val balance: Long,
        val due: LocalDate,
        val daysUntil: Long,
        val urgency: ReminderMath.Urgency,
    )

    fun build(
        customers: List<CustomerEntity>,
        entries: List<LedgerEntryEntity>,
        reminders: List<ReminderEntity>,
        today: LocalDate,
    ): List<Entry> {
        val entriesByCustomer = entries.groupBy { it.customerId }
        val dueByCustomer = reminders.associate { it.customerId to LocalDate.ofEpochDay(it.dueEpochDay) }
        return customers.mapNotNull { customer ->
            val balance = LedgerMath.balance(
                entriesByCustomer[customer.id].orEmpty()
                    .map { LedgerLine(EntryKind.valueOf(it.kind), it.amount, it.voided) }
            )
            if (balance <= 0) return@mapNotNull null // settled → no reminder (FR-3.3)
            val due = dueByCustomer[customer.id] ?: ReminderMath.defaultDue(today)
            Entry(
                customer = customer,
                balance = balance,
                due = due,
                daysUntil = ReminderMath.daysUntil(due, today),
                urgency = ReminderMath.urgency(due, today),
            )
        }.sortedBy { it.due }
    }

    // Whom to nudge today: due today or already overdue.
    fun due(entries: List<Entry>): List<Entry> = entries.filter { it.daysUntil <= 0L }
}
