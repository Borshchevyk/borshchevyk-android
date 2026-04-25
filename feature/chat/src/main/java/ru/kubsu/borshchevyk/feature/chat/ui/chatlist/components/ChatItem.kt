package ru.kubsu.borshchevyk.feature.chat.ui.chatlist.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import ru.kubsu.borshchevyk.core.model.domain.Chat
import ru.kubsu.borshchevyk.core.ui.theme.BorshchevykTheme

@Composable
internal fun ChatItem(
    chat: Chat,
    onClick: () -> Unit
) {
    val isSavedMessages = chat.type == ru.kubsu.borshchevyk.core.model.domain.ChatType.SAVED_MESSAGES
    val displayName = if (isSavedMessages) "Saved Messages" else chat.title ?: "Chat"
    val initial = displayName.firstOrNull()?.uppercase() ?: "?"
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSavedMessages) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(BorshchevykTheme.colors.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Bookmark,
                    contentDescription = "Saved Messages",
                    tint = BorshchevykTheme.colors.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        } else if (!chat.partnerAvatarUrl.isNullOrBlank()) {
            AsyncImage(
                model = chat.partnerAvatarUrl,
                contentDescription = "Avatar",
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(BorshchevykTheme.colors.surfaceVariant),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(BorshchevykTheme.colors.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial,
                    style = BorshchevykTheme.typography.titleMedium,
                    color = BorshchevykTheme.colors.primary
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayName,
                style = BorshchevykTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = BorshchevykTheme.colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = chat.lastMessage ?: "No messages yet",
                style = BorshchevykTheme.typography.bodyMedium,
                color = BorshchevykTheme.colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (chat.unreadCount > 0) {
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                color = BorshchevykTheme.colors.primary,
                shape = CircleShape,
                modifier = Modifier.sizeIn(minWidth = 22.dp, minHeight = 22.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text(
                        text = if (chat.unreadCount > 99) "99+" else chat.unreadCount.toString(),
                        style = BorshchevykTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = BorshchevykTheme.colors.onPrimary
                    )
                }
            }
        }
    }
}
