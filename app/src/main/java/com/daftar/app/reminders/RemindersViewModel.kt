package com.daftar.app.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daftar.app.kernel.db.CustomerDao
import com.daftar.app.kernel.db.CustomerEntity
import com.daftar.app.kernel.db.LedgerDao
import com.daftar.app.kernel.db.ReminderDao
import com.daftar.app.kernel.db.ReminderEntity
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
            ReminderBook.build(customers, entries, reminders, LocalDate.now())
                .map { Row(it.customer, it.balance, it.due, it.daysUntil, it.urgency) }
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
