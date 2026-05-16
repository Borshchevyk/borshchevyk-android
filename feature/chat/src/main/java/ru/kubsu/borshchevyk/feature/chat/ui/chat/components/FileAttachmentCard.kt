package ru.kubsu.borshchevyk.feature.chat.ui.chat.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.kubsu.borshchevyk.core.model.domain.Attachment
import ru.kubsu.borshchevyk.core.model.domain.DomainAttachmentType
import ru.kubsu.borshchevyk.core.ui.theme.BorshchevykTheme
import java.util.Locale

@Composable
internal fun FileAttachmentCard(
    attachment: Attachment,
    isFromMe: Boolean,
    onDownloadClick: () -> Unit = {}
) {
    val formattedSize = remember(attachment.sizeBytes) {
        val kb = attachment.sizeBytes / 1024.0
        val mb = kb / 1024.0
        when {
            mb >= 1.0 -> "%.2f MB".format(Locale.US, mb)
            kb >= 1.0 -> "%.2f KB".format(Locale.US, kb)
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
                    DomainAttachmentType.VOICE -> Icons.Default.Description // Placeholder
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
            IconButton(onClick = onDownloadClick, modifier = Modifier.size(32.dp)) {
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
