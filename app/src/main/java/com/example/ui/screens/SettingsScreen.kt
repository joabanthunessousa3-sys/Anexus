package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.AudioSourceOption
import com.example.data.model.OrientationOption
import com.example.data.model.RecordingConfig
import com.example.data.model.ResolutionOption
import com.example.ui.theme.AnexusBlueCyan
import com.example.ui.theme.AnexusBlueSecondary
import com.example.ui.theme.AnexusDarkSurface
import com.example.ui.theme.AnexusDeepBlack
import com.example.ui.theme.AnexusPurpleNeon
import com.example.ui.theme.AnexusPurplePrimary
import com.example.ui.theme.AnexusSurfaceContainer
import com.example.ui.theme.AnexusSurfaceContainerHigh

@Composable
fun SettingsScreen(
    config: RecordingConfig,
    onConfigChange: ((RecordingConfig) -> RecordingConfig) -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var showResolutionDialog by remember { mutableStateOf(false) }
    var showFpsDialog by remember { mutableStateOf(false) }
    var showBitrateDialog by remember { mutableStateOf(false) }
    var showAudioDialog by remember { mutableStateOf(false) }
    var showCountdownDialog by remember { mutableStateOf(false) }
    var showOrientationDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AnexusDeepBlack)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = "CONFIGURAÇÕES",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Category 1: Vídeo
            SettingsSectionHeader(title = "VÍDEO", icon = Icons.Default.Videocam)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = AnexusDarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF222840))
            ) {
                Column {
                    SettingsClickableItem(
                        title = "Resolução",
                        subtitle = config.resolution.label,
                        onClick = { showResolutionDialog = true }
                    )
                    HorizontalDivider(color = Color(0xFF1B2032))
                    SettingsClickableItem(
                        title = "Taxa de Quadros (FPS)",
                        subtitle = "${config.frameRate} FPS (Quadros por segundo)",
                        onClick = { showFpsDialog = true }
                    )
                    HorizontalDivider(color = Color(0xFF1B2032))
                    SettingsClickableItem(
                        title = "Qualidade de Vídeo (Bitrate)",
                        subtitle = formatBitrate(config.bitRateBps),
                        onClick = { showBitrateDialog = true }
                    )
                    HorizontalDivider(color = Color(0xFF1B2032))
                    SettingsClickableItem(
                        title = "Orientação",
                        subtitle = config.orientation.label,
                        onClick = { showOrientationDialog = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Category 2: Áudio
            SettingsSectionHeader(title = "ÁUDIO", icon = Icons.Default.Audiotrack)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = AnexusDarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF222840))
            ) {
                Column {
                    SettingsClickableItem(
                        title = "Fonte de Áudio",
                        subtitle = config.audioSource.label,
                        onClick = { showAudioDialog = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Category 3: Controles e Interface
            SettingsSectionHeader(title = "CONTROLES E GRAVAÇÃO", icon = Icons.Default.Layers)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = AnexusDarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF222840))
            ) {
                Column {
                    SettingsClickableItem(
                        title = "Contagem Regressiva",
                        subtitle = if (config.countdownSeconds == 0) "Desativada" else "${config.countdownSeconds} segundos",
                        onClick = { showCountdownDialog = true }
                    )
                    HorizontalDivider(color = Color(0xFF1B2032))
                    SettingsSwitchItem(
                        title = "Bolha Flutuante",
                        subtitle = "Controles acessíveis sobre outros aplicativos",
                        checked = config.showFloatingBubble,
                        onCheckedChange = { checked ->
                            if (checked && !Settings.canDrawOverlays(context)) {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            } else {
                                onConfigChange { it.copy(showFloatingBubble = checked) }
                            }
                        }
                    )
                    HorizontalDivider(color = Color(0xFF1B2032))
                    SettingsSwitchItem(
                        title = "Janela Invisível no Vídeo (FLAG_SECURE)",
                        subtitle = "Exclui a bolha flutuante da gravação de tela",
                        checked = config.secureInvisibleBubble,
                        onCheckedChange = { checked ->
                            onConfigChange { it.copy(secureInvisibleBubble = checked) }
                        }
                    )
                    HorizontalDivider(color = Color(0xFF1B2032))
                    SettingsSwitchItem(
                        title = "Parar ao desligar tela",
                        subtitle = "Finaliza a gravação automaticamente quando a tela apaga",
                        checked = config.stopOnScreenOff,
                        onCheckedChange = { checked ->
                            onConfigChange { it.copy(stopOnScreenOff = checked) }
                        }
                    )
                    HorizontalDivider(color = Color(0xFF1B2032))
                    SettingsSwitchItem(
                        title = "Vibração de Confirmação",
                        subtitle = "Vibra suavemente ao iniciar, pausar e parar a gravação",
                        checked = config.vibrateOnAction,
                        onCheckedChange = { checked ->
                            onConfigChange { it.copy(vibrateOnAction = checked) }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Category 4: Quick Settings Tile Help
            SettingsSectionHeader(title = "PAINEL DE NOTIFICAÇÕES (QUICK SETTINGS)", icon = Icons.Default.Info)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = AnexusDarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF222840))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Botão Rápido Anexus Recorder",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "1. Deslize a barra de notificações do Android duas vezes para baixo.\n" +
                                "2. Toque no ícone de lápis / editar botões rápidos.\n" +
                                "3. Arraste o botão 'Anexus Gravador' para o seu painel ativo.\n" +
                                "4. Inicie ou finalize suas gravações com apenas 1 toque!",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5E1),
                        lineHeight = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Category 5: About Anexus
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = AnexusSurfaceContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Anexus Screen Recorder v1.0.0",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Desenvolvido em Kotlin nativo com MediaProjection e MediaRecorder. Otimizado para alta performance e aparelhos como Samsung Galaxy A05.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Selection Dialogs
        if (showResolutionDialog) {
            OptionPickerDialog(
                title = "Selecionar Resolução",
                options = ResolutionOption.values().map { it.label },
                selectedIndex = ResolutionOption.values().indexOf(config.resolution),
                onSelect = { index ->
                    onConfigChange { it.copy(resolution = ResolutionOption.values()[index]) }
                    showResolutionDialog = false
                },
                onDismiss = { showResolutionDialog = false }
            )
        }

        if (showFpsDialog) {
            val fpsOptions = listOf(30, 60)
            OptionPickerDialog(
                title = "Selecionar Taxa de Quadros",
                options = fpsOptions.map { "$it FPS" },
                selectedIndex = fpsOptions.indexOf(config.frameRate).coerceAtLeast(0),
                onSelect = { index ->
                    onConfigChange { it.copy(frameRate = fpsOptions[index]) }
                    showFpsDialog = false
                },
                onDismiss = { showFpsDialog = false }
            )
        }

        if (showBitrateDialog) {
            val bitrates = listOf(4_000_000, 8_000_000, 12_000_000, 16_000_000, 24_000_000)
            OptionPickerDialog(
                title = "Taxa de Bits (Qualidade)",
                options = bitrates.map { formatBitrate(it) },
                selectedIndex = bitrates.indexOf(config.bitRateBps).coerceAtLeast(0),
                onSelect = { index ->
                    onConfigChange { it.copy(bitRateBps = bitrates[index]) }
                    showBitrateDialog = false
                },
                onDismiss = { showBitrateDialog = false }
            )
        }

        if (showAudioDialog) {
            OptionPickerDialog(
                title = "Fonte de Áudio",
                options = AudioSourceOption.values().map { "${it.label} — ${it.description}" },
                selectedIndex = AudioSourceOption.values().indexOf(config.audioSource),
                onSelect = { index ->
                    onConfigChange { it.copy(audioSource = AudioSourceOption.values()[index]) }
                    showAudioDialog = false
                },
                onDismiss = { showAudioDialog = false }
            )
        }

        if (showCountdownDialog) {
            val countdowns = listOf(0, 3, 5, 10)
            OptionPickerDialog(
                title = "Contagem Regressiva",
                options = countdowns.map { if (it == 0) "Sem contagem" else "$it segundos" },
                selectedIndex = countdowns.indexOf(config.countdownSeconds).coerceAtLeast(0),
                onSelect = { index ->
                    onConfigChange { it.copy(countdownSeconds = countdowns[index]) }
                    showCountdownDialog = false
                },
                onDismiss = { showCountdownDialog = false }
            )
        }

        if (showOrientationDialog) {
            OptionPickerDialog(
                title = "Orientação do Vídeo",
                options = OrientationOption.values().map { it.label },
                selectedIndex = OrientationOption.values().indexOf(config.orientation),
                onSelect = { index ->
                    onConfigChange { it.copy(orientation = OrientationOption.values()[index]) }
                    showOrientationDialog = false
                },
                onDismiss = { showOrientationDialog = false }
            )
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AnexusPurpleNeon,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = AnexusPurpleNeon,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun SettingsClickableItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF94A3B8)
            )
        }
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = Color(0xFF3B82F6),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun SettingsSwitchItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = AnexusPurplePrimary,
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = AnexusSurfaceContainer
            )
        )
    }
}

@Composable
fun OptionPickerDialog(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = AnexusDarkSurface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(16.dp))
                options.forEachIndexed { index, label ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(index) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = index == selectedIndex,
                            onClick = { onSelect(index) },
                            colors = RadioButtonDefaults.colors(selectedColor = AnexusPurplePrimary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (index == selectedIndex) Color.White else Color(0xFFCBD5E1)
                        )
                    }
                }
            }
        }
    }
}

private fun formatBitrate(bps: Int): String {
    val mbps = bps / 1_000_000
    return when (mbps) {
        4 -> "4 Mbps (Econômico / Arquivos leves)"
        8 -> "8 Mbps (Padrão balanceado)"
        12 -> "12 Mbps (Alta qualidade HD)"
        16 -> "16 Mbps (Ultra nítido)"
        24 -> "24 Mbps (Qualidade Máxima)"
        else -> "$mbps Mbps"
    }
}
