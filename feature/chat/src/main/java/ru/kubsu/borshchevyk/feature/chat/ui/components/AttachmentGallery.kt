package ru.kubsu.borshchevyk.feature.chat.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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

    if (attachment.type == AttachmentType.PHOTO && url != null && !loadError) {
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
