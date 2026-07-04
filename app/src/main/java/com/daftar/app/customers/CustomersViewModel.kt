package com.daftar.app.customers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daftar.app.kernel.db.CustomerDao
import com.daftar.app.kernel.db.CustomerEntity
import com.daftar.app.kernel.db.LedgerDao
import com.daftar.app.kernel.ledger.EntryKind
import com.daftar.app.kernel.ledger.LedgerLine
import com.daftar.app.kernel.ledger.LedgerMath
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class CustomersViewModel @Inject constructor(
    private val customerDao: CustomerDao,
    ledgerDao: LedgerDao,
) : ViewModel() {

    data class Row(val customer: CustomerEntity, val balance: Long)

    val rows: StateFlow<List<Row>> =
        combine(customerDao.observeAll(), ledgerDao.observeAll()) { customers, entries ->
            val byCustomer = entries.groupBy { it.customerId }
            customers.map { customer ->
                val lines = byCustomer[customer.id].orEmpty().map {
                    LedgerLine(EntryKind.valueOf(it.kind), it.amount, it.voided)
                }
                Row(customer, LedgerMath.balance(lines))
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addCustomer(name: String, phone: String?) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            customerDao.insert(
                CustomerEntity(
                    id = UUID.randomUUID().toString(),
                    name = trimmed,
                    phone = phone?.trim()?.takeIf { it.isNotEmpty() },
                    createdAt = now,
                    updatedAt = now,
                )
            )
        }
    }
}
