package com.daftar.app.customers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.daftar.app.kernel.format.ArabicNumbers
import com.daftar.app.kernel.ledger.BalanceSide
import com.daftar.app.kernel.ledger.LedgerMath
import com.daftar.app.kernel.theme.DaftarColors

@Composable
fun CustomersScreen(viewModel: CustomersViewModel = hiltViewModel()) {
    val rows by viewModel.rows.collectAsState()
    var showAdd by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        if (rows.isEmpty()) {
            Text(
                text = "لا زبائن بعد — أضيفي أول زبون",
                style = MaterialTheme.typography.bodyLarge,
                color = DaftarColors.TextSecondary,
                modifier = Modifier.align(Alignment.Center),
            )
        } else {
            LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(rows, key = { it.customer.id }) { row -> CustomerCard(row) }
            }
        }
        ExtendedFloatingActionButton(
            onClick = { showAdd = true },
            icon = { Icon(Icons.Outlined.PersonAdd, contentDescription = null) },
            text = { Text("زبون جديد") },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
        )
    }

    if (showAdd) {
        AddCustomerDialog(
            onSave = { name, phone ->
                viewModel.addCustomer(name, phone)
                showAdd = false
            },
            onDismiss = { showAdd = false },
        )
    }
}

@Composable
private fun CustomerCard(row: CustomersViewModel.Row) {
    Card(colors = CardDefaults.cardColors(containerColor = DaftarColors.Surface1)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(row.customer.name, style = MaterialTheme.typography.titleMedium)
            BalanceText(row.balance)
        }
    }
}

@Composable
private fun BalanceText(balance: Long) {
    val amount = ArabicNumbers.format(kotlin.math.abs(balance))
    when (LedgerMath.side(balance)) {
        BalanceSide.OWES_SHOP -> Text(
            "عليها $amount",
            style = MaterialTheme.typography.labelLarge,
            color = DaftarColors.Amber,
        )
        BalanceSide.SHOP_OWES -> Text(
            "لها $amount",
            style = MaterialTheme.typography.labelLarge,
            color = DaftarColors.Green,
        )
        BalanceSide.SETTLED -> Text(
            "٠",
            style = MaterialTheme.typography.labelLarge,
            color = DaftarColors.TextSecondary,
        )
    }
}

@Composable
private fun AddCustomerDialog(onSave: (String, String?) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DaftarColors.Surface1,
        title = { Text("زبون جديد", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("الاسم") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("الهاتف (اختياري)") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name, phone) }, enabled = name.isNotBlank()) {
                Text("حفظ")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
    )
}
