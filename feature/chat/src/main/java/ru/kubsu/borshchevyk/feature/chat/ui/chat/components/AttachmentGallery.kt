package ru.kubsu.borshchevyk.feature.chat.ui.chat.components

import android.media.MediaPlayer
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import ru.kubsu.borshchevyk.core.model.domain.Attachment
import ru.kubsu.borshchevyk.core.model.dto.AttachmentType
import ru.kubsu.borshchevyk.core.ui.theme.BorshchevykTheme

@Composable
internal fun AttachmentGallery(
    attachments: List<Attachment>,
    attachmentUrls: Map<String, String>,
    onResolveAttachmentUrl: (String) -> Unit,
    isFromMe: Boolean
) {
    if (attachments.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        attachments.forEach { attachment ->
            AttachmentItem(attachment, attachmentUrls, onResolveAttachmentUrl, isFromMe)
        }
    }
}

@Composable
internal fun AttachmentItem(
    attachment: Attachment,
    attachmentUrls: Map<String, String>,
    onResolveAttachmentUrl: (String) -> Unit,
    isFromMe: Boolean
) {
    val url = attachmentUrls[attachment.id]
    var loadError by remember { mutableStateOf(false) }

    LaunchedEffect(attachment.id) {
        if (url == null) {
            onResolveAttachmentUrl(attachment.id)
        }
    }

    val context = LocalContext.current
    val imageRequest = remember(url) {
        url?.let {
            ImageRequest.Builder(context)
                .data(it)
                .crossfade(true)
                .build()
        }
    }

    var isPlaying by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(attachment.id) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    when (attachment.type) {
        AttachmentType.PHOTO -> {
            if (url != null && !loadError) {
                SubcomposeAsyncImage(
                    model = imageRequest,
                    contentDescription = "Attachment",
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop,
                    loading = {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = if (isFromMe) BorshchevykTheme.colors.onPrimary else BorshchevykTheme.colors.primary
                            )
                        }
                    },
                    error = {
                        loadError = true
                        FileAttachmentCard(attachment, isFromMe)
                    }
                )
            } else {
                FileAttachmentCard(attachment, isFromMe)
            }
        }
        AttachmentType.CIRCLE -> {
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(CircleShape)
                    .background(Color.Black)
                    .clickable {
                        if (url != null) isPlaying = !isPlaying
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isPlaying && url != null) {
                    AndroidView(
                        factory = { ctx ->
                            VideoView(ctx).apply {
                                setVideoPath(url)
                                setOnPreparedListener { it.start() }
                                setOnCompletionListener { isPlaying = false }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    if (url != null && !loadError) {
                        SubcomposeAsyncImage(
                            model = imageRequest,
                            contentDescription = "Video Circle",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            error = { loadError = true }
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play Circle",
                        tint = Color.White,
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            .padding(8.dp)
                    )
                    Text(
                        text = String.format("0:%02d", (attachment.duration?.toInt() ?: 0) % 60),
                        color = Color.White,
                        style = BorshchevykTheme.typography.labelSmall,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp)
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
        AttachmentType.VOICE -> {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isFromMe) BorshchevykTheme.colors.primaryContainer else BorshchevykTheme.colors.background
                ),
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (isPlaying) {
                                mediaPlayer?.pause()
                                isPlaying = false
                            } else {
                                if (mediaPlayer == null && url != null) {
                                    val player = MediaPlayer().apply {
                                        setDataSource(url)
                                        setOnCompletionListener {
                                            isPlaying = false
                                        }
                                        prepareAsync()
                                        setOnPreparedListener {
                                            start()
                                            isPlaying = true
                                        }
                                    }
                                    mediaPlayer = player
                                } else {
                                    mediaPlayer?.start()
                                    isPlaying = true
                                }
                            }
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .background(if (isFromMe) BorshchevykTheme.colors.primary else BorshchevykTheme.colors.primaryContainer, CircleShape)
                    ) {
                        Icon(
                            if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = "Play Voice",
                            tint = if (isFromMe) BorshchevykTheme.colors.onPrimary else BorshchevykTheme.colors.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (isFromMe) BorshchevykTheme.colors.primary else BorshchevykTheme.colors.outline)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = String.format("0:%02d", (attachment.duration?.toInt() ?: 0) % 60),
                            style = BorshchevykTheme.typography.labelSmall,
                            color = if (isFromMe) BorshchevykTheme.colors.onPrimaryContainer else BorshchevykTheme.colors.onSurfaceVariant
                        )
                    }
                }
            }
        }
        else -> {
            FileAttachmentCard(attachment, isFromMe)
        }
    }
}

@Composable
internal fun FileAttachmentCard(
    attachment: Attachment,
    isFromMe: Boolean
) {
    val formattedSize = remember(attachment.sizeBytes) {
        val kb = attachment.sizeBytes / 1024.0
        val mb = kb / 1024.0
        when {
            mb >= 1.0 -> "%.2f MB".format(java.util.Locale.US, mb)
            kb >= 1.0 -> "%.2f KB".format(java.util.Locale.US, kb)
            else -> "${attachment.sizeBytes} B"
        }
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isFromMe) BorshchevykTheme.colors.primaryContainer else BorshchevykTheme.colors.background
        ),
        modifier = Modifier.fillMaxWidth(0.8f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when(attachment.type) {
                    AttachmentType.VOICE -> Icons.Default.Description // Placeholder
                    else -> Icons.Default.Description
                },
                contentDescription = "File",
                tint = if (isFromMe) BorshchevykTheme.colors.primary else BorshchevykTheme.colors.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = attachment.originalFilename,
                    style = BorshchevykTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = BorshchevykTheme.colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formattedSize,
                    style = BorshchevykTheme.typography.labelSmall,
                    color = BorshchevykTheme.colors.onSurfaceVariant
                )
            }
            IconButton(onClick = { /* TODO: Download */ }, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Download,
                    contentDescription = "Download",
                    modifier = Modifier.size(20.dp),
                    tint = BorshchevykTheme.colors.primary
                )
            }
        }
    }
}
