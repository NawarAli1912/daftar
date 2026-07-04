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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
    private val dayStart: Long =
        LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
    private val dayEnd: Long =
        LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

    val state: StateFlow<State> =
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
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), State())
}
