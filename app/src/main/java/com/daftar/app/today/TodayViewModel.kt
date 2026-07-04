package com.daftar.app.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daftar.app.kernel.db.CustomerDao
import com.daftar.app.kernel.db.LedgerDao
import com.daftar.app.kernel.db.LedgerEntryEntity
import com.daftar.app.kernel.ledger.EntryKind
import com.daftar.app.kernel.ledger.LedgerLine
import com.daftar.app.kernel.ledger.LedgerMath
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class TodayViewModel @Inject constructor(
    ledgerDao: LedgerDao,
    customerDao: CustomerDao,
) : ViewModel() {

    data class Item(val entry: LedgerEntryEntity, val customerName: String?)

    data class State(
        val items: List<Item> = emptyList(),
        val paymentsTotal: Long = 0L,
    )

    private val zone: ZoneId = ZoneId.systemDefault()
    private val dayStart: Long =
        LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
    private val dayEnd: Long =
        LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

    val state: StateFlow<State> =
        combine(
            ledgerDao.observeDay(dayStart, dayEnd),
            customerDao.observeAll(),
        ) { entries, customers ->
            val names = customers.associate { it.id to it.name }
            val lines = entries.map { LedgerLine(EntryKind.valueOf(it.kind), it.amount, it.voided) }
            State(
                items = entries.map { Item(it, it.customerId?.let(names::get)) },
                paymentsTotal = LedgerMath.paymentsTotal(lines),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), State())
}
