package com.daftar.app.stock

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
import androidx.compose.material.icons.outlined.Add
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
import com.daftar.app.kernel.db.ItemTypeEntity
import com.daftar.app.kernel.format.ArabicNumbers
import com.daftar.app.kernel.theme.DaftarColors

@Composable
fun TypesSection(viewModel: StockViewModel) {
    val rows by viewModel.typeRows.collectAsState()
    val sourceRows by viewModel.rows.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<ItemTypeEntity?>(null) }
    var associating by remember { mutableStateOf<ItemTypeEntity?>(null) }

    Box(Modifier.fillMaxSize()) {
        if (rows.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "لا أصناف بعد — أضيفي أول صنف",
                    style = MaterialTheme.typography.bodyLarge,
                    color = DaftarColors.TextSecondary,
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(rows, key = { it.type.id }) { row ->
                    TypeCard(
                        row = row,
                        onEdit = { editing = row.type },
                        onAssociate = { associating = row.type },
                    )
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = { showAdd = true },
            icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
            text = { Text("صنف جديد") },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
        )
    }

    if (showAdd) {
        TypeDialog(
            title = "صنف جديد",
            initialName = "",
            initialPrice = "",
            onSave = { name, price ->
                viewModel.addType(name, price)
                showAdd = false
            },
            onDismiss = { showAdd = false },
        )
    }

    editing?.let { type ->
        TypeDialog(
            title = "تعديل الصنف",
            initialName = type.name,
            initialPrice = type.askingPrice.toString(),
            onSave = { name, price ->
                viewModel.updateType(type.id, name, price)
                editing = null
            },
            onDismiss = { editing = null },
        )
    }

    associating?.let { type ->
        AssociateDialog(
            type = type,
            sources = sourceRows.map { it.source },
            onSave = { sourceId, qty, price, cost ->
                sourceRows.firstOrNull { it.source.id == sourceId }?.let {
                    viewModel.addIntakeLine(sourceId, type, qty, price, cost)
                }
                associating = null
            },
            onDismiss = { associating = null },
        )
    }
}

@Composable
private fun TypeCard(
    row: StockViewModel.TypeRow,
    onEdit: () -> Unit,
    onAssociate: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = DaftarColors.Surface1)) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(row.type.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    ArabicNumbers.format(row.type.askingPrice),
                    style = MaterialTheme.typography.titleMedium,
                    color = DaftarColors.Teal,
                )
            }
            if (row.associations.isEmpty()) {
                Text(
                    "غير مرتبط بمصدر — يُباع ويُنسب غير محدد",
                    style = MaterialTheme.typography.labelMedium,
                    color = DaftarColors.TextSecondary,
                )
            } else {
                row.associations.forEach { assoc ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            "${assoc.sourceLabel} @ ${ArabicNumbers.format(assoc.price)}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            "متبقٍ ~${ArabicNumbers.format(assoc.remaining.toLong())}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (assoc.remaining > 0) DaftarColors.TextSecondary else DaftarColors.Amber,
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                TextButton(onClick = onEdit) { Text("تعديل السعر") }
                TextButton(onClick = onAssociate) { Text("+ إضافة لمصدر") }
            }
        }
    }
}

@Composable
private fun TypeDialog(
    title: String,
    initialName: String,
    initialPrice: String,
    onSave: (String, Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var priceText by remember { mutableStateOf(initialPrice) }
    val price = ArabicNumbers.parseAmount(priceText)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DaftarColors.Surface1,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("الاسم") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { new -> priceText = new.filter { it.isDigit() } },
                    label = { Text("السعر الأساسي") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name, price) },
                enabled = name.isNotBlank() && price > 0,
            ) { Text("حفظ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
    )
}

@Composable
private fun AssociateDialog(
    type: ItemTypeEntity,
    sources: List<com.daftar.app.kernel.db.StockSourceEntity>,
    onSave: (sourceId: String, qty: Int, price: Long, cost: Long?) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedSourceId by remember { mutableStateOf<String?>(null) }
    var qtyText by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf(type.askingPrice.toString()) }
    var costText by remember { mutableStateOf("") }
    val qty = ArabicNumbers.parseAmount(qtyText).toInt()
    val price = ArabicNumbers.parseAmount(priceText)
    val cost = ArabicNumbers.parseAmount(costText).takeIf { it > 0 }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DaftarColors.Surface1,
        title = { Text("${type.name} — إضافة لمصدر", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (sources.isEmpty()) {
                    Text(
                        "لا مصادر بعد — أنشئي مصدراً أولاً",
                        style = MaterialTheme.typography.bodyMedium,
                        color = DaftarColors.TextSecondary,
                    )
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(sources, key = { it.id }) { source ->
                        FilterChip(
                            selected = selectedSourceId == source.id,
                            onClick = { selectedSourceId = source.id },
                            label = { Text(source.label) },
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
                    label = { Text("نقطة السعر") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr),
                )
                OutlinedTextField(
                    value = costText,
                    onValueChange = { new -> costText = new.filter { it.isDigit() } },
                    label = { Text("كلفة القطعة (اختياري)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { selectedSourceId?.let { onSave(it, qty, price, cost) } },
                enabled = selectedSourceId != null && qty > 0 && price > 0,
            ) { Text("حفظ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
    )
}
