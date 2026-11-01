package com.zjheadunit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.zjheadunit.music.MusicLibrary
import com.zjheadunit.music.PlayerHolder
import com.zjheadunit.ui.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class Screen(val label: String) {
    HOME("Inicio"), MAP("Mapa"), MUSIC("Música"), APPS("Apps"), BT("Bluetooth"), SETTINGS("Ajustes")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        setContent { HeadUnitTheme { Root() } }
    }
}

@Composable
fun Root() {
    val ctx = LocalContext.current
    val player = remember { PlayerHolder(ctx.applicationContext) }
    val library = remember { MusicLibrary(ctx.applicationContext) }
    var screen by remember { mutableStateOf(Screen.HOME) }

    Row(Modifier.fillMaxSize().background(ZJ.bg)) {
        Rail(screen) { screen = it }
        Column(Modifier.weight(1f).fillMaxHeight().padding(horizontal = 32.dp, vertical = 24.dp)) {
            Header(screen)
            Spacer(Modifier.height(20.dp))
            Box(Modifier.weight(1f)) {
                when (screen) {
                    Screen.HOME -> HomeScreen(
                        onMap = { screen = Screen.MAP }, onMusic = { screen = Screen.MUSIC },
                        onApps = { screen = Screen.APPS }, onBt = { screen = Screen.BT }
                    )
                    Screen.MAP -> MapScreen()
                    Screen.MUSIC -> MusicScreen(player, library)
                    Screen.APPS -> AppsScreen()
                    Screen.BT -> BluetoothScreen()
                    Screen.SETTINGS -> SettingsScreen()
                }
            }
            if (screen != Screen.MUSIC) {
                Spacer(Modifier.height(20.dp))
                MiniPlayer(player) { screen = Screen.MUSIC }
            }
        }
    }
}

@Composable
private fun Rail(current: Screen, onSelect: (Screen) -> Unit) {
    Column(
        Modifier.width(120.dp).fillMaxHeight().background(ZJ.rail).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Screen.entries.filter { it != Screen.SETTINGS }.forEach { RailItem(it, it == current, onSelect) }
        Spacer(Modifier.weight(1f))
        RailItem(Screen.SETTINGS, current == Screen.SETTINGS, onSelect)
    }
}

@Composable
private fun RailItem(s: Screen, selected: Boolean, onSelect: (Screen) -> Unit) {
    Box(
        Modifier.size(88.dp).clip(RoundedCornerShape(26.dp))
            .background(if (selected) ZJ.accent else ZJ.rail)
            .clickable { onSelect(s) },
        contentAlignment = Alignment.Center
    ) {
        Text(
            s.label, fontSize = 14.sp, fontWeight = FontWeight.Bold,
            color = if (selected) ZJ.bg else ZJ.muted
        )
    }
}

@Composable
private fun Header(screen: Screen) {
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) { while (true) { now = Date(); delay(10_000) } }
    Row(Modifier.fillMaxWidth().height(72.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                if (screen == Screen.HOME) "Grand Cherokee ZJ" else screen.label,
                color = ZJ.text, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold
            )
            Text(
                SimpleDateFormat("EEEE d 'de' MMMM", Locale("es", "VE")).format(now),
                color = ZJ.muted, fontSize = 16.sp, fontWeight = FontWeight.SemiBold
            )
        }
        Text(
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(now),
            color = ZJ.accent, fontSize = 60.sp, fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun MiniPlayer(player: PlayerHolder, onOpen: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(96.dp)
            .clip(RoundedCornerShape(28.dp)).background(ZJ.card)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(Modifier.weight(1f).clickable(onClick = onOpen)) {
            Text(player.title, color = ZJ.text, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
            Text(player.subtitle, color = ZJ.muted, fontSize = 16.sp, maxLines = 1)
        }
        RoundBtn("⏮", 64.dp, false) { player.previous() }
        RoundBtn(if (player.isPlaying) "⏸" else "▶", 72.dp, true) { player.toggle() }
        RoundBtn("⏭", 64.dp, false) { player.next() }
    }
}
