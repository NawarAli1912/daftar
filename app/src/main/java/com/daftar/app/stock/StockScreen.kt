package com.daftar.app.stock

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.daftar.app.kernel.db.ItemTypeEntity
import com.daftar.app.kernel.i18n.Str
import com.daftar.app.kernel.ledger.SourceKind
import com.daftar.app.kernel.theme.DaftarColors
import com.daftar.app.kernel.ui.AmountField
import com.daftar.app.kernel.ui.Hairline
import com.daftar.app.kernel.ui.LedgerRow
import com.daftar.app.kernel.ui.QtyField
import com.daftar.app.kernel.ui.SectionCard
import com.daftar.app.kernel.ui.SumLine
import kotlin.math.abs
import kotlin.math.roundToInt

internal fun kindLabel(kind: String): String = when (kind) {
    SourceKind.BALE.name -> Str.bale
    SourceKind.MARKET.name -> Str.market
    else -> Str.storeClothes
}

@Composable
fun StockScreen(viewModel: StockViewModel = hiltViewModel()) {
    var section by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(start = 16.dp, top = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = section == 0,
                onClick = { section = 0 },
                label = { Text(Str.sources) },
            )
            FilterChip(
                selected = section == 1,
                onClick = { section = 1 },
                label = { Text(Str.typesTitle) },
            )
            FilterChip(
                selected = section == 2,
                onClick = { section = 2 },
                label = { Text(Str.profitTab) },
            )
            FilterChip(
                selected = section == 3,
                onClick = { section = 3 },
                label = { Text(Str.demo) },
            )
        }
        when (section) {
            0 -> SourcesSection(viewModel)
            1 -> TypesSection(viewModel)
            2 -> ProfitSection(viewModel)
            else -> DemoSection(viewModel)
        }
    }
}

@Composable
private fun DemoSection(viewModel: StockViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            "Fill the app with realistic sample data to feel the real experience, or clear everything to start fresh.",
            style = MaterialTheme.typography.bodyMedium,
            color = DaftarColors.TextSecondary,
        )
        androidx.compose.material3.Button(
            onClick = { viewModel.loadDemo() },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(Str.loadDemo) }
        androidx.compose.material3.OutlinedButton(
            onClick = { viewModel.clearAll() },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(Str.clearAll) }
    }
}

@Composable
private fun ProfitSection(viewModel: StockViewModel) {
    val rows by viewModel.profitRows.collectAsState()
    if (rows.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                Str.noProfitData,
                style = MaterialTheme.typography.bodyLarge,
                color = DaftarColors.TextSecondary,
            )
        }
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(rows, key = { it.source.id }) { row -> ProfitCard(row) }
    }
}

@Composable
private fun ProfitCard(row: StockViewModel.ProfitRow) {
    val p = row.profit
    SectionCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(row.source.label, style = MaterialTheme.typography.bodyLarge)
                Text(
                    "${kindLabel(row.source.kind)} · ${Str.count(p.ageDays)} ${Str.daysOld}",
                    style = MaterialTheme.typography.labelMedium,
                    color = DaftarColors.TextSecondary,
                )
            }
        }
        Hairline(Modifier.padding(horizontal = 16.dp))
        LedgerRow(
            title = Str.cost,
            subtitle = p.costLocal?.let { Str.money(it) },
            amountText = p.costUsd?.let { "$${Str.money(it)}" } ?: "—",
        )
        Hairline(Modifier.padding(horizontal = 16.dp))
        LedgerRow(
            title = Str.soldLabel,
            subtitle = "${Str.count(p.soldQty)} ${Str.pieces}",
            amountText = Str.money(p.soldValue),
        )
        Hairline(Modifier.padding(horizontal = 16.dp))
        LedgerRow(
            title = Str.remainingLabel,
            subtitle = null,
            amountText = Str.count(p.remainingQty),
            trailingNote = Str.pieces,
        )
        val ratio = p.recoveredRatio
        if (ratio != null) {
            SumLine(
                label = Str.recovered,
                amountText = "${Str.count((ratio * 100).roundToInt())}%",
                amountColor = if (p.paidBack) DaftarColors.Green else DaftarColors.Amber,
            )
            val profit = p.profitLocal!!
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${Str.profitLabel} · ${Str.approxNote}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DaftarColors.TextSecondary,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    (if (profit >= 0) "+" else "−") + Str.money(abs(profit)),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (profit >= 0) DaftarColors.Green else DaftarColors.Red,
                )
            }
        } else {
            Hairline(Modifier.padding(horizontal = 16.dp))
            Text(
                Str.localCostMissing,
                style = MaterialTheme.typography.labelMedium,
                color = DaftarColors.TextSecondary,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 12.dp),
            )
        }
        if (p.needsMarkdown()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Text(
                    Str.agingNudge,
                    style = MaterialTheme.typography.labelLarge,
                    color = DaftarColors.Amber,
                )
            }
        }
    }
}

@Composable
private fun SourcesSection(viewModel: StockViewModel) {
    val rows by viewModel.rows.collectAsState()
    val types by viewModel.types.collectAsState()
    val typeRows by viewModel.typeRows.collectAsState()
    var showAddSource by remember { mutableStateOf(false) }
    var showStoreClothes by remember { mutableStateOf(false) }
    var intakeFor by remember { mutableStateOf<String?>(null) }
    val existingFor: (String) -> List<StockViewModel.TypeAssociation> = { typeId ->
        typeRows.firstOrNull { it.type.id == typeId }?.associations.orEmpty()
    }

    Box(Modifier.fillMaxSize()) {
        if (rows.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    Str.noSources,
                    style = MaterialTheme.typography.bodyLarge,
                    color = DaftarColors.TextSecondary,
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 140.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(rows, key = { it.source.id }) { row ->
                    SourceCard(row, onAddIntake = { intakeFor = row.source.id })
                }
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            ExtendedFloatingActionButton(
                onClick = { showStoreClothes = true },
                icon = { Icon(Icons.Outlined.Checkroom, contentDescription = null) },
                text = { Text(Str.addStoreClothes) },
                containerColor = DaftarColors.Surface2,
                contentColor = DaftarColors.Teal,
            )
            ExtendedFloatingActionButton(
                onClick = { showAddSource = true },
                icon = { Icon(Icons.Outlined.Archive, contentDescription = null) },
                text = { Text(Str.newSource) },
            )
        }
    }

    if (showStoreClothes) {
        AddIntakeDialog(
            title = Str.addStoreTitle,
            priceLabel = Str.salePricePerUnit,
            costLabel = Str.buyPriceOptional,
            types = types,
            existingFor = existingFor,
            onSave = { type, qty, price, unitCost ->
                viewModel.addStoreClothes(type, qty, price, unitCost)
                showStoreClothes = false
            },
            onDismiss = { showStoreClothes = false },
        )
    }

    if (showAddSource) {
        AddSourceDialog(
            onSave = { kind, label, costUsd, costLocal ->
                viewModel.addSource(kind, label, costUsd, costLocal)
                showAddSource = false
            },
            onDismiss = { showAddSource = false },
        )
    }

    intakeFor?.let { sourceId ->
        AddIntakeDialog(
            title = Str.countingSession,
            priceLabel = Str.pricePoint,
            costLabel = Str.unitCostOptional,
            types = types,
            existingFor = existingFor,
            onSave = { type, qty, price, unitCost ->
                viewModel.addIntakeLine(sourceId, type, qty, price, unitCost)
                intakeFor = null
            },
            onDismiss = { intakeFor = null },
        )
    }
}

@Composable
private fun SourceCard(row: StockViewModel.SourceRow, onAddIntake: () -> Unit) {
    val lines = row.lines.filterNot { it.voided }
    val totalPieces = lines.sumOf { it.qty }
    SectionCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(row.source.label, style = MaterialTheme.typography.bodyLarge)
                val sub = buildString {
                    append(kindLabel(row.source.kind))
                    row.source.costUsd?.let { usd -> append(" · ${Str.cost} $${Str.money(usd)}") }
                }
                Text(
                    sub,
                    style = MaterialTheme.typography.labelMedium,
                    color = DaftarColors.TextSecondary,
                )
            }
            if (totalPieces > 0) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        Str.count(totalPieces),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = DaftarColors.Teal,
                    )
                    Text(
                        Str.pieces,
                        style = MaterialTheme.typography.labelMedium,
                        color = DaftarColors.TextSecondary,
                    )
                }
            }
        }
        if (lines.isEmpty()) {
            Text(
                Str.notCounted,
                style = MaterialTheme.typography.labelMedium,
                color = DaftarColors.TextSecondary,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            )
        } else {
            lines.forEach { line ->
                Hairline(Modifier.padding(horizontal = 16.dp))
                LedgerRow(
                    title = line.typeName,
                    subtitle = "@ ${Str.money(line.pricePoint)}",
                    amountText = "×${Str.count(line.qty)}",
                    amountColor = DaftarColors.TextSecondary,
                )
            }
        }
        Hairline(Modifier.padding(horizontal = 16.dp))
        Text(
            Str.addCounting,
            style = MaterialTheme.typography.labelLarge,
            color = DaftarColors.Teal,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onAddIntake)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun AddSourceDialog(
    onSave: (SourceKind, String, Long?, Long?) -> Unit,
    onDismiss: () -> Unit,
) {
    var kind by remember { mutableStateOf(SourceKind.BALE) }
    var label by remember { mutableStateOf("") }
    var usd by remember { mutableLongStateOf(0L) }
    var local by remember { mutableLongStateOf(0L) }
    val valid = label.isNotBlank() && (kind != SourceKind.BALE || usd > 0)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DaftarColors.Surface1,
        title = { Text(Str.newSource, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf(SourceKind.BALE, SourceKind.MARKET)) { entry ->
                        FilterChip(
                            selected = kind == entry,
                            onClick = { kind = entry },
                            label = { Text(kindLabel(entry.name)) },
                        )
                    }
                }
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text(Str.sourceLabel) },
                    singleLine = true,
                )
                AmountField(
                    value = usd,
                    onValue = { usd = it },
                    label = if (kind == SourceKind.BALE) Str.costUsdRequired else Str.costUsdOptional,
                    step = 50,
                )
                AmountField(
                    value = local,
                    onValue = { local = it },
                    label = Str.costLocalOptional,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(kind, label, usd.takeIf { it > 0 }, local.takeIf { it > 0 }) },
                enabled = valid,
            ) { Text(Str.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(Str.cancel) } },
    )
}

@Composable
internal fun AddIntakeDialog(
    title: String,
    priceLabel: String,
    costLabel: String,
    types: List<ItemTypeEntity>,
    existingFor: (String) -> List<StockViewModel.TypeAssociation>,
    onSave: (ItemTypeEntity, Int, Long, Long?) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedType by remember { mutableStateOf<ItemTypeEntity?>(null) }
    var qty by remember { mutableIntStateOf(0) }
    var price by remember { mutableLongStateOf(0L) }
    var cost by remember { mutableLongStateOf(0L) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DaftarColors.Surface1,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (types.isEmpty()) {
                    Text(
                        Str.noTypesHint,
                        style = MaterialTheme.typography.bodyMedium,
                        color = DaftarColors.TextSecondary,
                    )
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(types, key = { it.id }) { type ->
                        FilterChip(
                            selected = selectedType?.id == type.id,
                            onClick = {
                                selectedType = type
                                if (price == 0L) price = type.askingPrice
                            },
                            label = { Text(type.name) },
                        )
                    }
                }
                selectedType?.let { type ->
                    val existing = existingFor(type.id)
                    if (existing.isNotEmpty()) {
                        Text(
                            "${Str.inStoreNow}: " + existing.joinToString(" · ") {
                                "${it.sourceLabel} @ ${Str.money(it.price)} ×${it.remaining}"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = DaftarColors.Teal,
                        )
                    }
                }
                QtyField(value = qty, onValue = { qty = it }, label = Str.qtyApprox)
                AmountField(value = price, onValue = { price = it }, label = priceLabel)
                AmountField(value = cost, onValue = { cost = it }, label = costLabel)
            }
        },
        confirmButton = {
            TextButton(
                onClick = { selectedType?.let { onSave(it, qty, price, cost.takeIf { c -> c > 0 }) } },
                enabled = selectedType != null && qty > 0 && price > 0,
            ) { Text(Str.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(Str.cancel) } },
    )
}
