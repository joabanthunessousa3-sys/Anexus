package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.RecordedVideo
import com.example.ui.components.VideoPlayerDialog
import com.example.ui.theme.AnexusDarkSurface
import com.example.ui.theme.AnexusDeepBlack
import com.example.ui.theme.AnexusPurpleNeon
import com.example.ui.theme.AnexusPurplePrimary
import com.example.ui.theme.AnexusRecordRed
import com.example.ui.theme.AnexusSurfaceContainer
import com.example.ui.theme.AnexusSurfaceContainerHigh

@Composable
fun GalleryScreen(
    videos: List<RecordedVideo>,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onDeleteVideo: (RecordedVideo) -> Unit,
    onRenameVideo: (RecordedVideo, String) -> Unit,
    onNavigateToRecord: () -> Unit
) {
    val context = LocalContext.current
    var selectedVideoForPlayback by remember { mutableStateOf<RecordedVideo?>(null) }
    var videoToDelete by remember { mutableStateOf<RecordedVideo?>(null) }
    var videoToRename by remember { mutableStateOf<RecordedVideo?>(null) }
    var newRenameText by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }

    val filteredVideos = if (searchQuery.isBlank()) {
        videos
    } else {
        videos.filter { it.displayName.contains(searchQuery, ignoreCase = true) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AnexusDeepBlack)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "GALERIA",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "${videos.size} vídeos gravados",
                        style = MaterialTheme.typography.bodySmall,
                        color = AnexusPurpleNeon
                    )
                }

                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(AnexusSurfaceContainer)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Atualizar galeria",
                        tint = Color.White
                    )
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                placeholder = { Text("Buscar gravações...", color = Color(0xFF64748B)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8)
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AnexusPurplePrimary,
                    unfocusedBorderColor = Color(0xFF222840),
                    focusedContainerColor = AnexusDarkSurface,
                    unfocusedContainerColor = AnexusDarkSurface,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AnexusPurplePrimary)
                }
            } else if (filteredVideos.isEmpty()) {
                // Empty State
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(AnexusSurfaceContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.VideoLibrary,
                                contentDescription = null,
                                tint = AnexusPurpleNeon,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = if (searchQuery.isNotBlank()) "Nenhum vídeo com esse nome" else "Nenhuma gravação ainda",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = if (searchQuery.isNotBlank()) "Tente pesquisar outro termo." else "Suas capturas de tela em formato MP4 aparecerão aqui com miniaturas e controles.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF94A3B8),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        if (searchQuery.isBlank()) {
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = onNavigateToRecord,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AnexusPurplePrimary)
                            ) {
                                Text("Gravar Meu Primeiro Vídeo")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(filteredVideos, key = { it.id }) { video ->
                        VideoCardItem(
                            video = video,
                            onPlay = { selectedVideoForPlayback = video },
                            onShare = { shareVideo(context, video) },
                            onRename = {
                                videoToRename = video
                                newRenameText = video.displayName.removeSuffix(".mp4")
                            },
                            onDelete = { videoToDelete = video }
                        )
                    }
                }
            }
        }

        // In-App Video Player Dialog
        selectedVideoForPlayback?.let { video ->
            VideoPlayerDialog(
                video = video,
                onDismiss = { selectedVideoForPlayback = null },
                onShare = { shareVideo(context, video) }
            )
        }

        // Delete Confirmation Dialog
        videoToDelete?.let { video ->
            AlertDialog(
                onDismissRequest = { videoToDelete = null },
                title = { Text("Excluir gravação?") },
                text = { Text("O vídeo '${video.displayName}' será excluído permanentemente do armazenamento.") },
                confirmButton = {
                    Button(
                        onClick = {
                            onDeleteVideo(video)
                            videoToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AnexusRecordRed)
                    ) {
                        Text("Excluir")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { videoToDelete = null }) {
                        Text("Cancelar", color = Color.Gray)
                    }
                },
                containerColor = AnexusDarkSurface
            )
        }

        // Rename Dialog
        videoToRename?.let { video ->
            AlertDialog(
                onDismissRequest = { videoToRename = null },
                title = { Text("Renomear Vídeo") },
                text = {
                    OutlinedTextField(
                        value = newRenameText,
                        onValueChange = { newRenameText = it },
                        singleLine = true,
                        placeholder = { Text("Novo nome") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newRenameText.isNotBlank()) {
                                onRenameVideo(video, newRenameText.trim())
                            }
                            videoToRename = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AnexusPurplePrimary)
                    ) {
                        Text("Salvar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { videoToRename = null }) {
                        Text("Cancelar", color = Color.Gray)
                    }
                },
                containerColor = AnexusDarkSurface
            )
        }
    }
}

@Composable
fun VideoCardItem(
    video: RecordedVideo,
    onPlay: () -> Unit,
    onShare: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("video_item_${video.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = AnexusDarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF222840))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Video Thumbnail with play icon
                Box(
                    modifier = Modifier
                        .size(width = 110.dp, height = 74.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AnexusSurfaceContainer)
                        .clickable { onPlay() },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(video.uri)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Miniatura do vídeo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Play button overlay
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0x99000000)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Reproduzir",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Duration Badge
                    if (video.durationMs > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(4.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xCC000000))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = video.formattedDuration,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Info & Actions
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = video.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "${video.formattedSize} • ${video.formattedDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = onPlay,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Reproduzir",
                                tint = AnexusPurpleNeon,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onShare,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Compartilhar",
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onRename,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Renomear",
                                tint = Color(0xFFCBD5E1),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Excluir",
                                tint = AnexusRecordRed,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun shareVideo(context: Context, video: RecordedVideo) {
    try {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, video.uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Compartilhar vídeo Anexus"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
