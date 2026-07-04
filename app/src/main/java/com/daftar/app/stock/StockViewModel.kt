package com.daftar.app.stock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daftar.app.kernel.db.IntakeLineEntity
import com.daftar.app.kernel.db.ItemTypeDao
import com.daftar.app.kernel.db.ItemTypeEntity
import com.daftar.app.kernel.db.StockDao
import com.daftar.app.kernel.db.StockSourceEntity
import com.daftar.app.kernel.ledger.SourceKind
import com.daftar.app.kernel.ledger.SourceProfit
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

    data class ProfitRow(val source: StockSourceEntity, val profit: SourceProfit)

    data class TypeAssociation(val sourceLabel: String, val price: Long, val remaining: Int)

    data class TypeRow(val type: ItemTypeEntity, val associations: List<TypeAssociation>)

    // Item-first (Beta v1): one row per stock item, tagged with its package + estimated remaining.
    data class ItemRow(
        val line: IntakeLineEntity,
        val packageId: String,
        val packageLabel: String,
        val packageKind: String,
        val remaining: Int,
    )

    // A row typed into the "new package" screen before it's saved.
    data class NewItem(val type: ItemTypeEntity, val qty: Int, val price: Long, val unitCost: Long?)

    val rows: StateFlow<List<SourceRow>> =
        combine(stockDao.observeSources(), stockDao.observeIntakeLines()) { sources, lines ->
            val bySource = lines.groupBy { it.sourceId }
            sources.map { SourceRow(it, bySource[it.id].orEmpty()) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val types: StateFlow<List<ItemTypeEntity>> =
        itemTypeDao.observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val profitRows: StateFlow<List<ProfitRow>> =
        combine(stockDao.observeSources(), sourcesRepository.profits) { sources, profits ->
            val byId = profits.associateBy { it.sourceId }
            sources.mapNotNull { source -> byId[source.id]?.let { ProfitRow(source, it) } }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val itemRows: StateFlow<List<ItemRow>> =
        combine(
            stockDao.observeIntakeLines(),
            stockDao.observeSources(),
            sourcesRepository.snapshots,
        ) { lines, sources, snapshots ->
            val byId = sources.associateBy { it.id }
            lines.map { line ->
                val source = byId[line.sourceId]
                val remaining = snapshots.firstOrNull { it.id == line.sourceId }
                    ?.points?.firstOrNull { it.typeId == line.itemTypeId && it.price == line.pricePoint }
                    ?.estimatedRemaining ?: line.qty
                ItemRow(
                    line = line,
                    packageId = line.sourceId,
                    packageLabel = source?.label ?: "",
                    packageKind = source?.kind ?: SourceKind.PRE_APP.name,
                    remaining = remaining,
                )
            }.sortedWith(compareBy({ it.packageLabel }, { it.line.typeName }))
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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

    // ── Item-first edits (Beta v1) ──

    fun updateItem(id: String, qty: Int, price: Long) {
        if (qty <= 0 || price <= 0) return
        viewModelScope.launch { stockDao.updateIntakeLine(id, qty, price, System.currentTimeMillis()) }
    }

    fun moveItem(id: String, sourceId: String) {
        viewModelScope.launch { stockDao.moveIntakeLine(id, sourceId, System.currentTimeMillis()) }
    }

    fun deleteItem(id: String) {
        viewModelScope.launch { stockDao.voidIntakeLine(id, System.currentTimeMillis()) }
    }

    // One-screen, many-rows package entry: the source and all its items saved together.
    fun createPackage(
        kind: SourceKind,
        label: String,
        costUsd: Long?,
        costLocal: Long?,
        items: List<NewItem>,
    ) {
        val trimmed = label.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val sourceId = UUID.randomUUID().toString()
            stockDao.insertSource(
                StockSourceEntity(sourceId, kind.name, trimmed, costUsd, costLocal, now, now)
            )
            items.filter { it.qty > 0 && it.price > 0 }
                .forEach { insertLine(sourceId, it.type, it.qty, it.price, it.unitCost) }
        }
    }

    fun updatePackage(id: String, label: String, costUsd: Long?, costLocal: Long?) {
        val trimmed = label.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { stockDao.updateSource(id, trimmed, costUsd, costLocal, System.currentTimeMillis()) }
    }

    fun deletePackage(id: String) {
        viewModelScope.launch { stockDao.voidSource(id, System.currentTimeMillis()) }
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
