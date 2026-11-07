package com.example.data.model

enum class RecordingStatus {
    IDLE,
    COUNTDOWN,
    RECORDING,
    PAUSED,
    STOPPING
}

data class RecordingSnapshot(
    val status: RecordingStatus = RecordingStatus.IDLE,
    val elapsedSeconds: Long = 0,
    val countdownRemaining: Int = 0,
    val outputPath: String? = null,
    val lastRecordedUri: String? = null,
    val errorMessage: String? = null
) {
    val isRecordingActive: Boolean
        get() = status == RecordingStatus.RECORDING || status == RecordingStatus.PAUSED

    val formattedElapsedTime: String
        get() {
            val hours = elapsedSeconds / 3600
            val minutes = (elapsedSeconds % 3600) / 60
            val seconds = elapsedSeconds % 60
            return String.format("%02d:%02d:%02d", hours, minutes, seconds)
        }
}
