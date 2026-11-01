package com.zjheadunit.nav

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class Place(val name: String, val lat: Double, val lon: Double)
data class Step(val text: String, val meters: Double, val lat: Double, val lon: Double)
data class Route(
    val points: List<Pair<Double, Double>>, // lon, lat
    val steps: List<Step>,
    val meters: Double,
    val seconds: Double
)

/**
 * Búsqueda (Nominatim) y rutas (OSRM) sobre datos de OpenStreetMap.
 *
 * IMPORTANTE: los servidores públicos son solo para pruebas y uso ligero.
 * Para uso diario conviene alojar tu propio Valhalla/OSRM/GraphHopper o
 * pasar a navegación sin internet. Las URLs son configurables aquí.
 */
object Routing {
    var nominatimUrl = "https://nominatim.openstreetmap.org"
    var osrmUrl = "https://router.project-osrm.org"
    private const val UA = "ZJHeadUnit/0.1 (uso personal)"

    private suspend fun get(url: String): String = withContext(Dispatchers.IO) {
        val c = URL(url).openConnection() as HttpURLConnection
        c.setRequestProperty("User-Agent", UA)
        c.connectTimeout = 10_000
        c.readTimeout = 15_000
        try { c.inputStream.bufferedReader().readText() } finally { c.disconnect() }
    }

    suspend fun search(query: String, nearLat: Double?, nearLon: Double?): List<Place> {
        val q = URLEncoder.encode(query, "UTF-8")
        var url = "$nominatimUrl/search?q=$q&format=json&limit=6&accept-language=es"
        if (nearLat != null && nearLon != null) {
            val d = 1.0 // sesgo hacia el área cercana
            url += "&viewbox=${nearLon - d},${nearLat + d},${nearLon + d},${nearLat - d}"
        }
        val arr = JSONArray(get(url))
        return (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            Place(o.getString("display_name"), o.getString("lat").toDouble(), o.getString("lon").toDouble())
        }
    }

    suspend fun route(fromLat: Double, fromLon: Double, to: Place): Route {
        val url = "$osrmUrl/route/v1/driving/$fromLon,$fromLat;${to.lon},${to.lat}" +
            "?overview=full&geometries=geojson&steps=true"
        val root = JSONObject(get(url))
        val r = root.getJSONArray("routes").getJSONObject(0)
        val coords = r.getJSONObject("geometry").getJSONArray("coordinates")
        val points = (0 until coords.length()).map {
            val p = coords.getJSONArray(it)
            p.getDouble(0) to p.getDouble(1)
        }
        val stepsJson = r.getJSONArray("legs").getJSONObject(0).getJSONArray("steps")
        val steps = (0 until stepsJson.length()).map {
            val s = stepsJson.getJSONObject(it)
            val m = s.getJSONObject("maneuver")
            val loc = m.getJSONArray("location")
            Step(
                instruction(m.getString("type"), m.optString("modifier"), s.optString("name")),
                s.getDouble("distance"),
                loc.getDouble(1), loc.getDouble(0)
            )
        }
        return Route(points, steps, r.getDouble("distance"), r.getDouble("duration"))
    }

    private fun instruction(type: String, mod: String, road: String): String {
        val onto = if (road.isNotBlank()) " hacia $road" else ""
        val dir = when (mod) {
            "left" -> "a la izquierda"
            "right" -> "a la derecha"
            "slight left" -> "ligeramente a la izquierda"
            "slight right" -> "ligeramente a la derecha"
            "sharp left" -> "cerrado a la izquierda"
            "sharp right" -> "cerrado a la derecha"
            "uturn" -> "para dar la vuelta"
            else -> "recto"
        }
        return when (type) {
            "depart" -> "Inicia la ruta$onto"
            "arrive" -> "Has llegado a tu destino"
            "roundabout", "rotary" -> "Entra en la rotonda y sal$onto"
            "continue", "new name" -> "Continúa $dir$onto"
            else -> "Gira $dir$onto"
        }
    }
}
