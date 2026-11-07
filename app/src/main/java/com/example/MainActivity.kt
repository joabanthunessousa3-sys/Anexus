package com.example

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AudioSourceOption
import com.example.ui.MainViewModel
import com.example.ui.screens.GalleryScreen
import com.example.ui.screens.RecordHomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.AnexusDarkSurface
import com.example.ui.theme.AnexusDeepBlack
import com.example.ui.theme.AnexusPurpleNeon
import com.example.ui.theme.AnexusPurplePrimary
import com.example.ui.theme.MyApplicationTheme

enum class DisplayMode {
    HTML5,
    NATIVE
}

class WebAppInterface(private val activity: MainActivity) {
    @JavascriptInterface
    fun showToast(message: String) {
        activity.runOnUiThread {
            Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
        }
    }

    @JavascriptInterface
    fun startNativeRecording() {
        activity.runOnUiThread {
            activity.checkOverlayAndTriggerFlow()
        }
    }

    @JavascriptInterface
    fun stopNativeRecording() {
        activity.runOnUiThread {
            activity.viewModel.stopRecording()
        }
    }

    @JavascriptInterface
    fun pauseNativeRecording() {
        activity.runOnUiThread {
            activity.viewModel.pauseRecording()
        }
    }

    @JavascriptInterface
    fun resumeNativeRecording() {
        activity.runOnUiThread {
            activity.viewModel.resumeRecording()
        }
    }
}

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_TRIGGER_RECORD = "extra_trigger_record"
        const val EXTRA_OPEN_TAB = "extra_open_tab"
    }

    val viewModel: MainViewModel by viewModels()
    private var showOverlayPermissionDialog by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                var currentMode by remember { mutableStateOf(DisplayMode.HTML5) }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AnexusDeepBlack)
                ) {
                    when (currentMode) {
                        DisplayMode.HTML5 -> {
                            HtmlAppView(
                                modifier = Modifier.fillMaxSize(),
                                activity = this@MainActivity
                            )
                        }
                        DisplayMode.NATIVE -> {
                            MainAppScreen(
                                viewModel = viewModel,
                                onStartProjectionRequested = { checkOverlayAndTriggerFlow() }
                            )
                        }
                    }

                    // Floating Mode Switcher Chip at top right
                    Box(
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(top = 10.dp, end = 12.dp)
                            .align(Alignment.TopEnd)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = AnexusDarkSurface.copy(alpha = 0.94f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AnexusPurplePrimary.copy(alpha = 0.6f)),
                            shadowElevation = 8.dp,
                            modifier = Modifier
                                .clickable {
                                    currentMode = if (currentMode == DisplayMode.HTML5) DisplayMode.NATIVE else DisplayMode.HTML5
                                }
                                .testTag("toggle_app_mode_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = if (currentMode == DisplayMode.HTML5) Icons.Default.PhoneAndroid else Icons.Default.Code,
                                    contentDescription = "Alternar Modo",
                                    tint = AnexusPurpleNeon,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (currentMode == DisplayMode.HTML5) "Modo Nativo" else "Modo HTML5",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (showOverlayPermissionDialog) {
                        AlertDialog(
                            onDismissRequest = { showOverlayPermissionDialog = false },
                            title = { Text("Permissão de Sobreposição") },
                            text = {
                                Text(
                                    "Para exibir o botão flutuante sobre outros aplicativos durante as gravações (e que não aparece no vídeo gravado), conceda a permissão 'Sobrepor a outros apps'."
                                )
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        showOverlayPermissionDialog = false
                                        val intent = Intent(
                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            Uri.parse("package:$packageName")
                                        )
                                        overlayPermissionLauncher.launch(intent)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = AnexusPurplePrimary)
                                ) {
                                    Text("Conceder Permissão")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showOverlayPermissionDialog = false }) {
                                    Text("Cancelar", color = Color.Gray)
                                }
                            },
                            containerColor = AnexusDarkSurface
                        )
                    }
                }
            }
        }

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val targetTab = intent?.getIntExtra(EXTRA_OPEN_TAB, -1) ?: -1
        if (targetTab >= 0) {
            viewModel.setTab(targetTab)
        }

        if (intent?.getBooleanExtra(EXTRA_TRIGGER_RECORD, false) == true) {
            checkOverlayAndTriggerFlow()
        }
    }

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Settings.canDrawOverlays(this)) {
            triggerMediaProjectionFlow()
        } else {
            Toast.makeText(this, "Permissão de sobreposição necessária para o botão flutuante", Toast.LENGTH_LONG).show()
        }
    }

    // MediaProjection launcher
    private val projectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            viewModel.onMediaProjectionGranted(result.resultCode, result.data!!)
            Toast.makeText(this, "Botão flutuante ativado! Toque nele para gravar.", Toast.LENGTH_LONG).show()
            moveTaskToBack(true) // Minimize app to home screen so floating button is ready
        } else {
            Toast.makeText(this, "Permissão de gravação de tela negada", Toast.LENGTH_SHORT).show()
        }
    }

    // Permission launcher for audio and notifications
    private val permissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        launchProjectionPrompt()
    }

    fun checkOverlayAndTriggerFlow() {
        if (!Settings.canDrawOverlays(this)) {
            showOverlayPermissionDialog = true
        } else {
            triggerMediaProjectionFlow()
        }
    }

    private fun triggerMediaProjectionFlow() {
        val permissionsToRequest = mutableListOf<String>()

        if (viewModel.config.value.audioSource != AudioSourceOption.NONE &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionsLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            launchProjectionPrompt()
        }
    }

    private fun launchProjectionPrompt() {
        val mpManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val intent = mpManager.createScreenCaptureIntent()
        projectionLauncher.launch(intent)
    }
}

@Composable
fun MainAppScreen(
    viewModel: MainViewModel,
    onStartProjectionRequested: () -> Unit
) {
    val recordingSnapshot by viewModel.recordingState.collectAsStateWithLifecycle()
    val config by viewModel.config.collectAsStateWithLifecycle()
    val countdown by viewModel.countdown.collectAsStateWithLifecycle()
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val videos by viewModel.videos.collectAsStateWithLifecycle()
    val isLoadingVideos by viewModel.isLoadingVideos.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = AnexusDeepBlack,
        bottomBar = {
            NavigationBar(
                containerColor = AnexusDarkSurface,
                contentColor = Color.White,
                tonalElevation = 4.dp,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("anexus_bottom_navigation")
            ) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { viewModel.setTab(0) },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == 0) Icons.Filled.Videocam else Icons.Outlined.Videocam,
                            contentDescription = "Gravar"
                        )
                    },
                    label = { Text("Gravar") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = AnexusPurpleNeon,
                        indicatorColor = AnexusPurplePrimary,
                        unselectedIconColor = Color(0xFF64748B),
                        unselectedTextColor = Color(0xFF64748B)
                    )
                )

                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { viewModel.setTab(1) },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == 1) Icons.Filled.VideoLibrary else Icons.Outlined.VideoLibrary,
                            contentDescription = "Galeria"
                        )
                    },
                    label = { Text("Galeria") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = AnexusPurpleNeon,
                        indicatorColor = AnexusPurplePrimary,
                        unselectedIconColor = Color(0xFF64748B),
                        unselectedTextColor = Color(0xFF64748B)
                    )
                )

                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { viewModel.setTab(2) },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == 2) Icons.Filled.Settings else Icons.Outlined.Settings,
                            contentDescription = "Configurações"
                        )
                    },
                    label = { Text("Ajustes") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = AnexusPurpleNeon,
                        indicatorColor = AnexusPurplePrimary,
                        unselectedIconColor = Color(0xFF64748B),
                        unselectedTextColor = Color(0xFF64748B)
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                0 -> RecordHomeScreen(
                    recordingSnapshot = recordingSnapshot,
                    config = config,
                    countdown = countdown,
                    onStartRequested = onStartProjectionRequested,
                    onPauseClicked = { viewModel.pauseRecording() },
                    onResumeClicked = { viewModel.resumeRecording() },
                    onStopClicked = { viewModel.stopRecording() },
                    onCancelCountdown = { viewModel.cancelCountdown() },
                    onConfigChange = { viewModel.updateConfig(it) }
                )
                1 -> GalleryScreen(
                    videos = videos,
                    isLoading = isLoadingVideos,
                    onRefresh = { viewModel.loadVideos() },
                    onDeleteVideo = { viewModel.deleteVideo(it) },
                    onRenameVideo = { video, name -> viewModel.renameVideo(video, name) },
                    onNavigateToRecord = { viewModel.setTab(0) }
                )
                2 -> SettingsScreen(
                    config = config,
                    onConfigChange = { viewModel.updateConfig(it) }
                )
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun HtmlAppView(
    modifier: Modifier = Modifier,
    activity: MainActivity
) {
    AndroidView(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.databaseEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.allowFileAccess = true
                settings.allowContentAccess = true
                settings.cacheMode = WebSettings.LOAD_DEFAULT

                webChromeClient = object : WebChromeClient() {
                    override fun onPermissionRequest(request: PermissionRequest) {
                        request.grant(request.resources)
                    }
                }
                webViewClient = WebViewClient()

                addJavascriptInterface(WebAppInterface(activity), "AndroidBridge")
                loadUrl("file:///android_asset/index.html")
            }
        }
    )
}

