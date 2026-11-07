package com.example.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.example.MainActivity
import com.example.R
import com.example.data.model.RecordingSnapshot
import com.example.data.model.RecordingStatus
import com.example.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FloatingBubbleService : Service() {

    companion object {
        const val ACTION_SHOW = "com.example.anexus.ACTION_SHOW_BUBBLE"
        const val ACTION_SHOW_STANDBY = "com.example.anexus.ACTION_SHOW_STANDBY"
        const val ACTION_HIDE = "com.example.anexus.ACTION_HIDE_BUBBLE"

        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"

        // Cached projection token while in standby before user taps "Gravar"
        var pendingResultCode: Int = 0
        var pendingResultData: Intent? = null
    }

    private var windowManager: WindowManager? = null
    private var bubbleContainer: FrameLayout? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    private var isExpanded = false
    private var isStandby = true // True before user hits "Gravar" inside bubble
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var stateJob: Job? = null
    private var countdownJob: Job? = null

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    // UI elements inside bubble
    private var timerTextView: TextView? = null
    private var pauseResumeIcon: ImageView? = null
    private var pauseResumeLayout: LinearLayout? = null
    private var stopLayout: LinearLayout? = null
    private var startRecLayout: LinearLayout? = null
    private var expandedControlsLayout: LinearLayout? = null
    private var collapsedDot: View? = null
    private var mainPillLayout: LinearLayout? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        observeRecordingState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY

        when (action) {
            ACTION_SHOW_STANDBY -> {
                val code = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
                val data = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_RESULT_DATA)
                }

                if (code != 0 && data != null) {
                    pendingResultCode = code
                    pendingResultData = data
                    isStandby = true
                }

                if (Settings.canDrawOverlays(this)) {
                    showBubble()
                }
            }
            ACTION_SHOW -> {
                isStandby = false
                if (Settings.canDrawOverlays(this)) {
                    showBubble()
                }
            }
            ACTION_HIDE -> hideBubble()
        }

        return START_NOT_STICKY
    }

    private fun observeRecordingState() {
        stateJob?.cancel()
        stateJob = serviceScope.launch {
            ScreenCaptureService.recordingState.collectLatest { snapshot ->
                updateBubbleUi(snapshot)
            }
        }
    }

    private fun updateBubbleUi(snapshot: RecordingSnapshot) {
        val dpToPx = { dp: Int ->
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp.toFloat(),
                resources.displayMetrics
            ).toInt()
        }

        if (snapshot.isRecordingActive) {
            isStandby = false
            startRecLayout?.visibility = View.GONE
            pauseResumeLayout?.visibility = View.VISIBLE
            stopLayout?.visibility = View.VISIBLE

            timerTextView?.text = snapshot.formattedElapsedTime

            // A bolinha vermelha vira um QUADRADO VERMELHO que para a gravação
            collapsedDot?.background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(3).toFloat()
                setColor(Color.parseColor("#EF4444")) // Quadrado vermelho
            }

            // O botão de pause usa o triângulo vermelho
            pauseResumeIcon?.setImageResource(R.drawable.ic_pause_red_triangle)
            pauseResumeIcon?.setColorFilter(Color.parseColor("#EF4444"))

        } else if (snapshot.status == RecordingStatus.STOPPING) {
            hideBubble()
        } else if (!isStandby && snapshot.status == RecordingStatus.IDLE) {
            hideBubble()
        }
    }

    private fun updateBubbleMode() {
        val dpToPx = { dp: Int ->
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp.toFloat(),
                resources.displayMetrics
            ).toInt()
        }

        if (isStandby) {
            timerTextView?.text = "PRONTO (REC)"
            // Bolinha vermelha antes de iniciar
            collapsedDot?.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#EF4444")) // Bolinha vermelha
            }
            startRecLayout?.visibility = View.VISIBLE
            pauseResumeLayout?.visibility = View.GONE
            stopLayout?.visibility = View.GONE
        } else {
            // Quadrado vermelho após iniciar a gravação
            collapsedDot?.background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(3).toFloat()
                setColor(Color.parseColor("#EF4444")) // Quadrado vermelho
            }
            startRecLayout?.visibility = View.GONE
            pauseResumeLayout?.visibility = View.VISIBLE
            stopLayout?.visibility = View.VISIBLE
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showBubble() {
        if (bubbleContainer != null) {
            updateBubbleMode()
            return
        }

        val settingsRepo = SettingsRepository(this)
        val config = settingsRepo.getConfig()

        val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        var flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

        // FLAG_SECURE prevents window from being captured in the recorded video MP4
        if (config.secureInvisibleBubble) {
            flags = flags or WindowManager.LayoutParams.FLAG_SECURE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            windowType,
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 30
            y = 350
        }

        layoutParams = params
        val root = createBubbleView()
        bubbleContainer = root

        root.setOnTouchListener { _, event ->
            handleTouch(event)
        }

        try {
            windowManager?.addView(root, params)
            updateBubbleMode()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleTouch(event: MotionEvent): Boolean {
        val params = layoutParams ?: return false
        val root = bubbleContainer ?: return false

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = params.x
                initialY = params.y
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = (event.rawX - initialTouchX).toInt()
                val dy = (event.rawY - initialTouchY).toInt()
                params.x = initialX + dx
                params.y = initialY + dy
                try {
                    windowManager?.updateViewLayout(root, params)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                val diffX = Math.abs(event.rawX - initialTouchX)
                val diffY = Math.abs(event.rawY - initialTouchY)
                if (diffX < 15 && diffY < 15) {
                    // Tap detected -> toggle expanded controls
                    toggleExpanded()
                }
                return true
            }
        }
        return false
    }

    private fun toggleExpanded() {
        isExpanded = !isExpanded
        expandedControlsLayout?.visibility = if (isExpanded) View.VISIBLE else View.GONE
        try {
            windowManager?.updateViewLayout(bubbleContainer, layoutParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createBubbleView(): FrameLayout {
        val dpToPx = { dp: Int ->
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp.toFloat(),
                resources.displayMetrics
            ).toInt()
        }

        val container = FrameLayout(this)

        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(12), dpToPx(9), dpToPx(12), dpToPx(9))

            val bg = GradientDrawable().apply {
                setColor(Color.parseColor("#EE0B0D15")) // 93% opacity deep void dark
                cornerRadius = dpToPx(28).toFloat()
                setStroke(dpToPx(1), Color.parseColor("#558B5CF6")) // subtle neon purple glow
            }
            background = bg
            elevation = dpToPx(10).toFloat()
        }
        mainPillLayout = mainLayout

        // Status indicator dot
        collapsedDot = View(this).apply {
            val dotBg = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#8B5CF6"))
            }
            background = dotBg
            val dotSize = dpToPx(11)
            layoutParams = LinearLayout.LayoutParams(dotSize, dotSize).apply {
                marginEnd = dpToPx(8)
            }
        }
        mainLayout.addView(collapsedDot)

        // Status / Timer text
        timerTextView = TextView(this).apply {
            text = if (isStandby) "PRONTO (REC)" else "00:00:00"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTypeface(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = dpToPx(8)
            }
        }
        mainLayout.addView(timerTextView)

        // Expanded controls container
        expandedControlsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            visibility = View.GONE
        }

        // START RECORD BUTTON: Bolinha Vermelha
        startRecLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(10), dpToPx(5), dpToPx(12), dpToPx(5))
            val btnBg = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#EF4444")) // Bolinha vermelha
            }
            background = btnBg
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dpToPx(36)
            ).apply {
                marginEnd = dpToPx(8)
            }

            val redCircleView = View(context).apply {
                val circle = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.WHITE)
                }
                background = circle
                val s = dpToPx(10)
                layoutParams = LinearLayout.LayoutParams(s, s).apply { marginEnd = dpToPx(6) }
            }
            addView(redCircleView)

            val recLabel = TextView(context).apply {
                text = "GRAVAR"
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            addView(recLabel)

            setOnClickListener {
                startRecordingFromBubble()
            }
        }
        expandedControlsLayout?.addView(startRecLayout)

        // PAUSE / RESUME BUTTON: Triângulo Vermelho
        pauseResumeLayout = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            val btnBg = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#1B2032"))
                setStroke(dpToPx(1), Color.parseColor("#EF4444"))
            }
            background = btnBg
            val size = dpToPx(36)
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                marginEnd = dpToPx(8)
            }

            pauseResumeIcon = ImageView(context).apply {
                setImageResource(R.drawable.ic_pause_red_triangle) // Triângulo vermelho
                setColorFilter(Color.parseColor("#EF4444"))
                val iconSize = dpToPx(18)
                layoutParams = LinearLayout.LayoutParams(iconSize, iconSize)
            }
            addView(pauseResumeIcon)

            setOnClickListener {
                val state = ScreenCaptureService.recordingState.value.status
                val action = if (state == RecordingStatus.PAUSED) {
                    ScreenCaptureService.ACTION_RESUME
                } else {
                    ScreenCaptureService.ACTION_PAUSE
                }
                sendServiceAction(action)
            }
        }
        expandedControlsLayout?.addView(pauseResumeLayout)

        // STOP BUTTON: Quadrado Vermelho que para a gravação
        stopLayout = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            orientation = LinearLayout.HORIZONTAL
            setPadding(dpToPx(8), dpToPx(5), dpToPx(10), dpToPx(5))
            val btnBg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(5).toFloat()
                setColor(Color.parseColor("#EF4444")) // Quadrado vermelho
            }
            background = btnBg
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dpToPx(36)
            ).apply {
                marginEnd = dpToPx(8)
            }

            val squareView = View(context).apply {
                val sq = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dpToPx(2).toFloat()
                    setColor(Color.WHITE)
                }
                background = sq
                val s = dpToPx(10)
                layoutParams = LinearLayout.LayoutParams(s, s).apply { marginEnd = dpToPx(6) }
            }
            addView(squareView)

            val stopLabel = TextView(context).apply {
                text = "PARAR"
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            addView(stopLabel)

            setOnClickListener {
                sendServiceAction(ScreenCaptureService.ACTION_STOP)
            }
        }
        expandedControlsLayout?.addView(stopLayout)

        // APP BUTTON (Opens Anexus)
        val appBtn = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            val btnBg = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#3B1D78"))
            }
            background = btnBg
            val size = dpToPx(34)
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                marginEnd = dpToPx(6)
            }

            val appIcon = ImageView(context).apply {
                setImageResource(R.drawable.ic_tile_record)
                setColorFilter(Color.parseColor("#A855F7"))
                val iconSize = dpToPx(18)
                layoutParams = LinearLayout.LayoutParams(iconSize, iconSize)
            }
            addView(appIcon)

            setOnClickListener {
                val intent = Intent(this@FloatingBubbleService, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                startActivity(intent)
            }
        }
        expandedControlsLayout?.addView(appBtn)

        // DISMISS BUTTON (X to close bubble if standby)
        val closeBtn = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setColorFilter(Color.parseColor("#94A3B8"))
            val s = dpToPx(24)
            layoutParams = LinearLayout.LayoutParams(s, s).apply {
                marginStart = dpToPx(2)
            }
            setOnClickListener {
                if (ScreenCaptureService.recordingState.value.isRecordingActive) {
                    // Just collapse
                    toggleExpanded()
                } else {
                    // Close bubble
                    hideBubble()
                }
            }
        }
        expandedControlsLayout?.addView(closeBtn)

        mainLayout.addView(expandedControlsLayout)
        container.addView(mainLayout)

        return container
    }

    private fun startRecordingFromBubble() {
        val code = pendingResultCode
        val data = pendingResultData

        if (code == 0 || data == null) {
            Toast.makeText(this, "Permissão de gravação necessária. Abra o Anexus.", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(MainActivity.EXTRA_TRIGGER_RECORD, true)
            }
            startActivity(intent)
            hideBubble()
            return
        }

        val settingsRepo = SettingsRepository(this)
        val cd = settingsRepo.getConfig().countdownSeconds

        if (cd > 0) {
            countdownJob?.cancel()
            countdownJob = serviceScope.launch {
                for (i in cd downTo 1) {
                    timerTextView?.text = "REC EM $i..."
                    delay(1000L)
                }
                launchCaptureService(code, data)
            }
        } else {
            launchCaptureService(code, data)
        }
    }

    private fun launchCaptureService(resultCode: Int, resultData: Intent) {
        val serviceIntent = Intent(this, ScreenCaptureService::class.java).apply {
            action = ScreenCaptureService.ACTION_START
            putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, resultCode)
            putExtra(ScreenCaptureService.EXTRA_RESULT_DATA, resultData)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        isStandby = false
        // Collapse controls back to pill
        isExpanded = false
        expandedControlsLayout?.visibility = View.GONE
        try {
            windowManager?.updateViewLayout(bubbleContainer, layoutParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun sendServiceAction(action: String) {
        val intent = Intent(this, ScreenCaptureService::class.java).apply {
            this.action = action
        }
        startService(intent)
    }

    private fun hideBubble() {
        if (bubbleContainer != null) {
            try {
                windowManager?.removeView(bubbleContainer)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            bubbleContainer = null
        }
        pendingResultCode = 0
        pendingResultData = null
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        stateJob?.cancel()
        countdownJob?.cancel()
        hideBubble()
    }
}
