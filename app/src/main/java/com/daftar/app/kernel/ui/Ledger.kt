package com.daftar.app.kernel.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daftar.app.kernel.i18n.Str
import com.daftar.app.kernel.theme.DaftarColors

// D43 — the ledger design language: one card per section, ruled rows inside,
// money in an aligned trailing column, eyebrow labels, double-rule sum lines.

@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier) {
    Text(
        text = if (Str.arabic) text else text.uppercase(),
        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.5.sp),
        color = DaftarColors.TextSecondary,
        modifier = modifier,
    )
}

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = DaftarColors.Surface1,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(content = content)
    }
}

@Composable
fun Hairline(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(DaftarColors.Hairline),
    )
}

@Composable
fun LedgerRow(
    title: String,
    subtitle: String?,
    amountText: String,
    amountColor: Color = DaftarColors.TextPrimary,
    trailingNote: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = DaftarColors.TextSecondary,
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                amountText,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = amountColor,
            )
            if (trailingNote != null) {
                Text(
                    trailingNote,
                    style = MaterialTheme.typography.labelMedium,
                    color = DaftarColors.TextSecondary,
                )
            }
        }
    }
}

// The signature: a paper cash-book's double rule above a sum.
@Composable
fun SumLine(label: String, amountText: String, amountColor: Color = DaftarColors.Teal) {
    Column(Modifier.fillMaxWidth()) {
        Hairline()
        Spacer(Modifier.height(2.dp))
        Hairline()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                label,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                amountText,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = amountColor,
            )
        }
    }
}
