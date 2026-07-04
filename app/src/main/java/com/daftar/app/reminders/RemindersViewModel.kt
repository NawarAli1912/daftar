package com.daftar.app.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daftar.app.kernel.db.CustomerDao
import com.daftar.app.kernel.db.CustomerEntity
import com.daftar.app.kernel.db.LedgerDao
import com.daftar.app.kernel.db.ReminderDao
import com.daftar.app.kernel.db.ReminderEntity
import com.daftar.app.kernel.ledger.EntryKind
import com.daftar.app.kernel.ledger.LedgerLine
import com.daftar.app.kernel.ledger.LedgerMath
import com.daftar.app.kernel.ledger.ReminderMath
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class RemindersViewModel @Inject constructor(
    customerDao: CustomerDao,
    ledgerDao: LedgerDao,
    private val reminderDao: ReminderDao,
) : ViewModel() {

    data class Row(
        val customer: CustomerEntity,
        val balance: Long,
        val due: LocalDate,
        val daysUntil: Long,
        val urgency: ReminderMath.Urgency,
    )

    // Only customers who owe appear (FR-3.3: settling drops them off automatically).
    // Sorted soonest/overdue first — the top of the list is who to chase now.
    val rows: StateFlow<List<Row>> =
        combine(
            customerDao.observeAll(),
            ledgerDao.observeAll(),
            reminderDao.observeAll(),
        ) { customers, entries, reminders ->
            val today = LocalDate.now()
            val entriesByCustomer = entries.groupBy { it.customerId }
            val dueByCustomer = reminders.associate { it.customerId to LocalDate.ofEpochDay(it.dueEpochDay) }
            customers.mapNotNull { customer ->
                val lines = entriesByCustomer[customer.id].orEmpty()
                    .map { LedgerLine(EntryKind.valueOf(it.kind), it.amount, it.voided) }
                val balance = LedgerMath.balance(lines)
                if (balance <= 0) return@mapNotNull null
                val due = dueByCustomer[customer.id] ?: ReminderMath.defaultDue(today)
                Row(
                    customer = customer,
                    balance = balance,
                    due = due,
                    daysUntil = ReminderMath.daysUntil(due, today),
                    urgency = ReminderMath.urgency(due, today),
                )
            }.sortedBy { it.due }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun snooze(customerId: String, snooze: ReminderMath.Snooze) {
        viewModelScope.launch {
            val dueEpochDay = ReminderMath.snoozed(LocalDate.now(), snooze).toEpochDay()
            val now = System.currentTimeMillis()
            if (reminderDao.findByCustomer(customerId) == null) {
                reminderDao.insert(
                    ReminderEntity(
                        id = UUID.randomUUID().toString(),
                        customerId = customerId,
                        dueEpochDay = dueEpochDay,
                        createdAt = now,
                        updatedAt = now,
                    )
                )
            } else {
                reminderDao.updateDue(customerId, dueEpochDay, now)
            }
        }
    }
}
