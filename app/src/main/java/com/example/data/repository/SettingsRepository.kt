package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.AudioSourceOption
import com.example.data.model.OrientationOption
import com.example.data.model.RecordingConfig
import com.example.data.model.ResolutionOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("anexus_settings", Context.MODE_PRIVATE)

    private val _configFlow = MutableStateFlow(loadConfig())
    val configFlow: StateFlow<RecordingConfig> = _configFlow.asStateFlow()

    fun getConfig(): RecordingConfig = _configFlow.value

    fun updateConfig(update: (RecordingConfig) -> RecordingConfig) {
        val newConfig = update(_configFlow.value)
        _configFlow.value = newConfig
        saveConfig(newConfig)
    }

    private fun loadConfig(): RecordingConfig {
        val resolutionName = prefs.getString("resolution", ResolutionOption.P1080.name) ?: ResolutionOption.P1080.name
        val resolution = runCatching { ResolutionOption.valueOf(resolutionName) }.getOrDefault(ResolutionOption.P1080)

        val frameRate = prefs.getInt("frame_rate", 60)
        val bitRate = prefs.getInt("bit_rate", 12_000_000)

        val audioName = prefs.getString("audio_source", AudioSourceOption.MIC.name) ?: AudioSourceOption.MIC.name
        val audioSource = runCatching { AudioSourceOption.valueOf(audioName) }.getOrDefault(AudioSourceOption.MIC)

        val countdown = prefs.getInt("countdown_seconds", 3)
        val showBubble = prefs.getBoolean("show_floating_bubble", true)
        val secureBubble = prefs.getBoolean("secure_invisible_bubble", true)
        val stopScreenOff = prefs.getBoolean("stop_screen_off", true)
        val vibrate = prefs.getBoolean("vibrate_action", true)

        val orientationName = prefs.getString("orientation", OrientationOption.AUTO.name) ?: OrientationOption.AUTO.name
        val orientation = runCatching { OrientationOption.valueOf(orientationName) }.getOrDefault(OrientationOption.AUTO)

        return RecordingConfig(
            resolution = resolution,
            frameRate = frameRate,
            bitRateBps = bitRate,
            audioSource = audioSource,
            countdownSeconds = countdown,
            showFloatingBubble = showBubble,
            secureInvisibleBubble = secureBubble,
            stopOnScreenOff = stopScreenOff,
            vibrateOnAction = vibrate,
            orientation = orientation
        )
    }

    private fun saveConfig(config: RecordingConfig) {
        prefs.edit()
            .putString("resolution", config.resolution.name)
            .putInt("frame_rate", config.frameRate)
            .putInt("bit_rate", config.bitRateBps)
            .putString("audio_source", config.audioSource.name)
            .putInt("countdown_seconds", config.countdownSeconds)
            .putBoolean("show_floating_bubble", config.showFloatingBubble)
            .putBoolean("secure_invisible_bubble", config.secureInvisibleBubble)
            .putBoolean("stop_screen_off", config.stopOnScreenOff)
            .putBoolean("vibrate_action", config.vibrateOnAction)
            .putString("orientation", config.orientation.name)
            .apply()
    }
}
