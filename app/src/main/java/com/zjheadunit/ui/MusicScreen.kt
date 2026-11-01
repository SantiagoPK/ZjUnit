package com.zjheadunit.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import com.zjheadunit.music.MusicLibrary
import com.zjheadunit.music.PlayerHolder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MusicScreen(player: PlayerHolder, library: MusicLibrary) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var folders by remember { mutableStateOf(library.folders()) }
    var tracks by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var showFolders by remember { mutableStateOf(folders.isEmpty()) }
    var scanning by remember { mutableStateOf(false) }

    fun rescan() = scope.launch {
        scanning = true
        tracks = library.scan()
        scanning = false
    }
    LaunchedEffect(Unit) { rescan() }
    LaunchedEffect(Unit) { while (true) { player.refreshProgress(); delay(500) } }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) { library.addFolder(uri); folders = library.folders(); rescan() }
    }

    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        // Reproduciendo
        Column(
            Modifier.width(520.dp).fillMaxHeight()
                .background(ZJ.card, RoundedCornerShape(32.dp))
                .border(1.dp, ZJ.line, RoundedCornerShape(32.dp))
                .padding(32.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(player.title, color = ZJ.text, fontSize = 38.sp, fontWeight = FontWeight.ExtraBold,
                    maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 42.sp)
                Text(player.subtitle, color = ZJ.muted, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val frac = if (player.duration > 0) player.position.toFloat() / player.duration else 0f
                Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(ZJ.track)) {
                    Box(Modifier.fillMaxWidth(frac.coerceIn(0f, 1f)).fillMaxHeight().background(ZJ.accent))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(fmt(player.position), color = ZJ.muted, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(fmt(player.duration), color = ZJ.muted, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                RoundBtn("⏮", 88.dp, false) { player.previous() }
                Spacer(Modifier.width(28.dp))
                RoundBtn(if (player.isPlaying) "⏸" else "▶", 116.dp, true) { player.toggle() }
                Spacer(Modifier.width(28.dp))
                RoundBtn("⏭", 88.dp, false) { player.next() }
            }
        }

        // Biblioteca / carpetas
        Column(
            Modifier.weight(1f).fillMaxHeight()
                .background(ZJ.card, RoundedCornerShape(32.dp))
                .border(1.dp, ZJ.line, RoundedCornerShape(32.dp))
                .padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BigButton("Pistas", filled = !showFolders) { showFolders = false }
                BigButton("Carpetas", filled = showFolders) { showFolders = true }
                Spacer(Modifier.weight(1f))
                BigButton("Spotify", filled = false) { launchPackage(ctx, "com.spotify.music") }
            }
            if (showFolders) {
                Text("Elige las carpetas que quieres escuchar", color = ZJ.muted, fontSize = 17.sp)
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    itemsIndexed(folders) { _, uri ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZJ.cardHi).padding(horizontal = 20.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(library.folderName(uri), color = ZJ.text, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                            BigButton("Quitar", filled = false) { library.removeFolder(uri); folders = library.folders(); rescan() }
                        }
                    }
                }
                BigButton("Agregar carpeta", Modifier.fillMaxWidth()) { picker.launch(null) }
            } else {
                if (scanning) Text("Buscando música…", color = ZJ.muted, fontSize = 17.sp)
                if (!scanning && tracks.isEmpty()) Text("No hay pistas. Agrega una carpeta en la pestaña Carpetas.", color = ZJ.muted, fontSize = 17.sp)
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    itemsIndexed(tracks) { i, item ->
                        Column(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZJ.cardHi)
                                .clickable { player.playList(tracks, i) }
                                .padding(horizontal = 20.dp, vertical = 14.dp)
                        ) {
                            Text(item.mediaMetadata.title?.toString() ?: "", color = ZJ.text, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(item.mediaMetadata.artist?.toString() ?: "", color = ZJ.muted, fontSize = 15.sp, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RoundBtn(glyph: String, size: androidx.compose.ui.unit.Dp, filled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(size).clip(CircleShape)
            .background(if (filled) ZJ.accent else ZJ.cardHi)
            .border(1.dp, if (filled) ZJ.accent else ZJ.line2, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(glyph, color = if (filled) ZJ.bg else ZJ.text, fontSize = (size.value * 0.36f).sp)
    }
}

fun launchPackage(ctx: Context, pkg: String) {
    val i = ctx.packageManager.getLaunchIntentForPackage(pkg)
        ?: Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg"))
    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { ctx.startActivity(i) }
}

private fun fmt(ms: Long): String {
    val s = (ms / 1000).toInt()
    return "%d:%02d".format(s / 60, s % 60)
}
