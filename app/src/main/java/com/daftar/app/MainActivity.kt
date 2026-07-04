package com.daftar.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Cairo = FontFamily(Font(R.font.cairo))

private val Surface0 = Color(0xFF0E0E10)
private val TextPrimary = Color(0xFFF4F4F5)
private val Teal = Color(0xFF2DD4BF)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Surface0)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "مرحباً — هذا دفتر",
                        fontFamily = Cairo,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "نص تجريبي بخط القاهرة، من اليمين إلى اليسار:",
                        fontFamily = Cairo,
                        fontSize = 18.sp,
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "بنطال ٧٬٥٠٠ — فستان ٥٬٠٠٠ — جاكيت ١٠٬٠٠٠",
                        fontFamily = Cairo,
                        fontSize = 18.sp,
                        color = Teal
                    )
                }
            }
        }
    }
}
