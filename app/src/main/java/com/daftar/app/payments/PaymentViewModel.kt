package com.daftar.app.payments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daftar.app.kernel.db.CustomerDao
import com.daftar.app.kernel.db.CustomerEntity
import com.daftar.app.kernel.db.ItemTypeDao
import com.daftar.app.kernel.db.ItemTypeEntity
import com.daftar.app.kernel.db.LedgerDao
import com.daftar.app.kernel.db.LedgerEntryEntity
import com.daftar.app.kernel.ledger.Attribution
import com.daftar.app.kernel.ledger.Attributor
import com.daftar.app.kernel.ledger.EntryKind
import com.daftar.app.kernel.ledger.SourceSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class PaymentViewModel @Inject constructor(
    private val ledgerDao: LedgerDao,
    customerDao: CustomerDao,
    itemTypeDao: ItemTypeDao,
    sourcesRepository: com.daftar.app.stock.SourcesRepository,
) : ViewModel() {

    val customers: StateFlow<List<CustomerEntity>> =
        customerDao.observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val types: StateFlow<List<ItemTypeEntity>> =
        itemTypeDao.observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val sources: StateFlow<List<SourceSnapshot>> =
        sourcesRepository.snapshots
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val sourceNames: StateFlow<Map<String, String>> =
        sourcesRepository.names
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun suggestionLabel(typeId: String, askedUnit: Long): String =
        when (val result = Attributor.attribute(typeId, askedUnit, sources.value)) {
            is Attribution.ToSource -> sourceNames.value[result.sourceId] ?: "غير محدد"
            Attribution.Unmatched -> "غير محدد"
        }

    suspend fun record(
        amount: Long,
        customerId: String?,
        itemTypeId: String? = null,
        askedUnit: Long? = null,
    ): String {
        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        val attributed = if (itemTypeId != null && askedUnit != null) {
            when (val result = Attributor.attribute(itemTypeId, askedUnit, sources.value)) {
                is Attribution.ToSource -> result.sourceId
                Attribution.Unmatched -> null
            }
        } else {
            null
        }
        ledgerDao.insert(
            LedgerEntryEntity(
                id = id,
                kind = EntryKind.PAYMENT.name,
                customerId = customerId,
                amount = amount,
                happenedAt = now,
                updatedAt = now,
                itemTypeId = itemTypeId,
                askedUnit = askedUnit,
                attributedSourceId = attributed,
            )
        )
        return id
    }

    suspend fun undo(entryId: String) {
        ledgerDao.voidEntry(entryId, System.currentTimeMillis())
    }
}
