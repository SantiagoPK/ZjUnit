package com.zjheadunit.ui

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

/* ---------- INICIO ---------- */
@Composable
fun HomeScreen(onMap: () -> Unit, onMusic: () -> Unit, onApps: () -> Unit, onBt: () -> Unit) {
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        Column(
            Modifier.weight(1.5f).fillMaxHeight()
                .background(ZJ.card, RoundedCornerShape(32.dp))
                .border(1.dp, ZJ.line, RoundedCornerShape(32.dp))
                .padding(32.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Text("¿A dónde vamos?", color = ZJ.text, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(18.dp))
            BigButton("Abrir mapa", onClick = onMap)
        }
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Tile("Música", Modifier.weight(1f), onMusic)
                Tile("Bluetooth", Modifier.weight(1f), onBt)
            }
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Tile("Todas las apps", Modifier.weight(2f), onApps)
            }
        }
    }
}

@Composable
private fun Tile(label: String, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.fillMaxHeight()
            .clip(RoundedCornerShape(28.dp))
            .background(ZJ.card)
            .border(1.dp, ZJ.line, RoundedCornerShape(28.dp))
            .clickable(onClick = onClick)
            .padding(24.dp),
        contentAlignment = Alignment.BottomStart
    ) { Text(label, color = ZJ.text, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold) }
}

/* ---------- APPS (todas las instaladas, como un launcher real) ---------- */
private data class AppEntry(val label: String, val pkg: String)

@Composable
fun AppsScreen() {
    val ctx = LocalContext.current
    val apps = remember {
        val pm = ctx.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        pm.queryIntentActivities(intent, 0)
            .map { AppEntry(it.loadLabel(pm).toString(), it.activityInfo.packageName) }
            .filter { it.pkg != ctx.packageName }
            .distinctBy { it.pkg }
            .sortedBy { it.label.lowercase() }
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(apps) { a ->
            Box(
                Modifier.height(140.dp).clip(RoundedCornerShape(28.dp))
                    .background(ZJ.card).border(1.dp, ZJ.line, RoundedCornerShape(28.dp))
                    .clickable { launchPackage(ctx, a.pkg) }.padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(a.label, color = ZJ.text, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, maxLines = 2)
            }
        }
    }
}

/* ---------- BLUETOOTH ---------- */
@SuppressLint("MissingPermission")
@Composable
fun BluetoothScreen() {
    val ctx = LocalContext.current
    val adapter = remember { (ctx.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter }
    var granted by remember {
        mutableStateOf(
            android.os.Build.VERSION.SDK_INT < 31 ||
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        )
    }
    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    LaunchedEffect(Unit) { if (!granted) perm.launch(Manifest.permission.BLUETOOTH_CONNECT) }

    val devices = if (granted) adapter?.bondedDevices?.toList().orEmpty() else emptyList()
    val on = adapter?.isEnabled == true

    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        Card(Modifier.width(400.dp).fillMaxHeight(), ) {
            Text("Bluetooth", color = ZJ.text, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(12.dp))
            Text(
                when {
                    adapter == null -> "Esta tablet no tiene Bluetooth."
                    on -> "Encendido. Conectar y desconectar dispositivos se hace desde los ajustes del sistema."
                    else -> "Apagado. Enciéndelo desde los ajustes del sistema."
                },
                color = ZJ.muted, fontSize = 17.sp, lineHeight = 25.sp
            )
            Spacer(Modifier.weight(1f))
            BigButton("Abrir ajustes Bluetooth", Modifier.fillMaxWidth()) {
                ctx.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
        Card(Modifier.weight(1f).fillMaxHeight()) {
            Text("DISPOSITIVOS VINCULADOS", color = ZJ.muted, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(14.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(devices) { d ->
                    Text(
                        d.name ?: d.address, color = ZJ.text, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZJ.cardHi).padding(20.dp)
                    )
                }
            }
        }
    }
}

/* ---------- AJUSTES ---------- */
@Composable
fun SettingsScreen() {
    val ctx = LocalContext.current
    Card(Modifier.fillMaxSize()) {
        Text("Ajustes", color = ZJ.text, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(20.dp))
        Text("Para que esta app arranque al encender la tablet, ponla como pantalla de inicio.", color = ZJ.muted, fontSize = 18.sp)
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BigButton("Elegir pantalla de inicio") {
                ctx.startActivity(Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            BigButton("Ajustes de pantalla", filled = false) {
                ctx.startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
    }
}
