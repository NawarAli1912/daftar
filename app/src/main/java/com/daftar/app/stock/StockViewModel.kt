package com.daftar.app.stock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daftar.app.kernel.db.IntakeLineEntity
import com.daftar.app.kernel.db.ItemTypeDao
import com.daftar.app.kernel.db.ItemTypeEntity
import com.daftar.app.kernel.db.StockDao
import com.daftar.app.kernel.db.StockSourceEntity
import com.daftar.app.kernel.ledger.SourceKind
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class StockViewModel @Inject constructor(
    private val stockDao: StockDao,
    private val itemTypeDao: ItemTypeDao,
) : ViewModel() {

    data class SourceRow(
        val source: StockSourceEntity,
        val lines: List<IntakeLineEntity>,
    )

    val rows: StateFlow<List<SourceRow>> =
        combine(stockDao.observeSources(), stockDao.observeIntakeLines()) { sources, lines ->
            val bySource = lines.groupBy { it.sourceId }
            sources.map { SourceRow(it, bySource[it.id].orEmpty()) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val types: StateFlow<List<ItemTypeEntity>> =
        itemTypeDao.observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addSource(kind: SourceKind, label: String, costUsd: Long?, costLocal: Long?) {
        val trimmed = label.trim()
        if (trimmed.isEmpty()) return
        if (kind == SourceKind.BALE && (costUsd == null || costUsd <= 0)) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            stockDao.insertSource(
                StockSourceEntity(
                    id = UUID.randomUUID().toString(),
                    kind = kind.name,
                    label = trimmed,
                    costUsd = costUsd,
                    costLocal = costLocal,
                    arrivedAt = now,
                    updatedAt = now,
                )
            )
        }
    }

    fun addIntakeLine(sourceId: String, type: ItemTypeEntity, qty: Int, pricePoint: Long, unitCost: Long?) {
        if (qty <= 0 || pricePoint <= 0) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            stockDao.insertIntakeLine(
                IntakeLineEntity(
                    id = UUID.randomUUID().toString(),
                    sourceId = sourceId,
                    itemTypeId = type.id,
                    typeName = type.name,
                    qty = qty,
                    unitCost = unitCost,
                    pricePoint = pricePoint,
                    updatedAt = now,
                )
            )
        }
    }
}
