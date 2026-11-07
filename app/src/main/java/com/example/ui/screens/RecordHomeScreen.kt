package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AudioSourceOption
import com.example.data.model.RecordingConfig
import com.example.data.model.RecordingSnapshot
import com.example.data.model.RecordingStatus
import com.example.data.model.ResolutionOption
import com.example.ui.theme.AnexusBlueCyan
import com.example.ui.theme.AnexusBlueSecondary
import com.example.ui.theme.AnexusDarkSurface
import com.example.ui.theme.AnexusDeepBlack
import com.example.ui.theme.AnexusPurpleNeon
import com.example.ui.theme.AnexusPurplePrimary
import com.example.ui.theme.AnexusRecordRed
import com.example.ui.theme.AnexusSurfaceContainer
import com.example.ui.theme.AnexusSurfaceContainerHigh

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecordHomeScreen(
    recordingSnapshot: RecordingSnapshot,
    config: RecordingConfig,
    countdown: Int?,
    onStartRequested: () -> Unit,
    onPauseClicked: () -> Unit,
    onResumeClicked: () -> Unit,
    onStopClicked: () -> Unit,
    onCancelCountdown: () -> Unit,
    onConfigChange: ((RecordingConfig) -> RecordingConfig) -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var showOverlayPermissionDialog by remember { mutableStateOf(false) }

    // Pulsing animation for record button and recording state
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val isRecording = recordingSnapshot.isRecordingActive
    val isPaused = recordingSnapshot.status == RecordingStatus.PAUSED

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AnexusDeepBlack)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Logo, Title, Status Chip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_anexus_logo),
                        contentDescription = "Anexus Logo",
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, AnexusPurplePrimary, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "ANEXUS",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.5.sp,
                            color = Color.White
                        )
                        Text(
                            text = "GRAVADOR DE TELA",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = AnexusPurpleNeon,
                            letterSpacing = 2.sp
                        )
                    }
                }

                // Live Status Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isRecording) {
                                if (isPaused) Color(0x33F59E0B) else Color(0x33EF4444)
                            } else Color(0x2210B981)
                        )
                        .border(
                            1.dp,
                            if (isRecording) {
                                if (isPaused) Color(0xFFF59E0B) else Color(0xFFEF4444)
                            } else Color(0xFF10B981),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isRecording) {
                                        if (isPaused) Color(0xFFF59E0B) else Color(0xFFEF4444)
                                    } else Color(0xFF10B981)
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isRecording) {
                                if (isPaused) "PAUSADO" else "GRAVANDO"
                            } else "PRONTO",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isRecording) {
                                if (isPaused) Color(0xFFF59E0B) else Color(0xFFEF4444)
                            } else Color(0xFF10B981)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main Recording Center Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp)),
                colors = CardDefaults.cardColors(containerColor = AnexusDarkSurface),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isRecording) Color(0x66EF4444) else Color(0x338B5CF6)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp, horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isRecording) {
                        // Chronometer
                        Text(
                            text = recordingSnapshot.formattedElapsedTime,
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = if (isPaused) Color(0xFFF59E0B) else AnexusPurpleNeon,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isPaused) "Captura pausada" else "Capturando tela e áudio...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isPaused) Color(0xFFF59E0B) else Color(0xFF94A3B8)
                            )
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        // Controls: Pause (Triângulo Vermelho) + Stop (Quadrado Vermelho)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = if (isPaused) onResumeClicked else onPauseClicked,
                                modifier = Modifier
                                    .height(54.dp)
                                    .testTag("pause_resume_button"),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1B2032),
                                    contentColor = Color.White
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444))
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_pause_red_triangle),
                                    contentDescription = if (isPaused) "Continuar" else "Pausar",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (isPaused) "Continuar" else "Pausar", color = Color.White)
                            }

                            // Botão Parar Gravação: Quadrado Vermelho
                            Button(
                                onClick = onStopClicked,
                                modifier = Modifier
                                    .height(54.dp)
                                    .testTag("stop_recording_button"),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFEF4444),
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_recording_red_square),
                                    contentDescription = "Parar Gravação",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Parar", fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        // Botão de Gravação: Bolinha Vermelha
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(140.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            Color(0x66EF4444),
                                            Color(0x22EF4444),
                                            Color.Transparent
                                        )
                                    )
                                )
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(110.dp)
                                    .clip(CircleShape) // Bolinha Vermelha
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                Color(0xFFFF3333),
                                                Color(0xFFEF4444),
                                                Color(0xFFDC2626)
                                            )
                                        )
                                    )
                                    .border(2.dp, Color(0x66FFFFFF), CircleShape)
                                    .clickable { onStartRequested() }
                                    .testTag("start_recording_button")
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "GRAVAR",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = "Toque em REC para ativar o botão flutuante",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )

                        Text(
                            text = "O botão flutuante aparecerá na tela para iniciar, pausar e finalizar a qualquer momento. Ele é 100% invisível no vídeo gravado!",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Quick Parameters Selector
            Text(
                text = "CONFIGURAÇÃO RÁPIDA",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = AnexusPurpleNeon,
                letterSpacing = 1.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, bottom = 8.dp)
            )

            // Chips Row: Resolution, FPS, Audio
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Resolution Selector Chip
                FilterChip(
                    selected = true,
                    onClick = {
                        val next = when (config.resolution) {
                            ResolutionOption.P1080 -> ResolutionOption.P720
                            ResolutionOption.P720 -> ResolutionOption.P480
                            ResolutionOption.P480 -> ResolutionOption.AUTO
                            ResolutionOption.AUTO -> ResolutionOption.P1080
                        }
                        onConfigChange { it.copy(resolution = next) }
                    },
                    label = { Text(config.resolution.label) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = AnexusPurplePrimary
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = AnexusSurfaceContainer,
                        labelColor = Color.White,
                        selectedContainerColor = AnexusSurfaceContainerHigh,
                        selectedLabelColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                // FPS Chip
                FilterChip(
                    selected = true,
                    onClick = {
                        val nextFps = if (config.frameRate == 60) 30 else 60
                        onConfigChange { it.copy(frameRate = nextFps) }
                    },
                    label = { Text("${config.frameRate} FPS") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = AnexusBlueSecondary
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = AnexusSurfaceContainer,
                        labelColor = Color.White,
                        selectedContainerColor = AnexusSurfaceContainerHigh,
                        selectedLabelColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                // Audio Chip
                FilterChip(
                    selected = config.audioSource != AudioSourceOption.NONE,
                    onClick = {
                        val nextAudio = when (config.audioSource) {
                            AudioSourceOption.MIC -> AudioSourceOption.INTERNAL
                            AudioSourceOption.INTERNAL -> AudioSourceOption.NONE
                            AudioSourceOption.NONE -> AudioSourceOption.MIC
                        }
                        onConfigChange { it.copy(audioSource = nextAudio) }
                    },
                    label = {
                        Text(
                            when (config.audioSource) {
                                AudioSourceOption.MIC -> "Microfone"
                                AudioSourceOption.INTERNAL -> "Áudio Interno"
                                AudioSourceOption.NONE -> "Mudo"
                            }
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (config.audioSource == AudioSourceOption.NONE) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (config.audioSource == AudioSourceOption.NONE) Color.Gray else AnexusBlueCyan
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = AnexusSurfaceContainer,
                        labelColor = Color.White,
                        selectedContainerColor = AnexusSurfaceContainerHigh,
                        selectedLabelColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                // Countdown Chip
                FilterChip(
                    selected = config.countdownSeconds > 0,
                    onClick = {
                        val nextCd = when (config.countdownSeconds) {
                            0 -> 3
                            3 -> 5
                            5 -> 10
                            else -> 0
                        }
                        onConfigChange { it.copy(countdownSeconds = nextCd) }
                    },
                    label = { Text(if (config.countdownSeconds == 0) "Sem contagem" else "${config.countdownSeconds}s contagem") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = AnexusPurpleNeon
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = AnexusSurfaceContainer,
                        labelColor = Color.White,
                        selectedContainerColor = AnexusSurfaceContainerHigh,
                        selectedLabelColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Floating Bubble Feature Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = AnexusDarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF222840))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Bolha Flutuante",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0x3310B981))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Invisível no vídeo",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF10B981),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            Text(
                                text = "Controles flutuantes sobre outros apps. Utiliza camada segura para não sair no arquivo gravado.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8)
                            )
                        }

                        Switch(
                            checked = config.showFloatingBubble,
                            onCheckedChange = { isChecked ->
                                if (isChecked && !Settings.canDrawOverlays(context)) {
                                    showOverlayPermissionDialog = true
                                } else {
                                    onConfigChange { it.copy(showFloatingBubble = isChecked) }
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = AnexusPurplePrimary,
                                uncheckedThumbColor = Color.Gray,
                                uncheckedTrackColor = AnexusSurfaceContainer
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(AnexusSurfaceContainer)
                            .padding(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = AnexusBlueCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Proteção de Janela Ativa: os botões flutuantes não aparecem no vídeo gravado.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFCBD5E1)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Storage Status Bar
            val freeGb = getAvailableStorageGb(context)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AnexusSurfaceContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Armazenamento Disponível",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = "$freeGb GB livres",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Text(
                        text = "Pasta: Movies/Anexus",
                        style = MaterialTheme.typography.labelSmall,
                        color = AnexusPurpleNeon
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Countdown Overlay
        AnimatedVisibility(
            visible = countdown != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xDD090A10)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "GRAVAÇÃO INICIANDO",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AnexusPurpleNeon,
                        letterSpacing = 2.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "${countdown ?: 1}",
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 96.sp),
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedButton(
                        onClick = onCancelCountdown,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Text("Cancelar")
                    }
                }
            }
        }
    }

    // Overlay Permission Dialog
    if (showOverlayPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showOverlayPermissionDialog = false },
            title = { Text("Permitir Janela Flutuante") },
            text = {
                Text(
                    "Para exibir a bolha flutuante sobre outros aplicativos durante as gravações, conceda a permissão 'Sobrepor a outros apps' nas configurações do Android."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showOverlayPermissionDialog = false
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AnexusPurplePrimary)
                ) {
                    Text("Abrir Configurações")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOverlayPermissionDialog = false }) {
                    Text("Depois", color = Color.Gray)
                }
            },
            containerColor = AnexusDarkSurface
        )
    }
}

private fun getAvailableStorageGb(context: Context): String {
    return try {
        val path = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir
        val stat = StatFs(path.path)
        val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
        String.format("%.1f", availableBytes.toDouble() / (1024 * 1024 * 1024))
    } catch (e: Exception) {
        "10.0"
    }
}
