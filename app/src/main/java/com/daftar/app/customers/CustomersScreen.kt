package com.daftar.app.customers

import android.content.Intent
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.daftar.app.kernel.i18n.Str
import com.daftar.app.kernel.ledger.BalanceSide
import com.daftar.app.kernel.ledger.LedgerMath
import com.daftar.app.kernel.theme.DaftarColors
import com.daftar.app.kernel.ui.AmountField

@Composable
fun CustomersScreen(viewModel: CustomersViewModel = hiltViewModel()) {
    val rows by viewModel.rows.collectAsState()
    var showAdd by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        if (rows.isEmpty()) {
            Text(
                text = Str.noCustomers,
                style = MaterialTheme.typography.bodyLarge,
                color = DaftarColors.TextSecondary,
                modifier = Modifier.align(Alignment.Center),
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(rows, key = { it.customer.id }) { row -> CustomerCard(row) }
            }
        }
        ExtendedFloatingActionButton(
            onClick = { showAdd = true },
            icon = { Icon(Icons.Outlined.PersonAdd, contentDescription = null) },
            text = { Text(Str.newCustomer) },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
        )
    }

    if (showAdd) {
        AddCustomerDialog(
            onSave = { name, phone, openingBalance ->
                viewModel.addCustomer(name, phone, openingBalance)
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
    val amount = Str.money(kotlin.math.abs(balance))
    when (LedgerMath.side(balance)) {
        BalanceSide.OWES_SHOP -> Text(
            "${Str.owesShop} $amount",
            style = MaterialTheme.typography.labelLarge,
            color = DaftarColors.Amber,
        )
        BalanceSide.SHOP_OWES -> Text(
            "${Str.shopOwes} $amount",
            style = MaterialTheme.typography.labelLarge,
            color = DaftarColors.Green,
        )
        BalanceSide.SETTLED -> Text(
            Str.money(0),
            style = MaterialTheme.typography.labelLarge,
            color = DaftarColors.TextSecondary,
        )
    }
}

@Composable
private fun AddCustomerDialog(
    onSave: (name: String, phone: String?, openingBalance: Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var opening by remember { mutableLongStateOf(0L) }
    val context = LocalContext.current

    val pickContact = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.data ?: return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                ),
                null, null, null,
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    name = cursor.getString(0) ?: ""
                    phone = cursor.getString(1) ?: ""
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DaftarColors.Surface1,
        title = { Text(Str.newCustomer, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TextButton(onClick = {
                    pickContact.launch(
                        Intent(Intent.ACTION_PICK).apply {
                            type = ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE
                        }
                    )
                }) {
                    Icon(Icons.Outlined.Contacts, contentDescription = null)
                    Text("  ${Str.fromContacts}")
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(Str.name) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(Str.phoneOptional) },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr),
                )
                AmountField(value = opening, onValue = { opening = it }, label = Str.oldDebtOptional)
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name, phone, opening) }, enabled = name.isNotBlank()) {
                Text(Str.save)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(Str.cancel) } },
    )
}
