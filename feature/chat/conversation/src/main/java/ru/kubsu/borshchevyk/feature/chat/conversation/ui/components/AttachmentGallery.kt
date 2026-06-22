package ru.kubsu.borshchevyk.feature.chat.conversation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import kotlinx.collections.immutable.PersistentMap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import ru.kubsu.borshchevyk.core.model.domain.Attachment
import ru.kubsu.borshchevyk.core.model.domain.DomainAttachmentType
import ru.kubsu.borshchevyk.core.ui.theme.BorshchevykTheme

@Composable
internal fun AttachmentGallery(
    attachments: List<Attachment>,
    attachmentUrls: PersistentMap<String, String>,
    thumbnailUrls: PersistentMap<String, String>,
    onResolveAttachmentUrl: (String, Boolean) -> Unit,
    onAttachmentClick: (Attachment) -> Unit,
    onDownloadClick: (String) -> Unit = {},
    onObserveProgress: (String) -> Flow<Float> = { flowOf(0f) },
    isFromMe: Boolean
) {
    if (attachments.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        attachments.forEach { attachment ->
            AttachmentItem(
                attachment = attachment,
                attachmentUrls = attachmentUrls,
                thumbnailUrls = thumbnailUrls,
                onResolveAttachmentUrl = onResolveAttachmentUrl,
                onAttachmentClick = onAttachmentClick,
                onDownloadClick = onDownloadClick,
                onObserveProgress = onObserveProgress,
                isFromMe = isFromMe
            )
        }
    }
}

@Composable
internal fun AttachmentItem(
    attachment: Attachment,
    attachmentUrls: PersistentMap<String, String>,
    thumbnailUrls: PersistentMap<String, String>,
    onResolveAttachmentUrl: (String, Boolean) -> Unit,
    onAttachmentClick: (Attachment) -> Unit,
    onDownloadClick: (String) -> Unit = {},
    onObserveProgress: (String) -> Flow<Float> = { flowOf(0f) },
    isFromMe: Boolean
) {
    val url = attachmentUrls[attachment.id]
    val thumbnailUrl = attachment.thumbnailKey?.let { thumbnailUrls[it] ?: attachmentUrls[it] } 
        ?: if (attachment.type == DomainAttachmentType.VIDEO || attachment.type == DomainAttachmentType.CIRCLE) thumbnailUrls[attachment.id] else null
    var loadError by remember { mutableStateOf(false) }
    
    val progressFlow = remember(attachment.id) { onObserveProgress(attachment.id) }
    val progress by progressFlow.collectAsState(initial = 0f)

    LaunchedEffect(attachment.id, attachment.thumbnailKey) {
        if (url == null && attachment.type != DomainAttachmentType.VIDEO && attachment.type != DomainAttachmentType.CIRCLE) {
            onResolveAttachmentUrl(attachment.id, false)
        }
        if (thumbnailUrl == null) {
            val key = attachment.thumbnailKey
            if (key != null) {
                onResolveAttachmentUrl(key, true)
            } else if (attachment.type == DomainAttachmentType.VIDEO || attachment.type == DomainAttachmentType.CIRCLE) {
                onResolveAttachmentUrl(attachment.id, true)
            }
        }
    }

    val context = LocalContext.current
    val imageRequest = remember(url, thumbnailUrl) {
        val targetUrl = thumbnailUrl ?: url
        targetUrl?.let {
            ImageRequest.Builder(context)
                .data(it)
                .crossfade(true)
                .build()
        }
    }

    when (attachment.type) {
        DomainAttachmentType.PHOTO -> {
            if ((url != null || thumbnailUrl != null) && !loadError) {
                SubcomposeAsyncImage(
                    model = imageRequest,
                    contentDescription = "Attachment",
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onAttachmentClick(attachment) },
                    contentScale = ContentScale.Crop,
                    loading = {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            if (progress > 0f && progress < 1f) {
                                CircularProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier.size(24.dp),
                                    color = if (isFromMe) BorshchevykTheme.colors.onPrimary else BorshchevykTheme.colors.primary,
                                )
                            } else {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = if (isFromMe) BorshchevykTheme.colors.onPrimary else BorshchevykTheme.colors.primary
                                )
                            }
                        }
                    },
                    error = {
                        loadError = true
                        FileAttachmentCard(attachment, isFromMe, progress, onCardClick = { onAttachmentClick(attachment) }) { onDownloadClick(attachment.id) }
                    }
                )
            } else {
                FileAttachmentCard(attachment, isFromMe, progress, onCardClick = { onAttachmentClick(attachment) }) { onDownloadClick(attachment.id) }
            }
        }
        DomainAttachmentType.VIDEO -> {
            // Only try to load an image if the server provided an explicit thumbnailKey.
            // This prevents Coil from using VideoFrameDecoder on the full video file,
            // which causes infinite spinners even for downloaded/local files.
            val hasExplicitThumbnail = attachment.thumbnailKey != null && thumbnailUrl != null

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(androidx.compose.ui.graphics.Color.Black)
                    .clickable { onAttachmentClick(attachment) },
                contentAlignment = Alignment.Center
            ) {
                if (hasExplicitThumbnail) {
                    SubcomposeAsyncImage(
                        model = imageRequest,
                        contentDescription = "Video Attachment",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        loading = {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                if (progress > 0f && progress < 1f) {
                                    CircularProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier.size(24.dp),
                                        color = if (isFromMe) BorshchevykTheme.colors.onPrimary else BorshchevykTheme.colors.primary,
                                    )
                                } else {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = if (isFromMe) BorshchevykTheme.colors.onPrimary else BorshchevykTheme.colors.primary
                                    )
                                }
                            }
                        },
                        error = {
                            // Silently fail, leaving the black background visible
                        }
                    )
                }
                
                // Play button always visible
                Surface(
                    shape = RoundedCornerShape(percent = 50),
                    color = BorshchevykTheme.colors.surface.copy(alpha = 0.7f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play Video",
                            tint = BorshchevykTheme.colors.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
        DomainAttachmentType.CIRCLE -> {
            CircleVideoPlayer(
                url = url,
                duration = attachment.duration ?: 0.0,
                imageRequest = imageRequest,
                loadError = loadError,
                onLoadError = { loadError = true },
                downloadProgress = progress
            )
        }
        DomainAttachmentType.VOICE -> {
            VoiceMessagePlayer(
                url = url,
                duration = attachment.duration ?: 0.0,
                isFromMe = isFromMe
            )
        }
        else -> {
            FileAttachmentCard(attachment, isFromMe, progress, onCardClick = { onAttachmentClick(attachment) }) { onDownloadClick(attachment.id) }
        }
    }
}
