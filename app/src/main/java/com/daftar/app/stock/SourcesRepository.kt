package com.daftar.app.stock

import com.daftar.app.kernel.db.LedgerDao
import com.daftar.app.kernel.db.SaleDao
import com.daftar.app.kernel.db.StockDao
import com.daftar.app.kernel.ledger.AttributedSale
import com.daftar.app.kernel.ledger.EntryKind
import com.daftar.app.kernel.ledger.IntakeCount
import com.daftar.app.kernel.ledger.SnapshotBuilder
import com.daftar.app.kernel.ledger.SourceMeta
import com.daftar.app.kernel.ledger.SourceSnapshot
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

// Feeds the Attributor everywhere a suggestion is shown (D36/D38): snapshots are
// rebuilt live from sources + intake counts − attributed sales (typed payments = qty 1).
@Singleton
class SourcesRepository @Inject constructor(
    stockDao: StockDao,
    saleDao: SaleDao,
    ledgerDao: LedgerDao,
) {

    val snapshots: Flow<List<SourceSnapshot>> = combine(
        stockDao.observeSources(),
        stockDao.observeIntakeLines(),
        saleDao.observeAllLines(),
        ledgerDao.observeAll(),
    ) { sources, intake, saleLines, ledger ->
        val attributedFromSales = saleLines
            .filter { it.attributedSourceId != null }
            .map { AttributedSale(it.attributedSourceId!!, it.itemTypeId, it.askedUnit, it.qty, it.voided) }
        val attributedFromPayments = ledger
            .filter {
                it.kind == EntryKind.PAYMENT.name &&
                    it.attributedSourceId != null && it.itemTypeId != null && it.askedUnit != null
            }
            .map { AttributedSale(it.attributedSourceId!!, it.itemTypeId!!, it.askedUnit!!, 1, it.voided) }

        SnapshotBuilder.build(
            sources = sources.map { SourceMeta(it.id, it.arrivedAt, it.voided) },
            intake = intake.map { IntakeCount(it.sourceId, it.itemTypeId, it.pricePoint, it.qty, it.voided) },
            sold = attributedFromSales + attributedFromPayments,
        )
    }

    val names: Flow<Map<String, String>> =
        stockDao.observeSources().map { sources -> sources.associate { it.id to it.label } }

    data class PricePointChip(val typeId: String, val typeName: String, val price: Long)

    val pricePointChips: Flow<List<PricePointChip>> =
        stockDao.observeIntakeLines().map { lines ->
            lines.filterNot { it.voided }
                .map { PricePointChip(it.itemTypeId, it.typeName, it.pricePoint) }
                .distinct()
        }
}
