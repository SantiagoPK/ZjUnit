package com.zjheadunit.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.zjheadunit.nav.Place
import com.zjheadunit.nav.Route
import com.zjheadunit.nav.Routing
import kotlinx.coroutines.launch
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory.*
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import kotlin.math.roundToInt

/** Estilo oscuro de OpenStreetMap vía teselas CARTO (con atribución). Cámbialo por tus propias teselas si quieres. */
private val DARK_STYLE = """
{
  "version": 8,
  "sources": { "osm": {
      "type": "raster",
      "tiles": ["https://a.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}@2x.png",
                "https://b.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}@2x.png"],
      "tileSize": 256,
      "attribution": "© OpenStreetMap contributors © CARTO" } },
  "layers": [ { "id": "osm", "type": "raster", "source": "osm" } ]
}
""".trimIndent()

@SuppressLint("MissingPermission")
@Composable
fun MapScreen() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val accentArgb = ZJ.accent.toArgb()

    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Place>>(emptyList()) }
    var route by remember { mutableStateOf<Route?>(null) }
    var stepIdx by remember { mutableIntStateOf(0) }
    var me by remember { mutableStateOf<Location?>(null) }
    var status by remember { mutableStateOf<String?>(null) }
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var follow by remember { mutableStateOf(true) }

    val tts = remember {
        var t: TextToSpeech? = null
        t = TextToSpeech(ctx) { if (it == TextToSpeech.SUCCESS) t?.language = java.util.Locale("es") }
        t
    }
    DisposableEffect(Unit) { onDispose { tts?.shutdown() } }

    val mapView = remember {
        MapLibre.getInstance(ctx)
        MapView(ctx)
    }

    // Ciclo de vida del MapView
    DisposableEffect(lifecycle) {
        val obs = LifecycleEventObserver { _, e ->
            when (e) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> {}
            }
        }
        lifecycle.addObserver(obs)
        mapView.onCreate(null)
        onDispose { lifecycle.removeObserver(obs); mapView.onDestroy() }
    }

    // GPS (sin Google Play Services)
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    DisposableEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) permLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val listener = LocationListener { me = it }
        if (granted) {
            runCatching {
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, listener)
                lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 3000L, 0f, listener)
                lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.let { me = it }
            }
        }
        onDispose { lm.removeUpdates(listener) }
    }

    // Mapa listo: estilo + capas de ruta y posición
    LaunchedEffect(Unit) {
        mapView.getMapAsync { m ->
            map = m
            m.setStyle(Style.Builder().fromJson(DARK_STYLE)) { style ->
                style.addSource(GeoJsonSource("route"))
                style.addLayer(
                    LineLayer("route-line", "route").withProperties(
                        lineColor(accentArgb), lineWidth(8f), lineCap("round"), lineJoin("round")
                    )
                )
                style.addSource(GeoJsonSource("me"))
                style.addLayer(
                    CircleLayer("me-dot", "me").withProperties(
                        circleRadius(9f), circleColor(0xFFFFFFFF.toInt()),
                        circleStrokeColor(accentArgb), circleStrokeWidth(4f)
                    )
                )
            }
            m.cameraPosition = CameraPosition.Builder().target(LatLng(10.48, -66.90)).zoom(11.0).build()
            m.addOnCameraMoveStartedListener { reason ->
                if (reason == MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE) follow = false
            }
        }
    }

    // Actualiza posición, cámara y paso actual
    LaunchedEffect(me, map) {
        val loc = me ?: return@LaunchedEffect
        val m = map ?: return@LaunchedEffect
        m.getStyle { s ->
            s.getSourceAs<GeoJsonSource>("me")?.setGeoJson(Feature.fromGeometry(Point.fromLngLat(loc.longitude, loc.latitude)))
        }
        if (follow) {
            m.animateCamera(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.Builder().target(LatLng(loc.latitude, loc.longitude))
                        .zoom(if (route != null) 16.0 else 14.0).bearing(loc.bearing.toDouble()).build()
                ), 800
            )
        }
        route?.let { r ->
            val next = r.steps.getOrNull(stepIdx + 1) ?: return@let
            val d = FloatArray(1)
            Location.distanceBetween(loc.latitude, loc.longitude, next.lat, next.lon, d)
            if (d[0] < 25f) {
                stepIdx += 1
                tts?.speak(r.steps[stepIdx].text, TextToSpeech.QUEUE_FLUSH, null, "step")
            }
        }
    }

    // Dibuja la ruta
    LaunchedEffect(route, map) {
        val m = map ?: return@LaunchedEffect
        m.getStyle { s ->
            val src = s.getSourceAs<GeoJsonSource>("route")
            val r = route
            if (r == null) src?.setGeoJson(Feature.fromGeometry(LineString.fromLngLats(emptyList())))
            else src?.setGeoJson(
                Feature.fromGeometry(LineString.fromLngLats(r.points.map { Point.fromLngLat(it.first, it.second) }))
            )
        }
    }

    fun doSearch() {
        if (query.isBlank()) return
        scope.launch {
            status = "Buscando…"
            runCatching { Routing.search(query, me?.latitude, me?.longitude) }
                .onSuccess { results = it; status = if (it.isEmpty()) "Sin resultados" else null }
                .onFailure { status = "Sin conexión o servicio no disponible" }
        }
    }

    fun go(p: Place) {
        val from = me
        if (from == null) { status = "Esperando señal GPS…"; return }
        scope.launch {
            status = "Calculando ruta…"
            runCatching { Routing.route(from.latitude, from.longitude, p) }
                .onSuccess {
                    route = it; stepIdx = 0; results = emptyList(); follow = true; status = null
                    tts?.speak(it.steps.firstOrNull()?.text ?: "", TextToSpeech.QUEUE_FLUSH, null, "start")
                }
                .onFailure { status = "No se pudo calcular la ruta" }
        }
    }

    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        // Panel izquierdo
        Column(
            Modifier.width(380.dp).fillMaxHeight()
                .background(ZJ.card, RoundedCornerShape(32.dp))
                .border(1.dp, ZJ.line, RoundedCornerShape(32.dp))
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            val r = route
            if (r != null) {
                val step = r.steps.getOrNull(stepIdx + 1) ?: r.steps.last()
                Text("SIGUIENTE", color = ZJ.muted, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(step.text, color = ZJ.text, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 33.sp)
                Text("en ${formatDist(step.meters)}", color = ZJ.accent, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Restante: ${formatDist(r.meters)} · ${(r.seconds / 60).roundToInt()} min",
                    color = ZJ.muted, fontSize = 17.sp, fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.weight(1f))
                BigButton("Terminar ruta", Modifier.fillMaxWidth(), filled = false) { route = null; stepIdx = 0 }
            } else {
                Text("Navegación", color = ZJ.text, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
                Box(
                    Modifier.fillMaxWidth().height(64.dp)
                        .background(ZJ.cardHi, RoundedCornerShape(20.dp))
                        .border(1.dp, ZJ.line2, RoundedCornerShape(20.dp))
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (query.isEmpty()) Text("Buscar destino", color = ZJ.muted, fontSize = 20.sp)
                    BasicTextField(
                        value = query, onValueChange = { query = it },
                        singleLine = true,
                        textStyle = TextStyle(color = ZJ.text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
                        cursorBrush = SolidColor(ZJ.accent),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { doSearch() }),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                BigButton("Buscar", Modifier.fillMaxWidth()) { doSearch() }
                status?.let { Text(it, color = ZJ.muted, fontSize = 16.sp) }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(results) { p ->
                        Text(
                            p.name, color = ZJ.text, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 2,
                            modifier = Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(ZJ.cardHi)
                                .clickable { go(p) }
                                .padding(16.dp)
                        )
                    }
                }
            }
        }

        // Mapa
        Box(
            Modifier.weight(1f).fillMaxHeight()
                .clip(RoundedCornerShape(32.dp))
                .border(1.dp, ZJ.line, RoundedCornerShape(32.dp))
        ) {
            AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
            if (!follow) {
                BigButton(
                    "Centrar", Modifier.align(Alignment.BottomEnd).padding(20.dp)
                ) { follow = true; me = me?.let { Location(it) } }
            }
        }
    }
}

private fun formatDist(m: Double) =
    if (m >= 1000) "%.1f km".format(m / 1000) else "${(m / 10).roundToInt() * 10} m"
