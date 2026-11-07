package com.example

import com.example.data.model.AudioSourceOption
import com.example.data.model.RecordedVideo
import com.example.data.model.RecordingConfig
import com.example.data.model.RecordingSnapshot
import com.example.data.model.RecordingStatus
import com.example.data.model.ResolutionOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleUnitTest {
    @Test
    fun testDefaultRecordingConfig() {
        val config = RecordingConfig()
        assertEquals(ResolutionOption.P1080, config.resolution)
        assertEquals(60, config.frameRate)
        assertEquals(12_000_000, config.bitRateBps)
        assertEquals(AudioSourceOption.MIC, config.audioSource)
        assertTrue(config.showFloatingBubble)
        assertTrue(config.secureInvisibleBubble)
    }

    @Test
    fun testRecordedVideoFormatting() {
        val video = RecordedVideo(
            id = 1L,
            uri = android.net.Uri.EMPTY,
            file = null,
            displayName = "Anexus_Test.mp4",
            durationMs = 125_000L, // 2 minutes 5 seconds
            sizeBytes = 15 * 1024 * 1024L, // 15 MB
            dateAddedMs = 1690000000000L
        )

        assertEquals("02:05", video.formattedDuration)
        assertEquals("15.0 MB", video.formattedSize)
    }

    @Test
    fun testRecordingSnapshotStatus() {
        val idleSnapshot = RecordingSnapshot(status = RecordingStatus.IDLE)
        assertFalse(idleSnapshot.isRecordingActive)

        val recordingSnapshot = RecordingSnapshot(
            status = RecordingStatus.RECORDING,
            elapsedSeconds = 65
        )
        assertTrue(recordingSnapshot.isRecordingActive)
        assertEquals("00:01:05", recordingSnapshot.formattedElapsedTime)
    }
}
