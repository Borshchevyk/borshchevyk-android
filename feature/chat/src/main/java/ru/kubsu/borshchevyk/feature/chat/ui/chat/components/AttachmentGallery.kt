package ru.kubsu.borshchevyk.feature.chat.ui.chat.components

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
import ru.kubsu.borshchevyk.core.model.domain.Attachment
import ru.kubsu.borshchevyk.core.model.domain.DomainAttachmentType
import ru.kubsu.borshchevyk.core.ui.theme.BorshchevykTheme

@Composable
internal fun AttachmentGallery(
    attachments: List<Attachment>,
    attachmentUrls: Map<String, String>,
    thumbnailUrls: Map<String, String>,
    onResolveAttachmentUrl: (String, Boolean) -> Unit,
    onAttachmentClick: (Attachment) -> Unit,
    onDownloadClick: (String) -> Unit = {},
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
                isFromMe = isFromMe
            )
        }
    }
}

@Composable
internal fun AttachmentItem(
    attachment: Attachment,
    attachmentUrls: Map<String, String>,
    thumbnailUrls: Map<String, String>,
    onResolveAttachmentUrl: (String, Boolean) -> Unit,
    onAttachmentClick: (Attachment) -> Unit,
    onDownloadClick: (String) -> Unit = {},
    isFromMe: Boolean
) {
    val url = attachmentUrls[attachment.id]
    val thumbnailUrl = attachment.thumbnailKey?.let { thumbnailUrls[it] ?: attachmentUrls[it] } 
        ?: if (attachment.type == DomainAttachmentType.VIDEO || attachment.type == DomainAttachmentType.CIRCLE) thumbnailUrls[attachment.id] else null
    var loadError by remember { mutableStateOf(false) }

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
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = if (isFromMe) BorshchevykTheme.colors.onPrimary else BorshchevykTheme.colors.primary
                            )
                        }
                    },
                    error = {
                        loadError = true
                        FileAttachmentCard(attachment, isFromMe) { onDownloadClick(attachment.id) }
                    }
                )
            } else {
                FileAttachmentCard(attachment, isFromMe) { onDownloadClick(attachment.id) }
            }
        }
        DomainAttachmentType.VIDEO -> {
            if ((url != null || thumbnailUrl != null) && !loadError) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onAttachmentClick(attachment) },
                    contentAlignment = Alignment.Center
                ) {
                    SubcomposeAsyncImage(
                        model = imageRequest,
                        contentDescription = "Video Attachment",
                        modifier = Modifier.fillMaxSize(),
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
                        }
                    )
                    
                    if (!loadError) {
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
                    } else {
                        FileAttachmentCard(attachment, isFromMe) { onDownloadClick(attachment.id) }
                    }
                }
            } else {
                FileAttachmentCard(attachment, isFromMe) { onDownloadClick(attachment.id) }
            }
        }
        DomainAttachmentType.CIRCLE -> {
            CircleVideoPlayer(
                url = url,
                duration = attachment.duration ?: 0.0,
                imageRequest = imageRequest,
                loadError = loadError,
                onLoadError = { loadError = true }
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
            FileAttachmentCard(attachment, isFromMe) { onDownloadClick(attachment.id) }
        }
    }
}