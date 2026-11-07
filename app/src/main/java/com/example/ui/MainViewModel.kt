package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AudioSourceOption
import com.example.data.model.RecordedVideo
import com.example.data.model.RecordingConfig
import com.example.data.model.RecordingSnapshot
import com.example.data.model.RecordingStatus
import com.example.data.model.ResolutionOption
import com.example.data.repository.SettingsRepository
import com.example.data.repository.VideoRepository
import com.example.service.ScreenCaptureService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)
    private val videoRepository = VideoRepository(application)

    val recordingState: StateFlow<RecordingSnapshot> = ScreenCaptureService.recordingState

    val config: StateFlow<RecordingConfig> = settingsRepository.configFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, settingsRepository.getConfig())

    private val _videos = MutableStateFlow<List<RecordedVideo>>(emptyList())
    val videos: StateFlow<List<RecordedVideo>> = _videos.asStateFlow()

    private val _isLoadingVideos = MutableStateFlow(false)
    val isLoadingVideos: StateFlow<Boolean> = _isLoadingVideos.asStateFlow()

    private val _currentTab = MutableStateFlow(0) // 0: Record, 1: Gallery, 2: Settings
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    private val _countdown = MutableStateFlow<Int?>(null)
    val countdown: StateFlow<Int?> = _countdown.asStateFlow()

    private var countdownJob: Job? = null

    // Pending projection intent while countdown runs
    private var pendingProjectionData: Pair<Int, Intent>? = null

    init {
        loadVideos()
        // Refresh video list when recording finishes
        viewModelScope.launch {
            ScreenCaptureService.recordingState.collect { snapshot ->
                if (snapshot.status == RecordingStatus.IDLE && snapshot.lastRecordedUri != null) {
                    loadVideos()
                }
            }
        }
    }

    fun setTab(tabIndex: Int) {
        _currentTab.value = tabIndex
        if (tabIndex == 1) {
            loadVideos()
        }
    }

    fun loadVideos() {
        viewModelScope.launch {
            _isLoadingVideos.value = true
            try {
                _videos.value = videoRepository.getRecordedVideos()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoadingVideos.value = false
            }
        }
    }

    fun onMediaProjectionGranted(resultCode: Int, data: Intent) {
        showFloatingControlsStandby(resultCode, data)
    }

    fun showFloatingControlsStandby(resultCode: Int, data: Intent) {
        val context = getApplication<Application>()
        val bubbleIntent = Intent(context, com.example.service.FloatingBubbleService::class.java).apply {
            action = com.example.service.FloatingBubbleService.ACTION_SHOW_STANDBY
            putExtra(com.example.service.FloatingBubbleService.EXTRA_RESULT_CODE, resultCode)
            putExtra(com.example.service.FloatingBubbleService.EXTRA_RESULT_DATA, data)
        }
        context.startService(bubbleIntent)
    }

    private fun startCountdown(seconds: Int) {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            for (i in seconds downTo 1) {
                _countdown.value = i
                delay(1000L)
            }
            _countdown.value = null
            pendingProjectionData?.let { (code, data) ->
                launchScreenService(code, data)
            }
            pendingProjectionData = null
        }
    }

    fun cancelCountdown() {
        countdownJob?.cancel()
        _countdown.value = null
        pendingProjectionData = null
    }

    private fun launchScreenService(resultCode: Int, data: Intent) {
        val context = getApplication<Application>()
        val serviceIntent = Intent(context, ScreenCaptureService::class.java).apply {
            action = ScreenCaptureService.ACTION_START
            putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, resultCode)
            putExtra(ScreenCaptureService.EXTRA_RESULT_DATA, data)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }

    fun pauseRecording() {
        sendServiceAction(ScreenCaptureService.ACTION_PAUSE)
    }

    fun resumeRecording() {
        sendServiceAction(ScreenCaptureService.ACTION_RESUME)
    }

    fun stopRecording() {
        sendServiceAction(ScreenCaptureService.ACTION_STOP)
    }

    private fun sendServiceAction(action: String) {
        val context = getApplication<Application>()
        val intent = Intent(context, ScreenCaptureService::class.java).apply {
            this.action = action
        }
        context.startService(intent)
    }

    fun updateConfig(update: (RecordingConfig) -> RecordingConfig) {
        settingsRepository.updateConfig(update)
    }

    fun deleteVideo(video: RecordedVideo) {
        viewModelScope.launch {
            videoRepository.deleteVideo(video)
            loadVideos()
        }
    }

    fun renameVideo(video: RecordedVideo, newName: String) {
        viewModelScope.launch {
            videoRepository.renameVideo(video, newName)
            loadVideos()
        }
    }

    fun hasOverlayPermission(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }
}
