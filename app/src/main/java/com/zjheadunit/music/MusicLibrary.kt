package com.zjheadunit.music

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Carpetas de música elegidas por el usuario (selector de carpetas del sistema,
 * acceso persistente) y escaneo de pistas dentro de ellas.
 */
class MusicLibrary(private val ctx: Context) {
    private val prefs = ctx.getSharedPreferences("music", Context.MODE_PRIVATE)

    fun folders(): List<Uri> =
        (prefs.getStringSet("folders", emptySet()) ?: emptySet()).map { Uri.parse(it) }

    fun addFolder(uri: Uri) {
        ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        prefs.edit().putStringSet("folders", (folders() + uri).map { it.toString() }.toSet()).apply()
    }

    fun removeFolder(uri: Uri) {
        runCatching {
            ctx.contentResolver.releasePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        prefs.edit().putStringSet("folders", (folders() - uri).map { it.toString() }.toSet()).apply()
    }

    fun folderName(uri: Uri): String =
        DocumentFile.fromTreeUri(ctx, uri)?.name ?: uri.lastPathSegment ?: "Carpeta"

    suspend fun scan(): List<MediaItem> = withContext(Dispatchers.IO) {
        val out = mutableListOf<MediaItem>()
        folders().forEach { uri ->
            DocumentFile.fromTreeUri(ctx, uri)?.let { collect(it, out) }
        }
        out.sortedBy { it.mediaMetadata.title?.toString()?.lowercase() }
    }

    private fun collect(dir: DocumentFile, out: MutableList<MediaItem>) {
        dir.listFiles().forEach { f ->
            when {
                f.isDirectory -> collect(f, out)
                f.type?.startsWith("audio/") == true || f.name?.matches(AUDIO_EXT) == true ->
                    out += MediaItem.Builder()
                        .setUri(f.uri)
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle(f.name?.substringBeforeLast('.') ?: "Pista")
                                .setArtist(dir.name)
                                .build()
                        )
                        .build()
            }
        }
    }

    companion object {
        private val AUDIO_EXT = Regex(".*\\.(mp3|m4a|flac|ogg|opus|wav|aac)$", RegexOption.IGNORE_CASE)
    }
}
