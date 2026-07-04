package com.daftar.app.payments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daftar.app.kernel.db.CustomerDao
import com.daftar.app.kernel.db.CustomerEntity
import com.daftar.app.kernel.db.LedgerDao
import com.daftar.app.kernel.db.LedgerEntryEntity
import com.daftar.app.kernel.ledger.EntryKind
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
) : ViewModel() {

    val customers: StateFlow<List<CustomerEntity>> =
        customerDao.observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    suspend fun record(amount: Long, customerId: String?): String {
        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        ledgerDao.insert(
            LedgerEntryEntity(
                id = id,
                kind = EntryKind.PAYMENT.name,
                customerId = customerId,
                amount = amount,
                happenedAt = now,
                updatedAt = now,
            )
        )
        return id
    }

    suspend fun undo(entryId: String) {
        ledgerDao.voidEntry(entryId, System.currentTimeMillis())
    }
}
