package com.daftar.app.stock

import com.daftar.app.kernel.db.LedgerDao
import com.daftar.app.kernel.db.SaleDao
import com.daftar.app.kernel.db.StockDao
import com.daftar.app.kernel.ledger.AttributedSale
import com.daftar.app.kernel.ledger.EntryKind
import com.daftar.app.kernel.ledger.IntakeCount
import com.daftar.app.kernel.ledger.IntakeQty
import com.daftar.app.kernel.ledger.ProfitMath
import com.daftar.app.kernel.ledger.RevenueLine
import com.daftar.app.kernel.ledger.SnapshotBuilder
import com.daftar.app.kernel.ledger.SourceCostInput
import com.daftar.app.kernel.ledger.SourceMeta
import com.daftar.app.kernel.ledger.SourceProfit
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

    // Stock v2 (D10/D23): "did this source pay for itself?" — cost vs the agreed value
    // of goods attributed out of it. Revenue = sold sale-lines (agreedUnit × qty) plus
    // typed payments attributed to a source (the amount received). Both sets are disjoint:
    // basket-sale payments carry no type/price, so nothing is double-counted.
    val profits: Flow<List<SourceProfit>> = combine(
        stockDao.observeSources(),
        stockDao.observeIntakeLines(),
        saleDao.observeAllLines(),
        ledgerDao.observeAll(),
    ) { sources, intake, saleLines, ledger ->
        val revenueFromSales = saleLines
            .filter { it.attributedSourceId != null }
            .map { RevenueLine(it.attributedSourceId!!, it.agreedUnit * it.qty, it.qty, it.voided) }
        val revenueFromPayments = ledger
            .filter {
                it.kind == EntryKind.PAYMENT.name &&
                    it.attributedSourceId != null && it.itemTypeId != null && it.askedUnit != null
            }
            .map { RevenueLine(it.attributedSourceId!!, it.amount, 1, it.voided) }

        ProfitMath.build(
            sources = sources.map { SourceCostInput(it.id, it.costUsd, it.costLocal, it.arrivedAt, it.voided) },
            intake = intake.map { IntakeQty(it.sourceId, it.qty, it.voided) },
            revenue = revenueFromSales + revenueFromPayments,
            now = System.currentTimeMillis(),
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
