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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import com.daftar.app.kernel.db.ItemTypeEntity
import com.daftar.app.kernel.db.StockSourceEntity
import com.daftar.app.kernel.i18n.Str
import com.daftar.app.kernel.theme.DaftarColors
import com.daftar.app.kernel.ui.AmountField
import com.daftar.app.kernel.ui.Hairline
import com.daftar.app.kernel.ui.QtyField
import com.daftar.app.kernel.ui.SectionCard

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
                    Str.noTypes,
                    style = MaterialTheme.typography.bodyLarge,
                    color = DaftarColors.TextSecondary,
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            ) {
                item {
                    SectionCard {
                        rows.forEachIndexed { index, row ->
                            if (index > 0) Hairline(Modifier.padding(horizontal = 16.dp))
                            TypeCard(
                                row = row,
                                onEdit = { editing = row.type },
                                onAssociate = { associating = row.type },
                            )
                        }
                    }
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = { showAdd = true },
            icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
            text = { Text(Str.newType) },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
        )
    }

    if (showAdd) {
        TypeDialog(
            title = Str.newType,
            initialName = "",
            initialPrice = 0L,
            onSave = { name, price ->
                viewModel.addType(name, price)
                showAdd = false
            },
            onDismiss = { showAdd = false },
        )
    }

    editing?.let { type ->
        TypeDialog(
            title = Str.editType,
            initialName = type.name,
            initialPrice = type.askingPrice,
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
                viewModel.addIntakeLine(sourceId, type, qty, price, cost)
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(row.type.name, style = MaterialTheme.typography.bodyLarge)
            if (row.associations.isEmpty()) {
                Text(
                    Str.noSourceLink,
                    style = MaterialTheme.typography.labelMedium,
                    color = DaftarColors.TextSecondary,
                )
            } else {
                row.associations.forEach { assoc ->
                    Text(
                        "${assoc.sourceLabel} @ ${Str.money(assoc.price)} · ${Str.remaining}${Str.count(assoc.remaining)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (assoc.remaining > 0) DaftarColors.TextSecondary else DaftarColors.Amber,
                    )
                }
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                Str.money(row.type.askingPrice),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = DaftarColors.Teal,
            )
            Text(
                Str.addToSource,
                style = MaterialTheme.typography.labelMedium,
                color = DaftarColors.Teal,
                modifier = Modifier.clickable(onClick = onAssociate),
            )
        }
    }
}

@Composable
private fun TypeDialog(
    title: String,
    initialName: String,
    initialPrice: Long,
    onSave: (String, Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var price by remember { mutableLongStateOf(initialPrice) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DaftarColors.Surface1,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(Str.name) },
                    singleLine = true,
                )
                AmountField(value = price, onValue = { price = it }, label = Str.basePrice)
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name, price) },
                enabled = name.isNotBlank() && price > 0,
            ) { Text(Str.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(Str.cancel) } },
    )
}

@Composable
private fun AssociateDialog(
    type: ItemTypeEntity,
    sources: List<StockSourceEntity>,
    onSave: (sourceId: String, qty: Int, price: Long, cost: Long?) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedSourceId by remember { mutableStateOf<String?>(null) }
    var qty by remember { mutableIntStateOf(0) }
    var price by remember { mutableLongStateOf(type.askingPrice) }
    var cost by remember { mutableLongStateOf(0L) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DaftarColors.Surface1,
        title = { Text("${type.name} — ${Str.addToSourceTitle}", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (sources.isEmpty()) {
                    Text(
                        Str.noSources,
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
                QtyField(value = qty, onValue = { qty = it }, label = Str.qtyApprox)
                AmountField(value = price, onValue = { price = it }, label = Str.pricePoint)
                AmountField(value = cost, onValue = { cost = it }, label = Str.unitCostOptional)
            }
        },
        confirmButton = {
            TextButton(
                onClick = { selectedSourceId?.let { onSave(it, qty, price, cost.takeIf { c -> c > 0 }) } },
                enabled = selectedSourceId != null && qty > 0 && price > 0,
            ) { Text(Str.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(Str.cancel) } },
    )
}
