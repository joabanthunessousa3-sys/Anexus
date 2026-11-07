package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.AudioSourceOption
import com.example.data.model.RecordingConfig
import com.example.data.model.RecordingSnapshot
import com.example.data.model.RecordingStatus
import com.example.data.model.ResolutionOption
import com.example.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScreenCaptureService : Service() {

    companion object {
        const val TAG = "AnexusScreenService"
        const val CHANNEL_ID = "anexus_recording_channel"
        const val NOTIFICATION_ID = 8801
        const val SAVED_CHANNEL_ID = "anexus_saved_videos_channel"
        const val SAVED_NOTIFICATION_ID = 8802

        const val ACTION_START = "com.example.anexus.ACTION_START"
        const val ACTION_STOP = "com.example.anexus.ACTION_STOP"
        const val ACTION_PAUSE = "com.example.anexus.ACTION_PAUSE"
        const val ACTION_RESUME = "com.example.anexus.ACTION_RESUME"

        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"

        private val _recordingState = MutableStateFlow(RecordingSnapshot())
        val recordingState = _recordingState.asStateFlow()

        fun updateState(transform: (RecordingSnapshot) -> RecordingSnapshot) {
            _recordingState.value = transform(_recordingState.value)
        }
    }

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var mediaRecorder: MediaRecorder? = null

    private var currentOutputFile: File? = null
    private var currentConfig: RecordingConfig = RecordingConfig()

    private var timerJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    private var screenOffReceiver: BroadcastReceiver? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        registerScreenOffReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY

        when (action) {
            ACTION_START -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
                val resultData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_RESULT_DATA)
                }

                if (resultCode != 0 && resultData != null) {
                    val settingsRepo = SettingsRepository(this)
                    currentConfig = settingsRepo.getConfig()
                    startRecordingSession(resultCode, resultData)
                } else {
                    stopSelf()
                }
            }
            ACTION_PAUSE -> pauseRecording()
            ACTION_RESUME -> resumeRecording()
            ACTION_STOP -> stopRecordingSession()
        }

        return START_NOT_STICKY
    }

    private fun startRecordingSession(resultCode: Int, resultData: Intent) {
        val notification = buildNotification("Iniciando gravação...", 0, false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            var serviceType = ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            if (currentConfig.audioSource != AudioSourceOption.NONE &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
            ) {
                serviceType = serviceType or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            }
            startForeground(NOTIFICATION_ID, notification, serviceType)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        try {
            val mpManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val projection = mpManager.getMediaProjection(resultCode, resultData)
                ?: throw IllegalStateException("MediaProjection is null")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // Android 14+ requires registering callback before createVirtualDisplay
                projection.registerCallback(object : MediaProjection.Callback() {
                    override fun onStop() {
                        super.onStop()
                        Log.d(TAG, "MediaProjection stopped by system")
                        stopRecordingSession()
                    }
                }, Handler(Looper.getMainLooper()))
            }

            mediaProjection = projection

            setupMediaRecorder()
            setupVirtualDisplay()

            mediaRecorder?.start()

            vibrateDevice()

            _recordingState.value = RecordingSnapshot(
                status = RecordingStatus.RECORDING,
                elapsedSeconds = 0,
                outputPath = currentOutputFile?.absolutePath
            )

            startTimer()

            // If floating bubble is enabled, start floating bubble service
            if (currentConfig.showFloatingBubble) {
                val bubbleIntent = Intent(this, FloatingBubbleService::class.java).apply {
                    action = FloatingBubbleService.ACTION_SHOW
                }
                startService(bubbleIntent)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start screen recording", e)
            _recordingState.value = RecordingSnapshot(
                status = RecordingStatus.IDLE,
                errorMessage = "Erro ao iniciar gravação: ${e.localizedMessage}"
            )
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun setupMediaRecorder() {
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(metrics)

        var screenWidth = metrics.widthPixels
        var screenHeight = metrics.heightPixels
        val densityDpi = metrics.densityDpi

        // Calculate recording dimensions according to config
        val (targetWidth, targetHeight) = calculateDimensions(screenWidth, screenHeight, currentConfig.resolution)

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "Anexus_$timestamp.mp4"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: cacheDir
        if (!storageDir.exists()) storageDir.mkdirs()
        currentOutputFile = File(storageDir, fileName)

        @Suppress("DEPRECATION")
        val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            MediaRecorder()
        }

        if (currentConfig.audioSource != AudioSourceOption.NONE) {
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        }
        recorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)

        if (currentConfig.audioSource != AudioSourceOption.NONE) {
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setAudioSamplingRate(44100)
            recorder.setAudioEncodingBitRate(128000)
        }

        recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        recorder.setVideoSize(targetWidth, targetHeight)
        recorder.setVideoFrameRate(currentConfig.frameRate)
        recorder.setVideoEncodingBitRate(currentConfig.bitRateBps)
        recorder.setOutputFile(currentOutputFile!!.absolutePath)

        recorder.prepare()
        mediaRecorder = recorder
    }

    private fun calculateDimensions(screenWidth: Int, screenHeight: Int, option: ResolutionOption): Pair<Int, Int> {
        val isPortrait = screenHeight >= screenWidth
        if (option.isAuto) {
            // Keep native resolution, ensure even numbers
            return Pair(makeEven(screenWidth), makeEven(screenHeight))
        }

        val targetShortSide = option.height // e.g. 1080, 720, 480
        val aspect = if (isPortrait) {
            screenHeight.toFloat() / screenWidth.toFloat()
        } else {
            screenWidth.toFloat() / screenHeight.toFloat()
        }

        val targetLongSide = (targetShortSide * aspect).toInt()

        return if (isPortrait) {
            Pair(makeEven(targetShortSide), makeEven(targetLongSide))
        } else {
            Pair(makeEven(targetLongSide), makeEven(targetShortSide))
        }
    }

    private fun makeEven(value: Int): Int = if (value % 2 != 0) value - 1 else value

    private fun setupVirtualDisplay() {
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(metrics)

        val (targetWidth, targetHeight) = calculateDimensions(metrics.widthPixels, metrics.heightPixels, currentConfig.resolution)

        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "Anexus_VirtualDisplay",
            targetWidth,
            targetHeight,
            metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            mediaRecorder?.surface,
            null,
            null
        )
    }

    private fun pauseRecording() {
        if (_recordingState.value.status == RecordingStatus.RECORDING) {
            try {
                mediaRecorder?.pause()
                timerJob?.cancel()
                _recordingState.value = _recordingState.value.copy(status = RecordingStatus.PAUSED)
                updateNotification()
                vibrateDevice()
            } catch (e: Exception) {
                Log.e(TAG, "Error pausing recording", e)
            }
        }
    }

    private fun resumeRecording() {
        if (_recordingState.value.status == RecordingStatus.PAUSED) {
            try {
                mediaRecorder?.resume()
                _recordingState.value = _recordingState.value.copy(status = RecordingStatus.RECORDING)
                startTimer()
                updateNotification()
                vibrateDevice()
            } catch (e: Exception) {
                Log.e(TAG, "Error resuming recording", e)
            }
        }
    }

    private fun stopRecordingSession() {
        if (_recordingState.value.status == RecordingStatus.IDLE || _recordingState.value.status == RecordingStatus.STOPPING) {
            return
        }

        _recordingState.value = _recordingState.value.copy(status = RecordingStatus.STOPPING)
        timerJob?.cancel()

        try {
            mediaRecorder?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping mediaRecorder", e)
        } finally {
            mediaRecorder?.reset()
            mediaRecorder?.release()
            mediaRecorder = null
        }

        try {
            virtualDisplay?.release()
            virtualDisplay = null
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing virtualDisplay", e)
        }

        try {
            mediaProjection?.stop()
            mediaProjection = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping mediaProjection", e)
        }

        // Hide floating bubble
        val bubbleIntent = Intent(this, FloatingBubbleService::class.java).apply {
            action = FloatingBubbleService.ACTION_HIDE
        }
        startService(bubbleIntent)

        vibrateDevice()

        // Save into MediaStore
        var finalUriString: String? = null
        currentOutputFile?.let { file ->
            if (file.exists() && file.length() > 0) {
                finalUriString = saveVideoToMediaStore(file)
                showSavedNotification(file.name, finalUriString)
            }
        }

        _recordingState.value = RecordingSnapshot(
            status = RecordingStatus.IDLE,
            elapsedSeconds = 0,
            outputPath = currentOutputFile?.absolutePath,
            lastRecordedUri = finalUriString
        )

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun saveVideoToMediaStore(sourceFile: File): String? {
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, sourceFile.name)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
            put(MediaStore.Video.Media.DATE_MODIFIED, System.currentTimeMillis() / 1000)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/Anexus")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }

        val uri: Uri? = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)

        if (uri != null) {
            try {
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    FileInputStream(sourceFile).use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear()
                    values.put(MediaStore.Video.Media.IS_PENDING, 0)
                    contentResolver.update(uri, values, null, null)
                }
                return uri.toString()
            } catch (e: Exception) {
                Log.e(TAG, "Error copying video to MediaStore", e)
            }
        }
        return null
    }

    private fun showSavedNotification(fileName: String, uriString: String?) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                SAVED_CHANNEL_ID,
                "Gravações Salvas",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificações quando uma gravação é salva na galeria do celular"
                enableVibration(true)
            }
            nm.createNotificationChannel(channel)
        }

        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_TAB, 1)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            SAVED_NOTIFICATION_ID,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, SAVED_CHANNEL_ID)
            .setContentTitle("🎬 Gravação Salva na Galeria!")
            .setContentText("O vídeo foi salvo com sucesso na galeria do celular.")
            .setStyle(NotificationCompat.BigTextStyle().bigText("O vídeo '$fileName' foi salvo com sucesso na galeria do celular (Movies/Anexus). Toque aqui para assistir."))
            .setSmallIcon(R.drawable.ic_tile_record)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        if (uriString != null) {
            val playIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse(uriString), "video/mp4")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val playPendingIntent = PendingIntent.getActivity(
                this,
                SAVED_NOTIFICATION_ID + 1,
                playIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(0, "Assistir Vídeo", playPendingIntent)
        }

        nm.notify(SAVED_NOTIFICATION_ID, builder.build())

        Handler(Looper.getMainLooper()).post {
            Toast.makeText(applicationContext, "Gravação salva na galeria do celular!", Toast.LENGTH_LONG).show()
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (isActive && _recordingState.value.status == RecordingStatus.RECORDING) {
                delay(1000L)
                val currentSec = _recordingState.value.elapsedSeconds + 1
                _recordingState.value = _recordingState.value.copy(elapsedSeconds = currentSec)
                updateNotification()
            }
        }
    }

    private fun updateNotification() {
        val snapshot = _recordingState.value
        val title = if (snapshot.status == RecordingStatus.PAUSED) "Gravação Pausada" else "Gravando Tela — Anexus"
        val notification = buildNotification(title, snapshot.elapsedSeconds, snapshot.status == RecordingStatus.PAUSED)
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(title: String, elapsedSeconds: Long, isPaused: Boolean): Notification {
        val hours = elapsedSeconds / 3600
        val minutes = (elapsedSeconds % 3600) / 60
        val seconds = elapsedSeconds % 60
        val timeString = String.format("%02d:%02d:%02d", hours, minutes, seconds)

        val mainIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val mainPendingIntent = PendingIntent.getActivity(
            this,
            0,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, ScreenCaptureService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val toggleIntent = Intent(this, ScreenCaptureService::class.java).apply {
            action = if (isPaused) ACTION_RESUME else ACTION_PAUSE
        }
        val togglePendingIntent = PendingIntent.getService(
            this,
            2,
            toggleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val toggleActionTitle = if (isPaused) "Continuar" else "Pausar"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText("Duração: $timeString")
            .setSmallIcon(R.drawable.ic_tile_record)
            .setContentIntent(mainPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(0, toggleActionTitle, togglePendingIntent)
            .addAction(0, "Finalizar", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun registerScreenOffReceiver() {
        val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        screenOffReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_SCREEN_OFF && currentConfig.stopOnScreenOff) {
                    Log.d(TAG, "Screen turned off. Stopping recording session.")
                    stopRecordingSession()
                }
            }
        }
        registerReceiver(screenOffReceiver, filter)
    }

    private fun vibrateDevice() {
        if (!currentConfig.vibrateOnAction) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator.vibrate(VibrationEffect.createOneShot(70, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val v = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(70, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    v.vibrate(70)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            if (screenOffReceiver != null) {
                unregisterReceiver(screenOffReceiver)
                screenOffReceiver = null
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        stopRecordingSession()
    }
}
