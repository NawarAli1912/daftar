package com.daftar.app.payments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.daftar.app.kernel.db.CustomerEntity
import com.daftar.app.kernel.theme.DaftarColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentSheet(
    customers: List<CustomerEntity>,
    onSave: (amount: Long, customerId: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var amountText by remember { mutableStateOf("") }
    var selectedCustomerId by remember { mutableStateOf<String?>(null) }
    val amount = amountText.filter { it.isDigit() }.toLongOrNull() ?: 0L

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = DaftarColors.Surface1) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("دفعة جديدة", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = amountText,
                onValueChange = { new -> amountText = new.filter { it.isDigit() } },
                label = { Text("المبلغ") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "من؟ (اختياري — D11)",
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
            Button(
                onClick = { onSave(amount, selectedCustomerId) },
                enabled = amount > 0,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("حفظ الدفعة")
            }
        }
    }
}
