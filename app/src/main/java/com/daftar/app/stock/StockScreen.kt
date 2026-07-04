package com.daftar.app.stock

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.daftar.app.kernel.db.ItemTypeEntity
import com.daftar.app.kernel.format.ArabicNumbers
import com.daftar.app.kernel.ledger.SourceKind
import com.daftar.app.kernel.theme.DaftarColors

private fun kindLabel(kind: String): String = when (kind) {
    SourceKind.BALE.name -> "بالة"
    SourceKind.MARKET.name -> "شراء من السوق"
    else -> "بضاعة المحل"
}

@Composable
fun StockScreen(viewModel: StockViewModel = hiltViewModel()) {
    var section by remember { mutableStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = section == 0,
                onClick = { section = 0 },
                label = { Text("المصادر") },
            )
            FilterChip(
                selected = section == 1,
                onClick = { section = 1 },
                label = { Text("الأصناف") },
            )
        }
        when (section) {
            0 -> SourcesSection(viewModel)
            else -> TypesSection(viewModel)
        }
    }
}

@Composable
private fun SourcesSection(viewModel: StockViewModel) {
    val rows by viewModel.rows.collectAsState()
    val types by viewModel.types.collectAsState()
    var showAddSource by remember { mutableStateOf(false) }
    var showStoreClothes by remember { mutableStateOf(false) }
    var intakeFor by remember { mutableStateOf<String?>(null) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            if (rows.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "لا مصادر بعد — سجّلي بالة أو بضاعة المحل الحالية",
                        style = MaterialTheme.typography.bodyLarge,
                        color = DaftarColors.TextSecondary,
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(rows, key = { it.source.id }) { row ->
                        SourceCard(row, onAddIntake = { intakeFor = row.source.id })
                    }
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
                text = { Text("+ بضاعة المحل") },
                containerColor = DaftarColors.Surface2,
                contentColor = DaftarColors.Teal,
            )
            ExtendedFloatingActionButton(
                onClick = { showAddSource = true },
                icon = { Icon(Icons.Outlined.Archive, contentDescription = null) },
                text = { Text("مصدر جديد") },
            )
        }
    }

    if (showStoreClothes) {
        AddIntakeDialog(
            title = "بضاعة المحل — إضافة",
            priceLabel = "سعر البيع للقطعة",
            costLabel = "سعر الشراء للقطعة (اختياري)",
            types = types,
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
            title = "جلسة عدّ — سطر جديد",
            priceLabel = "نقطة السعر",
            costLabel = "كلفة القطعة (اختياري)",
            types = types,
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
    Card(colors = CardDefaults.cardColors(containerColor = DaftarColors.Surface1)) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(row.source.label, style = MaterialTheme.typography.titleMedium)
                Text(
                    kindLabel(row.source.kind),
                    style = MaterialTheme.typography.labelMedium,
                    color = DaftarColors.Teal,
                )
            }
            row.source.costUsd?.let { usd ->
                Text(
                    "التكلفة: $${ArabicNumbers.format(usd)}" +
                        (row.source.costLocal?.let { " (${ArabicNumbers.format(it)})" } ?: ""),
                    style = MaterialTheme.typography.labelMedium,
                    color = DaftarColors.TextSecondary,
                )
            }
            if (row.lines.isEmpty()) {
                Text(
                    "لم تُعدّ بعد — العدّ اختياري ولا يوقف البيع",
                    style = MaterialTheme.typography.labelMedium,
                    color = DaftarColors.TextSecondary,
                )
            } else {
                row.lines.filterNot { it.voided }.forEach { line ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            "${line.typeName} @ ${ArabicNumbers.format(line.pricePoint)}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            "×${ArabicNumbers.format(line.qty.toLong())}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = DaftarColors.TextSecondary,
                        )
                    }
                }
            }
            Text(
                "+ جلسة عدّ",
                style = MaterialTheme.typography.labelLarge,
                color = DaftarColors.Teal,
                modifier = Modifier
                    .padding(top = 6.dp)
                    .clickable(onClick = onAddIntake),
            )
        }
    }
}

@Composable
private fun AddSourceDialog(
    onSave: (SourceKind, String, Long?, Long?) -> Unit,
    onDismiss: () -> Unit,
) {
    var kind by remember { mutableStateOf(SourceKind.BALE) }
    var label by remember { mutableStateOf("") }
    var usdText by remember { mutableStateOf("") }
    var localText by remember { mutableStateOf("") }
    val usd = ArabicNumbers.parseAmount(usdText).takeIf { it > 0 }
    val local = ArabicNumbers.parseAmount(localText).takeIf { it > 0 }
    val valid = label.isNotBlank() && (kind != SourceKind.BALE || usd != null)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DaftarColors.Surface1,
        title = { Text("مصدر جديد", style = MaterialTheme.typography.titleLarge) },
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
                    label = { Text("الاسم (بالة شباط…)") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = usdText,
                    onValueChange = { new -> usdText = new.filter { it.isDigit() } },
                    label = { Text(if (kind == SourceKind.BALE) "التكلفة بالدولار (إلزامي)" else "التكلفة بالدولار (اختياري)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr),
                )
                OutlinedTextField(
                    value = localText,
                    onValueChange = { new -> localText = new.filter { it.isDigit() } },
                    label = { Text("المعادل المحلي (اختياري)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(kind, label, usd, local) }, enabled = valid) {
                Text("حفظ")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
    )
}

@Composable
private fun AddIntakeDialog(
    title: String,
    priceLabel: String,
    costLabel: String,
    types: List<ItemTypeEntity>,
    onSave: (ItemTypeEntity, Int, Long, Long?) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedType by remember { mutableStateOf<ItemTypeEntity?>(null) }
    var qtyText by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var costText by remember { mutableStateOf("") }
    val qty = ArabicNumbers.parseAmount(qtyText).toInt()
    val price = ArabicNumbers.parseAmount(priceText)
    val unitCost = ArabicNumbers.parseAmount(costText).takeIf { it > 0 }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DaftarColors.Surface1,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (types.isEmpty()) {
                    Text(
                        "لا أصناف بعد — أضيفي صنفاً من شاشة البيع (+ صنف)",
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
                                if (priceText.isBlank()) priceText = type.askingPrice.toString()
                            },
                            label = { Text(type.name) },
                        )
                    }
                }
                OutlinedTextField(
                    value = qtyText,
                    onValueChange = { new -> qtyText = new.filter { it.isDigit() } },
                    label = { Text("الكمية (تقريباً)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr),
                )
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { new -> priceText = new.filter { it.isDigit() } },
                    label = { Text(priceLabel) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr),
                )
                OutlinedTextField(
                    value = costText,
                    onValueChange = { new -> costText = new.filter { it.isDigit() } },
                    label = { Text(costLabel) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { selectedType?.let { onSave(it, qty, price, unitCost) } },
                enabled = selectedType != null && qty > 0 && price > 0,
            ) { Text("حفظ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
    )
}
