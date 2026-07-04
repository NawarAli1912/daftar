package com.daftar.app.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daftar.app.kernel.db.CustomerDao
import com.daftar.app.kernel.db.LedgerDao
import com.daftar.app.kernel.db.LedgerEntryEntity
import com.daftar.app.kernel.db.SaleDao
import com.daftar.app.kernel.ledger.EntryKind
import com.daftar.app.kernel.ledger.LedgerLine
import com.daftar.app.kernel.ledger.LedgerMath
import com.daftar.app.kernel.ledger.SaleLineAmounts
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

sealed interface DayCard {
    val happenedAt: Long
    val key: String

    data class Ledger(
        val entry: LedgerEntryEntity,
        val customerName: String?,
    ) : DayCard {
        override val happenedAt: Long get() = entry.happenedAt
        override val key: String get() = entry.id
    }

    data class Sale(
        val saleId: String,
        val customerName: String?,
        val linesSummary: String,
        val total: Long,
        val paidNow: Long,
        override val happenedAt: Long,
    ) : DayCard {
        override val key: String get() = saleId
    }
}

@HiltViewModel
class TodayViewModel @Inject constructor(
    ledgerDao: LedgerDao,
    saleDao: SaleDao,
    customerDao: CustomerDao,
) : ViewModel() {

    data class State(
        val cards: List<DayCard> = emptyList(),
        val paymentsTotal: Long = 0L,
    )

    private val zone: ZoneId = ZoneId.systemDefault()

    // The day-book is a page you can flip: the selected date drives which day is shown.
    private val _selectedDate = MutableStateFlow(LocalDate.now(zone))
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    fun previousDay() {
        _selectedDate.value = _selectedDate.value.minusDays(1)
    }

    fun nextDay() {
        val next = _selectedDate.value.plusDays(1)
        if (!next.isAfter(LocalDate.now(zone))) _selectedDate.value = next // never past today
    }

    fun goToToday() {
        _selectedDate.value = LocalDate.now(zone)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<State> =
        _selectedDate.flatMapLatest { date ->
            val dayStart = date.atStartOfDay(zone).toInstant().toEpochMilli()
            val dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            combine(
                ledgerDao.observeDay(dayStart, dayEnd),
                saleDao.observeDay(dayStart, dayEnd),
                customerDao.observeAll(),
            ) { ledger, sales, customers ->
            val names = customers.associate { it.id to it.name }
            val paymentsTotal = LedgerMath.paymentsTotal(
                ledger.map { LedgerLine(EntryKind.valueOf(it.kind), it.amount, it.voided) }
            )
            val paidBySale = ledger
                .filter { it.saleId != null && it.kind == EntryKind.PAYMENT.name }
                .groupBy { it.saleId!! }
                .mapValues { (_, rows) -> rows.sumOf { it.amount } }

            val ledgerCards = ledger
                .filter { it.saleId == null }
                .map { DayCard.Ledger(it, it.customerId?.let(names::get)) }

            val saleCards = sales.map { saleWithLines ->
                val visibleLines = saleWithLines.lines.filterNot { it.voided }
                DayCard.Sale(
                    saleId = saleWithLines.sale.id,
                    customerName = saleWithLines.sale.customerId?.let(names::get),
                    linesSummary = visibleLines.joinToString(" · ") {
                        "${it.typeName} ×${it.qty}"
                    },
                    total = LedgerMath.saleTotal(
                        saleWithLines.lines.map { SaleLineAmounts(it.qty, it.agreedUnit, it.voided) }
                    ),
                    paidNow = paidBySale[saleWithLines.sale.id] ?: 0L,
                    happenedAt = saleWithLines.sale.happenedAt,
                )
            }

                State(
                    cards = (ledgerCards + saleCards).sortedByDescending { it.happenedAt },
                    paymentsTotal = paymentsTotal,
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), State())
}
