package com.example.data.model

import android.net.Uri
import java.io.File

data class RecordedVideo(
    val id: Long,
    val uri: Uri,
    val file: File?,
    val displayName: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val dateAddedMs: Long,
    val width: Int = 0,
    val height: Int = 0
) {
    val formattedDuration: String
        get() {
            val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            val hours = minutes / 60
            return if (hours > 0) {
                String.format("%02d:%02d:%02d", hours, minutes % 60, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
        }

    val formattedSize: String
        get() {
            val mb = sizeBytes.toDouble() / (1024 * 1024)
            return if (mb >= 1024) {
                String.format("%.2f GB", mb / 1024)
            } else {
                String.format("%.1f MB", mb)
            }
        }

    val formattedDate: String
        get() {
            val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
            return sdf.format(java.util.Date(dateAddedMs))
        }
}
