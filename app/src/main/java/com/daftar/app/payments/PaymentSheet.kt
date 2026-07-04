package com.daftar.app.payments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.daftar.app.kernel.db.CustomerEntity
import com.daftar.app.kernel.db.ItemTypeEntity
import com.daftar.app.kernel.i18n.Str
import com.daftar.app.kernel.theme.DaftarColors
import com.daftar.app.kernel.ui.AmountField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentSheet(
    customers: List<CustomerEntity>,
    types: List<ItemTypeEntity>,
    suggestionLabel: (typeId: String, askedUnit: Long) -> String,
    title: String = Str.newPayment,
    saveLabel: String = Str.savePayment,
    onAddCustomer: (name: String, onCreated: (String) -> Unit) -> Unit = { _, _ -> },
    onSave: (amount: Long, customerId: String?, itemTypeId: String?, askedUnit: Long?) -> Unit,
    onDismiss: () -> Unit,
) {
    var amount by remember { mutableLongStateOf(0L) }
    var selectedCustomerId by remember { mutableStateOf<String?>(null) }
    var selectedType by remember { mutableStateOf<ItemTypeEntity?>(null) }
    var showAddCustomer by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = DaftarColors.Surface1) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            AmountField(value = amount, onValue = { amount = it }, label = Str.amount)
            if (types.isNotEmpty()) {
                Text(
                    Str.forWhat,
                    style = MaterialTheme.typography.labelMedium,
                    color = DaftarColors.TextSecondary,
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedType == null,
                            onClick = { selectedType = null },
                            label = { Text(Str.unspecified) },
                        )
                    }
                    items(types, key = { it.id }) { type ->
                        FilterChip(
                            selected = selectedType?.id == type.id,
                            onClick = {
                                selectedType = type
                                if (amount == 0L) amount = type.askingPrice
                            },
                            label = { Text("${type.name} ${Str.money(type.askingPrice)}") },
                        )
                    }
                }
                selectedType?.let { type ->
                    Text(
                        "${Str.sourceApprox} ${suggestionLabel(type.id, type.askingPrice)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = DaftarColors.TextSecondary,
                    )
                }
            }
            Text(
                Str.fromWho,
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
            Button(
                onClick = {
                    onSave(amount, selectedCustomerId, selectedType?.id, selectedType?.askingPrice)
                },
                enabled = amount > 0,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(saveLabel)
            }
        }
    }

    if (showAddCustomer) {
        var newName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddCustomer = false },
            containerColor = DaftarColors.Surface1,
            title = { Text(Str.newCustomer, style = MaterialTheme.typography.titleLarge) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text(Str.name) },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { onAddCustomer(newName) { id -> selectedCustomerId = id }; showAddCustomer = false },
                    enabled = newName.isNotBlank(),
                ) { Text(Str.save) }
            },
            dismissButton = { TextButton(onClick = { showAddCustomer = false }) { Text(Str.cancel) } },
        )
    }
}
