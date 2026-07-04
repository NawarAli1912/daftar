package com.daftar.app.stock

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.daftar.app.kernel.db.ItemTypeEntity
import com.daftar.app.kernel.db.StockSourceEntity
import com.daftar.app.kernel.i18n.Str
import com.daftar.app.kernel.ledger.SourceKind
import com.daftar.app.kernel.theme.DaftarColors
import com.daftar.app.kernel.ui.AmountField
import com.daftar.app.kernel.ui.Eyebrow
import com.daftar.app.kernel.ui.Hairline
import com.daftar.app.kernel.ui.LedgerRow
import com.daftar.app.kernel.ui.QtyField
import com.daftar.app.kernel.ui.SectionCard
import com.daftar.app.kernel.ui.SumLine
import com.daftar.app.reminders.RemindersWorker
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
            FilterChip(section == 0, { section = 0 }, label = { Text(Str.itemsTab) })
            FilterChip(section == 1, { section = 1 }, label = { Text(Str.packagesTab) })
            FilterChip(section == 2, { section = 2 }, label = { Text(Str.typesTitle) })
            FilterChip(section == 3, { section = 3 }, label = { Text(Str.demo) })
        }
        when (section) {
            0 -> ItemsSection(viewModel)
            1 -> PackagesSection(viewModel)
            2 -> TypesSection(viewModel)
            else -> DemoSection(viewModel)
        }
    }
}

// ─────────────────────────── Items (item-first list) ───────────────────────────

@Composable
private fun ItemsSection(viewModel: StockViewModel) {
    val items by viewModel.itemRows.collectAsState()
    val packages by viewModel.rows.collectAsState()
    val types by viewModel.types.collectAsState()
    var editing by remember { mutableStateOf<StockViewModel.ItemRow?>(null) }
    var adding by remember { mutableStateOf(false) }
    val sources = packages.map { it.source }

    Box(Modifier.fillMaxSize()) {
        if (items.isEmpty()) {
            EmptyHint(Str.noItems)
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp),
            ) {
                item {
                    SectionCard {
                        items.forEachIndexed { index, row ->
                            if (index > 0) Hairline(Modifier.padding(horizontal = 16.dp))
                            ItemLineRow(row, onClick = { editing = row })
                        }
                    }
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = { adding = true },
            icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
            text = { Text(Str.addItemFab) },
            containerColor = DaftarColors.Teal,
            contentColor = DaftarColors.OnTeal,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
        )
    }

    editing?.let { row ->
        EditItemSheet(
            row = row,
            packages = sources,
            onSave = { qty, price, packageId ->
                viewModel.updateItem(row.line.id, qty, price)
                if (packageId != row.packageId) viewModel.moveItem(row.line.id, packageId)
                editing = null
            },
            onDelete = { viewModel.deleteItem(row.line.id); editing = null },
            onDismiss = { editing = null },
        )
    }
    if (adding) {
        AddItemSheet(
            packages = sources,
            types = types,
            onSave = { sourceId, type, qty, price ->
                viewModel.addIntakeLine(sourceId, type, qty, price, null)
                adding = false
            },
            onDismiss = { adding = false },
        )
    }
}

@Composable
private fun ItemLineRow(row: StockViewModel.ItemRow, onClick: () -> Unit) {
    Column(Modifier.clickable(onClick = onClick)) {
        LedgerRow(
            title = row.line.typeName,
            subtitle = "${kindLabel(row.packageKind)} · ${row.packageLabel} · @ ${Str.money(row.line.pricePoint)}",
            amountText = Str.count(row.line.qty),
            amountColor = DaftarColors.TextPrimary,
            trailingNote = if (row.remaining != row.line.qty) {
                "${Str.remaining}${Str.count(row.remaining)}"
            } else Str.pieces,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditItemSheet(
    row: StockViewModel.ItemRow,
    packages: List<StockSourceEntity>,
    onSave: (qty: Int, price: Long, packageId: String) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var qty by remember { mutableIntStateOf(row.line.qty) }
    var price by remember { mutableLongStateOf(row.line.pricePoint) }
    var packageId by remember { mutableStateOf(row.packageId) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = DaftarColors.Surface1) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("${Str.editItem} — ${row.line.typeName}", style = MaterialTheme.typography.titleLarge)
            QtyField(value = qty, onValue = { qty = it }, label = Str.qtyApprox)
            AmountField(value = price, onValue = { price = it }, label = Str.pricePoint)
            Text(Str.movePackage, style = MaterialTheme.typography.labelMedium, color = DaftarColors.TextSecondary)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(packages, key = { it.id }) { source ->
                    FilterChip(
                        selected = packageId == source.id,
                        onClick = { packageId = source.id },
                        label = { Text(source.label) },
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onDelete) {
                    Icon(Icons.Outlined.Delete, contentDescription = null, tint = DaftarColors.Red)
                    Text("  ${Str.delete}", color = DaftarColors.Red)
                }
                Spacer(Modifier.weight(1f))
                Button(onClick = { onSave(qty, price, packageId) }, enabled = qty > 0 && price > 0) {
                    Text(Str.save)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddItemSheet(
    packages: List<StockSourceEntity>,
    types: List<ItemTypeEntity>,
    onSave: (sourceId: String, type: ItemTypeEntity, qty: Int, price: Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var packageId by remember { mutableStateOf(packages.firstOrNull()?.id) }
    var type by remember { mutableStateOf<ItemTypeEntity?>(null) }
    var qty by remember { mutableIntStateOf(0) }
    var price by remember { mutableLongStateOf(0L) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = DaftarColors.Surface1) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(Str.addItemTitle, style = MaterialTheme.typography.titleLarge)
            if (packages.isEmpty()) {
                Text(Str.noPackages, style = MaterialTheme.typography.bodyMedium, color = DaftarColors.TextSecondary)
            }
            Text(Str.movePackage, style = MaterialTheme.typography.labelMedium, color = DaftarColors.TextSecondary)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(packages, key = { it.id }) { source ->
                    FilterChip(packageId == source.id, { packageId = source.id }, label = { Text(source.label) })
                }
            }
            TypePicker(types, type) { picked ->
                type = picked
                if (price == 0L) price = picked.askingPrice
            }
            QtyField(value = qty, onValue = { qty = it }, label = Str.qtyApprox)
            AmountField(value = price, onValue = { price = it }, label = Str.pricePoint)
            Button(
                onClick = { onSave(packageId!!, type!!, qty, price) },
                enabled = packageId != null && type != null && qty > 0 && price > 0,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(Str.save) }
        }
    }
}

// ─────────────────────────── Packages (thin trackers + profit) ───────────────────────────

@Composable
private fun PackagesSection(viewModel: StockViewModel) {
    val rows by viewModel.profitRows.collectAsState()
    val types by viewModel.types.collectAsState()
    var showNew by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<StockSourceEntity?>(null) }

    if (showNew) {
        NewPackageScreen(
            types = types,
            onSave = { kind, label, usd, local, items ->
                viewModel.createPackage(kind, label, usd, local, items)
                showNew = false
            },
            onClose = { showNew = false },
        )
        return
    }

    Box(Modifier.fillMaxSize()) {
        if (rows.isEmpty()) {
            EmptyHint(Str.noPackages)
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(rows, key = { it.source.id }) { row ->
                    ProfitCard(row, onClick = { editing = row.source })
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = { showNew = true },
            icon = { Icon(Icons.Outlined.Archive, contentDescription = null) },
            text = { Text(Str.newPackage) },
            containerColor = DaftarColors.Teal,
            contentColor = DaftarColors.OnTeal,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
        )
    }

    editing?.let { source ->
        EditPackageSheet(
            source = source,
            onSave = { label, usd, local ->
                viewModel.updatePackage(source.id, label, usd, local)
                editing = null
            },
            onDelete = { viewModel.deletePackage(source.id); editing = null },
            onDismiss = { editing = null },
        )
    }
}

@Composable
private fun NewPackageScreen(
    types: List<ItemTypeEntity>,
    onSave: (SourceKind, String, Long?, Long?, List<StockViewModel.NewItem>) -> Unit,
    onClose: () -> Unit,
) {
    BackHandler { onClose() }
    var kind by remember { mutableStateOf(SourceKind.BALE) }
    var label by remember { mutableStateOf("") }
    var usd by remember { mutableLongStateOf(0L) }
    var local by remember { mutableLongStateOf(0L) }
    val items = remember { mutableStateListOf<StockViewModel.NewItem>() }
    var rowType by remember { mutableStateOf<ItemTypeEntity?>(null) }
    var rowQty by remember { mutableIntStateOf(0) }
    var rowPrice by remember { mutableLongStateOf(0L) }

    val costValid = kind != SourceKind.BALE || usd > 0
    val valid = label.isNotBlank() && costValid && items.isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DaftarColors.Surface0)
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text(Str.newPackage, style = MaterialTheme.typography.headlineSmall)
            IconButton(onClick = onClose) { Icon(Icons.Outlined.Close, contentDescription = Str.close) }
        }

        Text(Str.packageKind, style = MaterialTheme.typography.labelMedium, color = DaftarColors.TextSecondary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(SourceKind.BALE, SourceKind.MARKET, SourceKind.PRE_APP).forEach { k ->
                FilterChip(kind == k, { kind = k }, label = { Text(kindLabel(k.name)) })
            }
        }

        OutlinedTextField(
            value = label,
            onValueChange = { label = it },
            label = { Text(Str.sourceLabel) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        if (kind == SourceKind.BALE || kind == SourceKind.MARKET) {
            AmountField(
                value = usd,
                onValue = { usd = it },
                label = if (kind == SourceKind.BALE) Str.costUsdRequired else Str.costUsdOptional,
                step = 50,
            )
            AmountField(value = local, onValue = { local = it }, label = Str.costLocalOptional)
        }

        Eyebrow(Str.packageItems)
        SectionCard {
            if (items.isEmpty()) {
                Text(
                    Str.notCounted,
                    style = MaterialTheme.typography.labelMedium,
                    color = DaftarColors.TextSecondary,
                    modifier = Modifier.padding(16.dp),
                )
            } else {
                items.forEachIndexed { index, it ->
                    if (index > 0) Hairline(Modifier.padding(horizontal = 16.dp))
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                        Arrangement.SpaceBetween,
                        Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(it.type.name, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "×${Str.count(it.qty)} · @ ${Str.money(it.price)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = DaftarColors.TextSecondary,
                            )
                        }
                        IconButton(onClick = { items.removeAt(index) }) {
                            Icon(Icons.Outlined.Delete, contentDescription = Str.delete, tint = DaftarColors.TextSecondary)
                        }
                    }
                }
            }
        }

        // Add-a-row sub-form
        TypePicker(types, rowType) { picked ->
            rowType = picked
            if (rowPrice == 0L) rowPrice = picked.askingPrice
        }
        QtyField(value = rowQty, onValue = { rowQty = it }, label = Str.qtyApprox)
        AmountField(value = rowPrice, onValue = { rowPrice = it }, label = Str.pricePoint)
        TextButton(
            onClick = {
                rowType?.let { t ->
                    items.add(StockViewModel.NewItem(t, rowQty, rowPrice, null))
                    rowType = null; rowQty = 0; rowPrice = 0L
                }
            },
            enabled = rowType != null && rowQty > 0 && rowPrice > 0,
        ) { Text("+ ${Str.addRow}") }

        Button(
            onClick = {
                onSave(kind, label, usd.takeIf { it > 0 }, local.takeIf { it > 0 }, items.toList())
            },
            enabled = valid,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(Str.savePackage) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditPackageSheet(
    source: StockSourceEntity,
    onSave: (label: String, usd: Long?, local: Long?) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var label by remember { mutableStateOf(source.label) }
    var usd by remember { mutableLongStateOf(source.costUsd ?: 0L) }
    var local by remember { mutableLongStateOf(source.costLocal ?: 0L) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = DaftarColors.Surface1) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("${Str.editPackage} — ${kindLabel(source.kind)}", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text(Str.sourceLabel) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            AmountField(value = usd, onValue = { usd = it }, label = Str.costUsdOptional, step = 50)
            AmountField(value = local, onValue = { local = it }, label = Str.costLocalOptional)
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onDelete) {
                    Icon(Icons.Outlined.Delete, contentDescription = null, tint = DaftarColors.Red)
                    Text("  ${Str.delete}", color = DaftarColors.Red)
                }
                Spacer(Modifier.weight(1f))
                Button(onClick = { onSave(label, usd.takeIf { it > 0 }, local.takeIf { it > 0 }) }, enabled = label.isNotBlank()) {
                    Text(Str.save)
                }
            }
        }
    }
}

@Composable
private fun ProfitCard(row: StockViewModel.ProfitRow, onClick: () -> Unit) {
    val p = row.profit
    SectionCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
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
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
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
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                Text(Str.agingNudge, style = MaterialTheme.typography.labelLarge, color = DaftarColors.Amber)
            }
        }
    }
}

// ─────────────────────────── shared bits ───────────────────────────

@Composable
private fun TypePicker(
    types: List<ItemTypeEntity>,
    selected: ItemTypeEntity?,
    onPick: (ItemTypeEntity) -> Unit,
) {
    if (types.isEmpty()) {
        Text(Str.noTypesHint, style = MaterialTheme.typography.bodyMedium, color = DaftarColors.TextSecondary)
        return
    }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(types, key = { it.id }) { type ->
            FilterChip(selected?.id == type.id, { onPick(type) }, label = { Text(type.name) })
        }
    }
}

@Composable
private fun EmptyHint(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.bodyLarge, color = DaftarColors.TextSecondary)
    }
}

@Composable
private fun DemoSection(viewModel: StockViewModel) {
    val context = LocalContext.current
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
        Button(onClick = { viewModel.loadDemo() }, modifier = Modifier.fillMaxWidth()) { Text(Str.loadDemo) }
        androidx.compose.material3.OutlinedButton(
            onClick = { viewModel.clearAll() },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(Str.clearAll) }
        androidx.compose.material3.OutlinedButton(
            onClick = { RemindersWorker.runNow(context) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(Str.testReminderAlert) }
    }
}
