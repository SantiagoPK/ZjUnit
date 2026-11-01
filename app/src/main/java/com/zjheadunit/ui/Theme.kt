package com.zjheadunit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object ZJ {
    val bg = Color(0xFF0A0C0F)
    val rail = Color(0xFF0E1115)
    val card = Color(0xFF14181D)
    val cardHi = Color(0xFF1C222A)
    val line = Color(0xFF1C222A)
    val line2 = Color(0xFF2A323D)
    val text = Color(0xFFF3F5F7)
    val muted = Color(0xFFA0AAB6)
    val track = Color(0xFF262D36)
    var accent = Color(0xFFFFB22E)
}

@Composable
fun HeadUnitTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = ZJ.bg, surface = ZJ.card, primary = ZJ.accent,
            onPrimary = ZJ.bg, onSurface = ZJ.text, onBackground = ZJ.text
        ),
        content = content
    )
}

/** Botón grande, fácil de tocar mientras se conduce (alto mínimo 64dp). */
@Composable
fun BigButton(
    label: String,
    modifier: Modifier = Modifier,
    filled: Boolean = true,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = modifier
            .heightIn(min = 64.dp)
            .clip(shape)
            .background(if (filled) ZJ.accent else ZJ.cardHi)
            .border(1.dp, if (filled) ZJ.accent else ZJ.line2, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (filled) ZJ.bg else ZJ.text,
            fontSize = 19.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
fun Card(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .background(ZJ.card, RoundedCornerShape(32.dp))
            .border(1.dp, ZJ.line, RoundedCornerShape(32.dp))
            .padding(28.dp),
        content = content
    )
}
