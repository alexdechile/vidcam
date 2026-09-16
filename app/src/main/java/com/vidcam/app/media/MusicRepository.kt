package com.vidcam.app.media

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.vidcam.app.model.MusicTrack

class MusicRepository(private val context: Context) {

    fun loadTracks(): List<MusicTrack> {
        val tracks = mutableListOf<MusicTrack>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"
        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            sortOrder,
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                tracks += MusicTrack(
                    id = id,
                    uri = uri.toString(),
                    title = cursor.getString(titleColumn) ?: "Sin título",
                    artist = cursor.getString(artistColumn)
                        ?.takeIf { it.isNotBlank() && it != "<unknown>" }
                        ?: "Artista desconocido",
                    durationMs = cursor.getLong(durationColumn),
                )
            }
        }
        return tracks
    }
}
