package ru.kubsu.borshchevyk.feature.chat.ui.chat.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
    val thumbnailUrl = attachment.thumbnailKey?.let { attachmentUrls[it] }
    var loadError by remember { mutableStateOf(false) }

    LaunchedEffect(attachment.id, attachment.thumbnailKey) {
        if (url == null) {
            onResolveAttachmentUrl(attachment.id)
        }
        val key = attachment.thumbnailKey
        if (thumbnailUrl == null && key != null) {
            onResolveAttachmentUrl(key)
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
            FileAttachmentCard(attachment, isFromMe)
        }
    }
}
