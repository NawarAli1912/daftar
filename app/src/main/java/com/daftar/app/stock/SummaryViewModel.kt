package com.daftar.app.stock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daftar.app.kernel.db.CustomerDao
import com.daftar.app.kernel.db.LedgerDao
import com.daftar.app.kernel.ledger.EntryKind
import com.daftar.app.kernel.ledger.LedgerLine
import com.daftar.app.kernel.ledger.LedgerMath
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

// الملخص — the owner summary (FR-7.1): what's owed, what sold, what it made.
// A read model derived from the same ledgers/sources the other screens use.
@HiltViewModel
class SummaryViewModel @Inject constructor(
    customerDao: CustomerDao,
    ledgerDao: LedgerDao,
    sourcesRepository: SourcesRepository,
) : ViewModel() {

    data class Summary(
        val outstandingDebt: Long = 0, // Σ positive customer balances (owed to the shop)
        val soldValue: Long = 0,       // Σ attributed sold value across all sources
        val profitSoFar: Long = 0,     // Σ profitLocal where the local cost is known
        val profitKnown: Boolean = false,
        val agingCount: Int = 0,       // sources that could use a markdown
        val packageCount: Int = 0,
    )

    val summary: StateFlow<Summary> =
        combine(
            customerDao.observeAll(),
            ledgerDao.observeAll(),
            sourcesRepository.profits,
        ) { customers, entries, profits ->
            val byCustomer = entries.groupBy { it.customerId }
            val outstanding = customers.sumOf { customer ->
                val balance = LedgerMath.balance(
                    byCustomer[customer.id].orEmpty()
                        .map { LedgerLine(EntryKind.valueOf(it.kind), it.amount, it.voided) }
                )
                if (balance > 0) balance else 0L
            }
            Summary(
                outstandingDebt = outstanding,
                soldValue = profits.sumOf { it.soldValue },
                profitSoFar = profits.mapNotNull { it.profitLocal }.sum(),
                profitKnown = profits.any { it.profitLocal != null },
                agingCount = profits.count { it.needsMarkdown() },
                packageCount = profits.size,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Summary())
}
