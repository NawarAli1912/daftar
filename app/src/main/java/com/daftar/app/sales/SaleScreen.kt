package com.daftar.app.sales

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.daftar.app.kernel.i18n.Str
import com.daftar.app.kernel.ledger.LedgerMath
import com.daftar.app.kernel.theme.DaftarColors
import com.daftar.app.kernel.ui.AmountField

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SaleScreen(
    onSave: (lines: List<DraftLine>, customerId: String?, paidNow: Long) -> Unit,
    onClose: () -> Unit,
    viewModel: SalesViewModel = hiltViewModel(),
) {
    val chips by viewModel.chips.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val basket = remember { mutableStateListOf<DraftLine>() }
    var selectedCustomerId by remember { mutableStateOf<String?>(null) }
    var paidNow by remember { mutableLongStateOf(0L) }
    var showAddType by remember { mutableStateOf(false) }
    var showAddCustomer by remember { mutableStateOf(false) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val total = basket.sumOf { LedgerMath.lineTotal(it.qty, it.agreedUnit) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DaftarColors.Surface0)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(Str.sale, style = MaterialTheme.typography.headlineSmall)
            IconButton(onClick = onClose) {
                Icon(Icons.Outlined.Close, contentDescription = Str.close)
            }
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            chips.forEach { chip ->
                AssistChip(
                    onClick = {
                        val existing = basket.indexOfFirst {
                            it.typeId == chip.typeId && it.askedUnit == chip.price &&
                                it.agreedUnit == it.askedUnit
                        }
                        if (existing >= 0) {
                            basket[existing] = basket[existing]
                                .copy(qty = basket[existing].qty + 1)
                        } else {
                            basket.add(
                                DraftLine(
                                    typeId = chip.typeId,
                                    typeName = chip.name,
                                    qty = 1,
                                    askedUnit = chip.price,
                                    agreedUnit = chip.price,
                                )
                            )
                        }
                    },
                    label = { Text("${chip.name} ${Str.money(chip.price)}") },
                )
            }
            AssistChip(
                onClick = { showAddType = true },
                label = { Text(Str.addTypeChip) },
            )
        }

        if (basket.isEmpty()) {
            Text(
                Str.basketHint,
                style = MaterialTheme.typography.bodyMedium,
                color = DaftarColors.TextSecondary,
            )
        }

        basket.forEachIndexed { index, line ->
            BasketLineCard(
                line = line,
                suggestionLabel = viewModel.suggestionLabel(line.typeId, line.askedUnit),
                onQtyChange = { qty -> if (qty >= 1) basket[index] = line.copy(qty = qty) },
                onPriceChange = { price -> basket[index] = line.copy(agreedUnit = price) },
                onRemove = { basket.removeAt(index) },
            )
        }

        Text(
            Str.customerOptional,
            style = MaterialTheme.typography.labelMedium,
            color = DaftarColors.TextSecondary,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = selectedCustomerId == null,
                    onClick = { selectedCustomerId = null },
                    label = { Text(Str.unspecified) },
                )
            }
            items(customers, key = { it.id }) { customer ->
                FilterChip(
                    selected = selectedCustomerId == customer.id,
                    onClick = { selectedCustomerId = customer.id },
                    label = { Text(customer.name) },
                )
            }
            item {
                AssistChip(
                    onClick = { showAddCustomer = true },
                    label = { Text(Str.newCustomer) },
                )
            }
        }

        AmountField(value = paidNow, onValue = { paidNow = it }, label = Str.paidNow)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(Str.total, style = MaterialTheme.typography.titleMedium)
            Text(
                Str.money(total),
                style = MaterialTheme.typography.titleMedium,
                color = DaftarColors.Teal,
            )
        }
        if (selectedCustomerId != null && total - paidNow > 0) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    Str.remainderDebt,
                    style = MaterialTheme.typography.bodyMedium,
                    color = DaftarColors.TextSecondary,
                )
                Text(
                    Str.money(total - paidNow),
                    style = MaterialTheme.typography.titleMedium,
                    color = DaftarColors.Amber,
                )
            }
        }

        Button(
            onClick = { onSave(basket.toList(), selectedCustomerId, paidNow) },
            enabled = basket.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(Str.saveSale)
        }
    }

    if (showAddType) {
        AddTypeDialog(
            onSave = { name, price ->
                viewModel.addType(name, price)
                showAddType = false
            },
            onDismiss = { showAddType = false },
        )
    }

    if (showAddCustomer) {
        QuickNameDialog(
            title = Str.newCustomer,
            onSave = { name ->
                scope.launch {
                    selectedCustomerId = viewModel.addCustomerInline(name)
                }
                showAddCustomer = false
            },
            onDismiss = { showAddCustomer = false },
        )
    }
}

@Composable
private fun QuickNameDialog(
    title: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DaftarColors.Surface1,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(Str.name) },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(name) }, enabled = name.isNotBlank()) { Text(Str.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(Str.cancel) } },
    )
}

@Composable
private fun BasketLineCard(
    line: DraftLine,
    suggestionLabel: String,
    onQtyChange: (Int) -> Unit,
    onPriceChange: (Long) -> Unit,
    onRemove: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = DaftarColors.Surface1)) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(line.typeName, style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { onQtyChange(line.qty - 1) }) { Text("−") }
                    Text(
                        Str.count(line.qty),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    TextButton(onClick = { onQtyChange(line.qty + 1) }) { Text("+") }
                    IconButton(onClick = onRemove) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = Str.delete,
                            tint = DaftarColors.TextSecondary,
                        )
                    }
                }
            }
            AmountField(
                value = line.agreedUnit,
                onValue = onPriceChange,
                label = Str.unitPrice,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "${Str.sourceApprox} $suggestionLabel",
                    style = MaterialTheme.typography.labelMedium,
                    color = DaftarColors.TextSecondary,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (line.agreedUnit != line.askedUnit) {
                        Text(
                            Str.money(line.askedUnit),
                            style = MaterialTheme.typography.labelMedium.copy(
                                textDecoration = TextDecoration.LineThrough
                            ),
                            color = DaftarColors.TextSecondary,
                        )
                    }
                    Text(
                        Str.money(LedgerMath.lineTotal(line.qty, line.agreedUnit)),
                        style = MaterialTheme.typography.titleMedium,
                        color = DaftarColors.Teal,
                    )
                }
            }
        }
    }
}

@Composable
private fun AddTypeDialog(onSave: (String, Long) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var price by remember { mutableLongStateOf(0L) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DaftarColors.Surface1,
        title = { Text(Str.newType, style = MaterialTheme.typography.titleLarge) },
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
