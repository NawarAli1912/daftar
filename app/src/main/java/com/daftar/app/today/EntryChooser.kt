package com.daftar.app.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AssignmentReturn
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.daftar.app.kernel.i18n.Str
import com.daftar.app.kernel.theme.DaftarColors

enum class EntryType { SALE, PAYMENT, RETURN }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryChooser(onPick: (EntryType) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = DaftarColors.Surface1) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(Str.whatToRecord, style = MaterialTheme.typography.titleLarge)
            ChoiceRow(Str.sale, Icons.Outlined.ShoppingBag) { onPick(EntryType.SALE) }
            ChoiceRow(Str.payment, Icons.Outlined.Payments) { onPick(EntryType.PAYMENT) }
            ChoiceRow(Str.entryReturn, Icons.Outlined.AssignmentReturn) { onPick(EntryType.RETURN) }
        }
    }
}

@Composable
private fun ChoiceRow(label: String, icon: ImageVector, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = DaftarColors.Surface2,
        shape = MaterialTheme.shapes.medium,
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(icon, contentDescription = null, tint = DaftarColors.Teal, modifier = Modifier.size(26.dp))
            Text(label, style = MaterialTheme.typography.titleMedium)
        }
    }
}
