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
    private val demoSeeder: com.daftar.app.demo.DemoSeeder,
    sourcesRepository: SourcesRepository,
) : ViewModel() {

    fun loadDemo() = viewModelScope.launch { demoSeeder.load() }
    fun clearAll() = viewModelScope.launch { demoSeeder.clear() }

    data class SourceRow(
        val source: StockSourceEntity,
        val lines: List<IntakeLineEntity>,
    )

    data class TypeAssociation(val sourceLabel: String, val price: Long, val remaining: Int)

    data class TypeRow(val type: ItemTypeEntity, val associations: List<TypeAssociation>)

    val rows: StateFlow<List<SourceRow>> =
        combine(stockDao.observeSources(), stockDao.observeIntakeLines()) { sources, lines ->
            val bySource = lines.groupBy { it.sourceId }
            sources.map { SourceRow(it, bySource[it.id].orEmpty()) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val types: StateFlow<List<ItemTypeEntity>> =
        itemTypeDao.observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val typeRows: StateFlow<List<TypeRow>> =
        combine(
            itemTypeDao.observeAll(),
            stockDao.observeSources(),
            sourcesRepository.snapshots,
        ) { allTypes, sources, snapshots ->
            val labels = sources.associate { it.id to it.label }
            allTypes.map { type ->
                val associations = snapshots.flatMap { snapshot ->
                    snapshot.points
                        .filter { it.typeId == type.id }
                        .map { point ->
                            TypeAssociation(
                                sourceLabel = labels[snapshot.id] ?: "غير محدد",
                                price = point.price,
                                remaining = point.estimatedRemaining,
                            )
                        }
                }
                TypeRow(type, associations)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun updateType(id: String, name: String, price: Long) {
        val trimmed = name.trim()
        if (trimmed.isEmpty() || price <= 0) return
        viewModelScope.launch {
            itemTypeDao.update(id, trimmed, price, System.currentTimeMillis())
        }
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
        viewModelScope.launch { insertLine(sourceId, type, qty, pricePoint, unitCost) }
    }

    // D39: store clothes go into a singleton implicit source — one step, no ceremony
    fun addStoreClothes(type: ItemTypeEntity, qty: Int, salePricePerUnit: Long, unitBuyPrice: Long?) {
        if (qty <= 0 || salePricePerUnit <= 0) return
        viewModelScope.launch {
            val existing = stockDao.findByKind(SourceKind.PRE_APP.name)
            val sourceId = existing?.id ?: UUID.randomUUID().toString().also { id ->
                val now = System.currentTimeMillis()
                stockDao.insertSource(
                    StockSourceEntity(
                        id = id,
                        kind = SourceKind.PRE_APP.name,
                        label = "بضاعة المحل",
                        costUsd = null,
                        costLocal = null,
                        arrivedAt = now,
                        updatedAt = now,
                    )
                )
            }
            insertLine(sourceId, type, qty, salePricePerUnit, unitBuyPrice)
        }
    }

    private suspend fun insertLine(
        sourceId: String,
        type: ItemTypeEntity,
        qty: Int,
        pricePoint: Long,
        unitCost: Long?,
    ) {
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
