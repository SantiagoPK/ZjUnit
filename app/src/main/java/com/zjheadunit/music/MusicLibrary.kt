package com.zjheadunit.music

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.documentfile.provider.DocumentFile
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Carpetas de música elegidas por el usuario. Cada carpeta se guarda como texto:
 * - una ruta de archivo (/storage/emulated/0/Music) elegida con el explorador de la app, o
 * - una URI "content://" elegida con el selector del sistema (respaldo).
 */
class MusicLibrary(private val ctx: Context) {
    private val prefs = ctx.getSharedPreferences("music", Context.MODE_PRIVATE)

    fun folders(): List<String> =
        (prefs.getStringSet("folders", emptySet()) ?: emptySet()).sorted()

    private fun save(list: Collection<String>) =
        prefs.edit().putStringSet("folders", list.toSet()).apply()

    fun addPath(path: String) = save(folders() + path)

    fun addTree(uri: Uri) {
        ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        save(folders() + uri.toString())
    }

    fun remove(entry: String) {
        if (entry.startsWith("content://")) runCatching {
            ctx.contentResolver.releasePersistableUriPermission(Uri.parse(entry), Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        save(folders() - entry)
    }

    fun displayName(entry: String): String =
        if (entry.startsWith("content://"))
            DocumentFile.fromTreeUri(ctx, Uri.parse(entry))?.name ?: "Carpeta"
        else File(entry).name.ifBlank { entry }

    suspend fun scan(): List<MediaItem> = withContext(Dispatchers.IO) {
        val out = mutableListOf<MediaItem>()
        folders().forEach { entry ->
            if (entry.startsWith("content://")) {
                DocumentFile.fromTreeUri(ctx, Uri.parse(entry))?.let { collectDoc(it, out) }
            } else {
                File(entry).walkTopDown()
                    .onEnter { !it.name.startsWith(".") }
                    .filter { it.isFile && AUDIO_EXT.matches(it.name) }
                    .forEach { f -> out += item(Uri.fromFile(f), f.nameWithoutExtension, f.parentFile?.name) }
            }
        }
        out.sortedBy { it.mediaMetadata.title?.toString()?.lowercase() }
    }

    private fun collectDoc(dir: DocumentFile, out: MutableList<MediaItem>) {
        dir.listFiles().forEach { f ->
            when {
                f.isDirectory -> collectDoc(f, out)
                f.type?.startsWith("audio/") == true || f.name?.matches(AUDIO_EXT) == true ->
                    out += item(f.uri, f.name?.substringBeforeLast('.') ?: "Pista", dir.name)
            }
        }
    }

    private fun item(uri: Uri, title: String, artist: String?) = MediaItem.Builder()
        .setUri(uri)
        .setMediaMetadata(MediaMetadata.Builder().setTitle(title).setArtist(artist).build())
        .build()

    /** ¿Puede la app leer el almacenamiento con rutas de archivo? */
    fun hasAllFilesAccess(): Boolean =
        if (android.os.Build.VERSION.SDK_INT >= 30) Environment.isExternalStorageManager() else true

    /** Raíces disponibles: almacenamiento interno y tarjetas SD. */
    fun roots(): List<Pair<String, File>> {
        val list = mutableListOf<Pair<String, File>>()
        list += "Interno" to Environment.getExternalStorageDirectory()
        ctx.getExternalFilesDirs(null).drop(1).filterNotNull().forEach { d ->
            val root = d.absolutePath.substringBefore("/Android/data")
            if (root.isNotBlank() && File(root).exists()) list += "Tarjeta SD" to File(root)
        }
        File("/storage").listFiles()?.forEach { f ->
            if (f.name != "emulated" && f.name != "self" && f.isDirectory && list.none { it.second.path == f.path })
                list += "Tarjeta SD" to f
        }
        return list
    }

    companion object {
        private val AUDIO_EXT = Regex(".*\\.(mp3|m4a|flac|ogg|opus|wav|aac)$", RegexOption.IGNORE_CASE)
    }
}
