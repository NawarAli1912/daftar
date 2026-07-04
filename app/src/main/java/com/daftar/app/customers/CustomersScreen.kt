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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.daftar.app.kernel.i18n.Str
import com.daftar.app.kernel.ledger.BalanceSide
import com.daftar.app.kernel.ledger.LedgerMath
import com.daftar.app.kernel.theme.DaftarColors
import com.daftar.app.kernel.ui.AmountField
import com.daftar.app.kernel.ui.Eyebrow
import com.daftar.app.kernel.ui.Hairline
import com.daftar.app.kernel.ui.LedgerRow
import com.daftar.app.kernel.ui.SectionCard
import com.daftar.app.kernel.ui.SumLine

@Composable
fun CustomersScreen(viewModel: CustomersViewModel = hiltViewModel()) {
    val rows by viewModel.rows.collectAsState()
    var showAdd by remember { mutableStateOf(false) }

    val outstanding = rows.filter { it.balance > 0 }.sumOf { it.balance }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 120.dp),
        ) {
            item {
                Eyebrow(Str.customersTitle)
                Spacer(Modifier.height(4.dp))
            }
            if (rows.isEmpty()) {
                item {
                    Spacer(Modifier.height(48.dp))
                    Text(
                        text = Str.noCustomers,
                        style = MaterialTheme.typography.bodyLarge,
                        color = DaftarColors.TextSecondary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                item {
                    SectionCard(modifier = Modifier.padding(top = 8.dp)) {
                        rows.forEachIndexed { index, row ->
                            if (index > 0) Hairline(Modifier.padding(horizontal = 16.dp))
                            CustomerRow(row)
                        }
                        if (outstanding > 0) SumLine(Str.totalOutstanding, Str.money(outstanding), DaftarColors.Amber)
                    }
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = { showAdd = true },
            icon = { Icon(Icons.Outlined.PersonAdd, contentDescription = null) },
            text = { Text(Str.newCustomer) },
            containerColor = DaftarColors.Teal,
            contentColor = DaftarColors.OnTeal,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
        )
    }

    if (showAdd) {
        AddCustomerSheet(
            onSave = { name, phone, openingBalance ->
                viewModel.addCustomer(name, phone, openingBalance)
                showAdd = false
            },
            onDismiss = { showAdd = false },
        )
    }
}

@Composable
private fun CustomerRow(row: CustomersViewModel.Row) {
    val (note, color) = when (LedgerMath.side(row.balance)) {
        BalanceSide.OWES_SHOP -> Str.owesShop to DaftarColors.Amber
        BalanceSide.SHOP_OWES -> Str.shopOwes to DaftarColors.Green
        BalanceSide.SETTLED -> null to DaftarColors.TextSecondary
    }
    LedgerRow(
        title = row.customer.name,
        subtitle = row.customer.phone,
        amountText = Str.money(kotlin.math.abs(row.balance)),
        amountColor = color,
        trailingNote = note,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddCustomerSheet(
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

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = DaftarColors.Surface1) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(Str.newCustomer, style = MaterialTheme.typography.titleLarge)
            TextButton(
                onClick = {
                    pickContact.launch(
                        Intent(Intent.ACTION_PICK).apply {
                            type = ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE
                        }
                    )
                },
                contentPadding = PaddingValues(0.dp),
            ) {
                Icon(Icons.Outlined.Contacts, contentDescription = null)
                Text("  ${Str.fromContacts}")
            }
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(Str.name) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text(Str.phoneOptional) },
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr),
                modifier = Modifier.fillMaxWidth(),
            )
            AmountField(value = opening, onValue = { opening = it }, label = Str.oldDebtOptional)
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onDismiss) { Text(Str.cancel) }
                Spacer(Modifier.weight(1f))
                Button(onClick = { onSave(name, phone, opening) }, enabled = name.isNotBlank()) {
                    Text(Str.save)
                }
            }
        }
    }
}
