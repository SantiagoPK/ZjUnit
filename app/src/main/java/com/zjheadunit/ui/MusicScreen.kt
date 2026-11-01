package com.zjheadunit.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import com.zjheadunit.music.MusicLibrary
import com.zjheadunit.music.PlayerHolder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

private enum class Pane { TRACKS, FOLDERS, BROWSER }

@Composable
fun MusicScreen(player: PlayerHolder, library: MusicLibrary) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var folders by remember { mutableStateOf(library.folders()) }
    var tracks by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var pane by remember { mutableStateOf(if (folders.isEmpty()) Pane.FOLDERS else Pane.TRACKS) }
    var scanning by remember { mutableStateOf(false) }

    fun rescan() = scope.launch {
        scanning = true
        tracks = library.scan()
        scanning = false
    }
    LaunchedEffect(Unit) { rescan() }
    LaunchedEffect(Unit) { while (true) { player.refreshProgress(); delay(500) } }

    // Respaldo: selector de carpetas del sistema (si el celular lo trae)
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) { library.addTree(uri); folders = library.folders(); rescan() }
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

        // Biblioteca / carpetas / explorador
        Column(
            Modifier.weight(1f).fillMaxHeight()
                .background(ZJ.card, RoundedCornerShape(32.dp))
                .border(1.dp, ZJ.line, RoundedCornerShape(32.dp))
                .padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BigButton("Pistas", filled = pane == Pane.TRACKS) { pane = Pane.TRACKS }
                BigButton("Carpetas", filled = pane != Pane.TRACKS) { pane = Pane.FOLDERS }
                Spacer(Modifier.weight(1f))
                BigButton("Spotify", filled = false) { launchPackage(ctx, "com.spotify.music") }
            }
            when (pane) {
                Pane.TRACKS -> {
                    if (scanning) Text("Buscando música…", color = ZJ.muted, fontSize = 17.sp)
                    if (!scanning && tracks.isEmpty())
                        Text("No hay pistas. Agrega una carpeta en la pestaña Carpetas.", color = ZJ.muted, fontSize = 17.sp)
                    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        itemsIndexed(tracks) { i, item ->
                            Column(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZJ.cardHi)
                                    .clickable { player.playList(tracks, i) }
                                    .padding(horizontal = 20.dp, vertical = 14.dp)
                            ) {
                                Text(item.mediaMetadata.title?.toString() ?: "", color = ZJ.text, fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(item.mediaMetadata.artist?.toString() ?: "", color = ZJ.muted, fontSize = 15.sp, maxLines = 1)
                            }
                        }
                    }
                }
                Pane.FOLDERS -> {
                    Text("Carpetas de música elegidas", color = ZJ.muted, fontSize = 17.sp)
                    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(folders) { entry ->
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZJ.cardHi)
                                    .padding(horizontal = 20.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(library.displayName(entry), color = ZJ.text, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
                                    if (!entry.startsWith("content://"))
                                        Text(entry, color = ZJ.muted, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                BigButton("Quitar", filled = false) { library.remove(entry); folders = library.folders(); rescan() }
                            }
                        }
                    }
                    BigButton("Agregar carpeta", Modifier.fillMaxWidth()) { pane = Pane.BROWSER }
                    BigButton("Usar el selector del sistema", Modifier.fillMaxWidth(), filled = false) {
                        runCatching { picker.launch(null) }
                    }
                }
                Pane.BROWSER -> FolderBrowser(
                    library = library,
                    onPick = { path ->
                        library.addPath(path); folders = library.folders(); rescan(); pane = Pane.FOLDERS
                    },
                    onCancel = { pane = Pane.FOLDERS }
                )
            }
        }
    }
}

/** Explorador de carpetas dentro de la app: almacenamiento interno y tarjeta SD. */
@Composable
private fun ColumnScope.FolderBrowser(library: MusicLibrary, onPick: (String) -> Unit, onCancel: () -> Unit) {
    val ctx = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var access by remember { mutableStateOf(library.hasAllFilesAccess()) }
    var current by remember { mutableStateOf<File?>(null) }
    val roots = remember { library.roots() }

    // Al volver de los ajustes del sistema, vuelve a comprobar el permiso
    DisposableEffect(lifecycle) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) access = library.hasAllFilesAccess()
        }
        lifecycle.addObserver(obs)
        onDispose { lifecycle.removeObserver(obs) }
    }

    if (!access) {
        Text(
            "Para explorar tus carpetas, la app necesita permiso de acceso a todos los archivos.",
            color = ZJ.text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, lineHeight = 28.sp
        )
        Text("Se abrirán los ajustes: activa el interruptor y vuelve a la app.", color = ZJ.muted, fontSize = 17.sp)
        Spacer(Modifier.weight(1f))
        BigButton("Dar permiso", Modifier.fillMaxWidth()) {
            val i = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:${ctx.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { ctx.startActivity(i) }.onFailure {
                runCatching {
                    ctx.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }
        }
        BigButton("Cancelar", Modifier.fillMaxWidth(), filled = false, onClick = onCancel)
        return
    }

    val dir = current
    if (dir == null) {
        Text("¿Dónde está tu música?", color = ZJ.text, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(roots) { (label, file) ->
                FolderRow(label, file.path) { current = file }
            }
        }
        BigButton("Cancelar", Modifier.fillMaxWidth(), filled = false, onClick = onCancel)
    } else {
        val subs = remember(dir) {
            dir.listFiles()?.filter { it.isDirectory && !it.name.startsWith(".") }
                ?.sortedBy { it.name.lowercase() } ?: emptyList()
        }
        Text(dir.path, color = ZJ.muted, fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (subs.isEmpty()) item { Text("No hay más carpetas aquí.", color = ZJ.muted, fontSize = 17.sp) }
            items(subs) { f -> FolderRow(f.name, null) { current = f } }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BigButton("Subir", filled = false) {
                val parent = dir.parentFile
                current = if (parent == null || roots.any { it.second.path == dir.path }) null else parent
            }
            BigButton("Elegir esta carpeta", Modifier.weight(1f)) { onPick(dir.path) }
        }
    }
}

@Composable
private fun FolderRow(title: String, subtitle: String?, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZJ.cardHi)
            .clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(title, color = ZJ.text, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (subtitle != null) Text(subtitle, color = ZJ.muted, fontSize = 14.sp, maxLines = 1)
    }
}

@Composable
fun RoundBtn(glyph: String, size: Dp, filled: Boolean, onClick: () -> Unit) {
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
