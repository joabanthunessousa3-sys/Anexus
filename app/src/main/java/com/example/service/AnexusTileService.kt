package com.example.service

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.example.MainActivity
import com.example.data.model.RecordingStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AnexusTileService : TileService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var stateJob: Job? = null

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()

        stateJob?.cancel()
        stateJob = serviceScope.launch {
            ScreenCaptureService.recordingState.collectLatest {
                updateTileState()
            }
        }
    }

    override fun onStopListening() {
        super.onStopListening()
        stateJob?.cancel()
    }

    override fun onClick() {
        super.onClick()
        val snapshot = ScreenCaptureService.recordingState.value

        if (snapshot.isRecordingActive) {
            // If already recording, stop recording
            val stopIntent = Intent(this, ScreenCaptureService::class.java).apply {
                action = ScreenCaptureService.ACTION_STOP
            }
            startService(stopIntent)
            updateTileState()
        } else {
            // Need user MediaProjection permission -> launch MainActivity
            launchAppToRecord()
        }
    }

    private fun launchAppToRecord() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_TRIGGER_RECORD, true)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val snapshot = ScreenCaptureService.recordingState.value

        if (snapshot.isRecordingActive) {
            tile.state = Tile.STATE_ACTIVE
            tile.label = "Gravando"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = if (snapshot.status == RecordingStatus.PAUSED) "Pausado" else snapshot.formattedElapsedTime
            }
        } else {
            tile.state = Tile.STATE_INACTIVE
            tile.label = "Anexus Gravador"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = "Toque para gravar"
            }
        }
        tile.updateTile()
    }
}
