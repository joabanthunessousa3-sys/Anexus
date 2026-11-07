package com.example.data.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.data.model.RecordedVideo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class VideoRepository(private val context: Context) {
    private val contentResolver: ContentResolver = context.contentResolver

    suspend fun getRecordedVideos(): List<RecordedVideo> = withContext(Dispatchers.IO) {
        val list = mutableListOf<RecordedVideo>()
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT
        )

        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        // Search for videos that match "Anexus" in display name or recorded in Anexus folder
        val selection = "${MediaStore.Video.Media.DISPLAY_NAME} LIKE ? OR ${MediaStore.Video.Media.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%Anexus%", "%anexus%")

        try {
            contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val durCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                val widthCol = cursor.getColumnIndex(MediaStore.Video.Media.WIDTH)
                val heightCol = cursor.getColumnIndex(MediaStore.Video.Media.HEIGHT)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol) ?: "Anexus_Video.mp4"
                    val duration = cursor.getLong(durCol)
                    val size = cursor.getLong(sizeCol)
                    val dateAddedSec = cursor.getLong(dateCol)
                    val width = if (widthCol != -1) cursor.getInt(widthCol) else 0
                    val height = if (heightCol != -1) cursor.getInt(heightCol) else 0

                    val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)

                    list.add(
                        RecordedVideo(
                            id = id,
                            uri = uri,
                            file = null,
                            displayName = name,
                            durationMs = duration,
                            sizeBytes = size,
                            dateAddedMs = dateAddedSec * 1000L,
                            width = width,
                            height = height
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Also check app-specific external movies directory if any was saved there
        val appMoviesDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        if (appMoviesDir != null && appMoviesDir.exists()) {
            val files = appMoviesDir.listFiles { f -> f.extension.equals("mp4", ignoreCase = true) }
            files?.forEach { f ->
                // Avoid duplicates if already queried
                if (list.none { it.displayName == f.name }) {
                    list.add(
                        RecordedVideo(
                            id = f.name.hashCode().toLong(),
                            uri = Uri.fromFile(f),
                            file = f,
                            displayName = f.name,
                            durationMs = 0L,
                            sizeBytes = f.length(),
                            dateAddedMs = f.lastModified()
                        )
                    )
                }
            }
        }

        list.sortedByDescending { it.dateAddedMs }
    }

    suspend fun deleteVideo(video: RecordedVideo): Boolean = withContext(Dispatchers.IO) {
        var success = false
        try {
            if (video.file != null && video.file.exists()) {
                success = video.file.delete()
            }
            val rows = contentResolver.delete(video.uri, null, null)
            success = success || (rows > 0)
        } catch (e: Exception) {
            e.printStackTrace()
            success = false
        }
        success
    }

    suspend fun renameVideo(video: RecordedVideo, newName: String): Boolean = withContext(Dispatchers.IO) {
        val cleanName = if (!newName.endsWith(".mp4", ignoreCase = true)) "$newName.mp4" else newName
        try {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, cleanName)
            }
            val rows = contentResolver.update(video.uri, values, null, null)
            if (rows > 0) return@withContext true

            if (video.file != null && video.file.exists()) {
                val newFile = File(video.file.parentFile, cleanName)
                return@withContext video.file.renameTo(newFile)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        false
    }
}
