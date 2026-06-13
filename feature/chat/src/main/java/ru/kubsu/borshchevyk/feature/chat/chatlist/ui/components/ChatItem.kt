package ru.kubsu.borshchevyk.feature.chat.chatlist.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ChatItem(
    chat: Chat,
    onClick: () -> Unit,
    onPinToggle: () -> Unit
) {
    val isSavedMessages = chat.type == ru.kubsu.borshchevyk.core.model.domain.ChatType.SAVED_MESSAGES
    val displayName = if (isSavedMessages) "Saved Messages" else chat.title ?: "Chat"
    val initial = displayName.firstOrNull()?.uppercase() ?: "?"
    var showMenu by rememberSaveable { mutableStateOf(false) }
    
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { showMenu = true }
                )
                .background(if (chat.isPinned) BorshchevykTheme.colors.surfaceVariant.copy(alpha = 0.5f) else BorshchevykTheme.colors.background)
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = displayName,
                        style = BorshchevykTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = BorshchevykTheme.colors.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (chat.isPinned) {
                        Icon(
                            Icons.Default.PushPin,
                            contentDescription = "Pinned",
                            tint = BorshchevykTheme.colors.onSurfaceVariant,
                            modifier = Modifier.size(16.dp).padding(start = 4.dp)
                        )
                    }
                }
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
        
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text(if (chat.isPinned) "Unpin Chat" else "Pin Chat") },
                onClick = {
                    onPinToggle()
                    showMenu = false
                },
                leadingIcon = { Icon(Icons.Default.PushPin, contentDescription = null) }
            )
        }
    }
}