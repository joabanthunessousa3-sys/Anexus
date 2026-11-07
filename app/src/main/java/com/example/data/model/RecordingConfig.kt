package com.example.data.model

enum class ResolutionOption(val label: String, val width: Int, val height: Int, val isAuto: Boolean = false) {
    AUTO("Nativa / Automática", 0, 0, true),
    P480("480p (SD)", 854, 480),
    P720("720p (HD)", 1280, 720),
    P1080("1080p (Full HD)", 1920, 1080)
}

enum class AudioSourceOption(val label: String, val description: String) {
    NONE("Sem Áudio (Silencioso)", "Vídeo gravado sem nenhuma faixa de som"),
    MIC("Microfone", "Grava voz e som ambiente pelo microfone do aparelho"),
    INTERNAL("Áudio Interno (Android 10+)", "Captura o som reproduzido pelos aplicativos e jogos")
}

enum class OrientationOption(val label: String) {
    AUTO("Automática"),
    PORTRAIT("Retrato (Vertical)"),
    LANDSCAPE("Paisagem (Horizontal)")
}

data class RecordingConfig(
    val resolution: ResolutionOption = ResolutionOption.P1080,
    val frameRate: Int = 60, // 30 or 60
    val bitRateBps: Int = 12_000_000, // 4M, 8M, 12M, 16M, 24M
    val audioSource: AudioSourceOption = AudioSourceOption.MIC,
    val countdownSeconds: Int = 3, // 0, 3, 5, 10
    val showFloatingBubble: Boolean = true,
    val secureInvisibleBubble: Boolean = true, // Uses FLAG_SECURE so bubble is excluded from screen capture
    val stopOnScreenOff: Boolean = true,
    val vibrateOnAction: Boolean = true,
    val orientation: OrientationOption = OrientationOption.AUTO
)
