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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
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
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.daftar.app.kernel.format.ArabicNumbers
import com.daftar.app.kernel.ledger.LedgerMath
import com.daftar.app.kernel.theme.DaftarColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SaleScreen(
    onSave: (lines: List<DraftLine>, customerId: String?, paidNow: Long) -> Unit,
    onClose: () -> Unit,
    viewModel: SalesViewModel = hiltViewModel(),
) {
    val types by viewModel.types.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val basket = remember { mutableStateListOf<DraftLine>() }
    var selectedCustomerId by remember { mutableStateOf<String?>(null) }
    var paidNowText by remember { mutableStateOf("") }
    var showAddType by remember { mutableStateOf(false) }
    val paidNow = ArabicNumbers.parseAmount(paidNowText)
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
            Text("بيع", style = MaterialTheme.typography.headlineSmall)
            IconButton(onClick = onClose) {
                Icon(Icons.Outlined.Close, contentDescription = "إغلاق")
            }
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            types.forEach { type ->
                AssistChip(
                    onClick = {
                        val existing = basket.indexOfFirst {
                            it.typeId == type.id && it.agreedUnit == it.askedUnit
                        }
                        if (existing >= 0) {
                            basket[existing] = basket[existing]
                                .copy(qty = basket[existing].qty + 1)
                        } else {
                            basket.add(
                                DraftLine(
                                    typeId = type.id,
                                    typeName = type.name,
                                    qty = 1,
                                    askedUnit = type.askingPrice,
                                    agreedUnit = type.askingPrice,
                                )
                            )
                        }
                    },
                    label = { Text("${type.name} ${ArabicNumbers.format(type.askingPrice)}") },
                )
            }
            AssistChip(
                onClick = { showAddType = true },
                label = { Text("+ صنف") },
            )
        }

        if (basket.isEmpty()) {
            Text(
                "اضغطي على صنف لإضافته للسلة",
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
            "الزبون (اختياري)",
            style = MaterialTheme.typography.labelMedium,
            color = DaftarColors.TextSecondary,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = selectedCustomerId == null,
                    onClick = { selectedCustomerId = null },
                    label = { Text("غير محدد") },
                )
            }
            items(customers, key = { it.id }) { customer ->
                FilterChip(
                    selected = selectedCustomerId == customer.id,
                    onClick = { selectedCustomerId = customer.id },
                    label = { Text(customer.name) },
                )
            }
        }

        OutlinedTextField(
            value = paidNowText,
            onValueChange = { new -> paidNowText = new.filter { it.isDigit() } },
            label = { Text("المدفوع الآن") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr),
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("الإجمالي", style = MaterialTheme.typography.titleMedium)
            Text(
                ArabicNumbers.format(total),
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
                    "الباقي دين",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DaftarColors.TextSecondary,
                )
                Text(
                    ArabicNumbers.format(total - paidNow),
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
            Text("حفظ البيع")
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
}

@Composable
private fun BasketLineCard(
    line: DraftLine,
    suggestionLabel: String,
    onQtyChange: (Int) -> Unit,
    onPriceChange: (Long) -> Unit,
    onRemove: () -> Unit,
) {
    var priceText by remember(line.typeId) { mutableStateOf(line.agreedUnit.toString()) }
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
                        ArabicNumbers.format(line.qty.toLong()),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    TextButton(onClick = { onQtyChange(line.qty + 1) }) { Text("+") }
                    IconButton(onClick = onRemove) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = "حذف",
                            tint = DaftarColors.TextSecondary,
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { new ->
                        priceText = new.filter { it.isDigit() }
                        onPriceChange(ArabicNumbers.parseAmount(priceText))
                    },
                    label = { Text("سعر القطعة") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr),
                    modifier = Modifier.weight(1f),
                )
                if (line.agreedUnit != line.askedUnit) {
                    Text(
                        ArabicNumbers.format(line.askedUnit),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            textDecoration = TextDecoration.LineThrough
                        ),
                        color = DaftarColors.TextSecondary,
                    )
                }
                Text(
                    ArabicNumbers.format(LedgerMath.lineTotal(line.qty, line.agreedUnit)),
                    style = MaterialTheme.typography.titleMedium,
                    color = DaftarColors.Teal,
                )
            }
            Text(
                "المصدر تقريباً: $suggestionLabel",
                style = MaterialTheme.typography.labelMedium,
                color = DaftarColors.TextSecondary,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun AddTypeDialog(onSave: (String, Long) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    val price = ArabicNumbers.parseAmount(priceText)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DaftarColors.Surface1,
        title = { Text("صنف جديد", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("الاسم (بنطال، فستان…)") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { new -> priceText = new.filter { it.isDigit() } },
                    label = { Text("سعر العرض") },
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
