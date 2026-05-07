package ru.kubsu.borshchevyk.feature.chat.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import ru.kubsu.borshchevyk.core.ui.theme.BorshchevykTheme
import ru.kubsu.borshchevyk.feature.chat.AttachmentFile

@Composable
internal fun AttachmentPreviewRow(
    attachments: List<AttachmentFile>,
    onRemoveAttachment: (AttachmentFile) -> Unit
) {
    if (attachments.isEmpty()) return

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        modifier = Modifier
            .fillMaxWidth()
    ) {
        items(attachments) { attachment ->
            Box(modifier = Modifier.padding(end = 8.dp)) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(BorshchevykTheme.colors.surfaceVariant)
                ) {
                    if (attachment.contentType.startsWith("image/")) {
                        AsyncImage(
                            model = attachment.uri,
                            contentDescription = "Preview",
                            modifier = Modifier
                                .fillMaxSize()
                                .aspectRatio(1f),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Default.AttachFile,
                            contentDescription = "File",
                            tint = BorshchevykTheme.colors.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .offset(x = 5.dp, y = (-5).dp)
                        .align(Alignment.TopEnd)
                        .clip(CircleShape)
                        .background(BorshchevykTheme.colors.error)
                        .clickable { onRemoveAttachment(attachment) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Remove",
                        tint = BorshchevykTheme.colors.onError,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}
