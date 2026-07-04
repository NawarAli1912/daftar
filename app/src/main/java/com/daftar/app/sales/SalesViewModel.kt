package com.daftar.app.sales

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daftar.app.kernel.db.CustomerDao
import com.daftar.app.kernel.db.CustomerEntity
import com.daftar.app.kernel.db.ItemTypeDao
import com.daftar.app.kernel.db.ItemTypeEntity
import com.daftar.app.kernel.db.LedgerDao
import com.daftar.app.kernel.db.LedgerEntryEntity
import com.daftar.app.kernel.db.SaleDao
import com.daftar.app.kernel.db.SaleEntity
import com.daftar.app.kernel.db.SaleLineEntity
import com.daftar.app.kernel.ledger.Attribution
import com.daftar.app.kernel.ledger.Attributor
import com.daftar.app.kernel.ledger.EntryKind
import com.daftar.app.kernel.ledger.LedgerMath
import com.daftar.app.kernel.ledger.SourceSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DraftLine(
    val typeId: String,
    val typeName: String,
    val qty: Int,
    val askedUnit: Long,
    val agreedUnit: Long,
)

@HiltViewModel
class SalesViewModel @Inject constructor(
    private val saleDao: SaleDao,
    private val ledgerDao: LedgerDao,
    private val itemTypeDao: ItemTypeDao,
    customerDao: CustomerDao,
) : ViewModel() {

    val types: StateFlow<List<ItemTypeEntity>> =
        itemTypeDao.observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val customers: StateFlow<List<CustomerEntity>> =
        customerDao.observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Interim (D34): no stock sources exist until the stock slice is built; the live
    // suggestion honestly says غير محدد. The stock slice replaces this with real sources.
    private val sources = MutableStateFlow<List<SourceSnapshot>>(emptyList())
    private val sourceNames = MutableStateFlow<Map<String, String>>(emptyMap())

    fun suggestionLabel(typeId: String, askedUnit: Long): String =
        when (val result = Attributor.attribute(typeId, askedUnit, sources.value)) {
            is Attribution.ToSource -> sourceNames.value[result.sourceId] ?: "غير محدد"
            Attribution.Unmatched -> "غير محدد"
        }

    private fun attributedSourceId(typeId: String, askedUnit: Long): String? =
        when (val result = Attributor.attribute(typeId, askedUnit, sources.value)) {
            is Attribution.ToSource -> result.sourceId
            Attribution.Unmatched -> null
        }

    fun addType(name: String, askingPrice: Long) {
        val trimmed = name.trim()
        if (trimmed.isEmpty() || askingPrice <= 0) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            runCatching {
                itemTypeDao.insert(
                    ItemTypeEntity(
                        id = UUID.randomUUID().toString(),
                        name = trimmed,
                        askingPrice = askingPrice,
                        createdAt = now,
                        updatedAt = now,
                    )
                )
            }
        }
    }

    suspend fun record(lines: List<DraftLine>, customerId: String?, paidNow: Long): String {
        val now = System.currentTimeMillis()
        val saleId = UUID.randomUUID().toString()
        saleDao.insertSale(SaleEntity(saleId, customerId, now, now))
        saleDao.insertLines(
            lines.map { line ->
                SaleLineEntity(
                    id = UUID.randomUUID().toString(),
                    saleId = saleId,
                    itemTypeId = line.typeId,
                    typeName = line.typeName,
                    qty = line.qty,
                    askedUnit = line.askedUnit,
                    agreedUnit = line.agreedUnit,
                    updatedAt = now,
                    attributedSourceId = attributedSourceId(line.typeId, line.askedUnit),
                )
            }
        )
        val total = lines.sumOf { LedgerMath.lineTotal(it.qty, it.agreedUnit) }
        if (customerId != null && total > 0) {
            ledgerDao.insert(
                LedgerEntryEntity(
                    id = UUID.randomUUID().toString(),
                    kind = EntryKind.SALE.name,
                    customerId = customerId,
                    amount = total,
                    happenedAt = now,
                    updatedAt = now,
                    saleId = saleId,
                )
            )
        }
        if (paidNow > 0) {
            ledgerDao.insert(
                LedgerEntryEntity(
                    id = UUID.randomUUID().toString(),
                    kind = EntryKind.PAYMENT.name,
                    customerId = customerId,
                    amount = paidNow,
                    happenedAt = now,
                    updatedAt = now,
                    saleId = saleId,
                )
            )
        }
        return saleId
    }

    suspend fun undo(saleId: String) {
        saleDao.voidSaleCascade(saleId, System.currentTimeMillis())
    }
}
