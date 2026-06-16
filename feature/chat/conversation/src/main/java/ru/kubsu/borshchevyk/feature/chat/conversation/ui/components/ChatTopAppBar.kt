package ru.kubsu.borshchevyk.feature.chat.conversation.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import ru.kubsu.borshchevyk.core.ui.theme.BorshchevykTheme
import ru.kubsu.borshchevyk.feature.chat.conversation.mvi.ChatContext
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Top app bar for the Chat screen, displaying chat name, status, and action buttons.
 *
 * @param context The current context of the chat, including status and metadata.
 * @param typingUsers Set of user IDs currently typing in the chat.
 * @param onBackClick Callback invoked when the back button is clicked.
 * @param onCallClick Callback invoked when the call button is clicked.
 * @param onSettingsClick Callback invoked when the settings button is clicked.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatTopAppBar(
    context: ChatContext,
    typingUsers: Set<String>,
    onBackClick: () -> Unit,
    onCallClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (context.chatAvatarUrl != null) {
                    AsyncImage(
                        model = context.chatAvatarUrl,
                        contentDescription = "Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = BorshchevykTheme.colors.primaryContainer
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Default Avatar",
                            modifier = Modifier.padding(8.dp),
                            tint = BorshchevykTheme.colors.onPrimaryContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = context.chatName,
                        style = BorshchevykTheme.typography.titleMedium,
                        color = BorshchevykTheme.colors.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    ChatStatusLine(context, typingUsers)
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = BorshchevykTheme.colors.onSurface
                )
            }
        },
        actions = {
            IconButton(onClick = onCallClick) {
                Icon(
                    Icons.Default.Call,
                    contentDescription = "Call",
                    tint = BorshchevykTheme.colors.onSurface
                )
            }
            IconButton(onClick = onSettingsClick) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = "Chat Info",
                    tint = BorshchevykTheme.colors.onSurface
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = BorshchevykTheme.colors.background
        )
    )
}

/**
 * Displays the current status of the chat, such as online status, last seen time, or typing indicators.
 *
 * @param context The current context of the chat, including status and metadata.
 * @param typingUsers Set of user IDs currently typing in the chat.
 */
@Composable
private fun ChatStatusLine(
    context: ChatContext,
    typingUsers: Set<String>
) {
    val filteredTyping = remember(typingUsers, context.currentUserId) {
        typingUsers.filter { it != context.currentUserId }
    }

    val statusText = when {
        filteredTyping.isNotEmpty() -> {
            if (filteredTyping.size == 1) "User is typing..." else "Multiple users are typing..."
        }
        context.isOnline == true -> "online"
        context.isOnline == false && context.lastSeenAt != null -> {
            formatLastSeen(context.lastSeenAt)
        }
        else -> " "
    }

    val statusColor = if (filteredTyping.isNotEmpty() || context.isOnline == true) {
        BorshchevykTheme.colors.primary
    } else {
        BorshchevykTheme.colors.onSurfaceVariant
    }

    Text(
        text = statusText,
        style = BorshchevykTheme.typography.labelSmall,
        color = if (statusText == " ") Color.Transparent else statusColor
    )
}

/**
 * Formats a timestamp into a human-readable "last seen" string (e.g., "last seen at 14:30").
 *
 * @param lastSeenAt The timestamp in milliseconds since epoch.
 * @return A formatted string indicating when the user was last seen.
 */
private fun formatLastSeen(lastSeenAt: Long): String {
    val time = Instant.ofEpochMilli(lastSeenAt)
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()
    val now = LocalDateTime.now()
    val formatter = if (time.toLocalDate() == now.toLocalDate()) {
        DateTimeFormatter.ofPattern("HH:mm")
    } else {
        DateTimeFormatter.ofPattern("MMM d, HH:mm")
    }
    return "last seen at ${time.format(formatter)}"
}
