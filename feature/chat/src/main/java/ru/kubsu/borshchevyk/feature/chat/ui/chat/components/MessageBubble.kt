package ru.kubsu.borshchevyk.feature.chat.ui.chat.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.kubsu.borshchevyk.core.model.domain.DomainAttachmentType
import ru.kubsu.borshchevyk.core.model.domain.Message
import ru.kubsu.borshchevyk.core.model.domain.MessageSource
import ru.kubsu.borshchevyk.core.model.domain.MessageStatus
import ru.kubsu.borshchevyk.core.ui.theme.BorshchevykTheme
import ru.kubsu.borshchevyk.feature.chat.util.MessageTimeFormatter

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun MessageBubble(
    message: Message,
    isFromMe: Boolean,
    currentUserId: String,
    attachmentUrls: Map<String, String>,
    thumbnailUrls: Map<String, String>,
    onResolveAttachmentUrl: (String, Boolean) -> Unit,
    onAttachmentClick: (ru.kubsu.borshchevyk.core.model.domain.Attachment) -> Unit,
    onPinToggle: () -> Unit,
    onReactionToggle: (String) -> Unit,
    onEdit: () -> Unit,
    onDelete: (Boolean) -> Unit,
    onMessageVisible: () -> Unit,
    onViewReaders: () -> Unit,
    onViewComments: () -> Unit,
    onResend: () -> Unit,
    onForward: () -> Unit
) {
    var showMenu by rememberSaveable { mutableStateOf(false) }
    
    LaunchedEffect(message.id) {
        if (!isFromMe) onMessageVisible()
    }
    
    val bubbleShape = remember(isFromMe) {
        if (isFromMe) {
            RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 4.dp)
        } else {
            RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 4.dp, bottomEnd = 20.dp)
        }
    }

    val isOnlyCircle = remember(message.text, message.attachments) {
        message.text.isBlank() && message.attachments.size == 1 && message.attachments.first().type == DomainAttachmentType.CIRCLE
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        horizontalAlignment = if (isFromMe) Alignment.End else Alignment.Start
    ) {
        if (!isFromMe) {
            val authorName = remember(message.author) {
                message.author?.let { "${it.firstName} ${it.lastName ?: ""}".trim() } ?: "User"
            }
            Text(
                text = authorName,
                style = BorshchevykTheme.typography.labelSmall,
                color = BorshchevykTheme.colors.primary,
                modifier = Modifier.padding(start = 12.dp, bottom = 4.dp)
            )
        }

        Box {
            Surface(
                color = if (isOnlyCircle) Color.Transparent else if (isFromMe) BorshchevykTheme.colors.primary else BorshchevykTheme.colors.surfaceVariant,
                shape = bubbleShape,
                shadowElevation = if (isOnlyCircle) 0.dp else 2.dp,
                modifier = Modifier.combinedClickable(
                    onClick = {},
                    onLongClick = { showMenu = true }
                )
            ) {
                MessageContent(
                    message = message,
                    isFromMe = isFromMe,
                    isOnlyCircle = isOnlyCircle,
                    attachmentUrls = attachmentUrls,
                    thumbnailUrls = thumbnailUrls,
                    onResolveAttachmentUrl = onResolveAttachmentUrl,
                    onAttachmentClick = onAttachmentClick,
                    onResend = onResend
                )
            }
            
            MessageDropdownMenu(
                showMenu = showMenu,
                onDismiss = { showMenu = false },
                message = message,
                isFromMe = isFromMe,
                onForward = onForward,
                onPinToggle = onPinToggle,
                onEdit = onEdit,
                onDelete = onDelete,
                onViewReaders = onViewReaders,
                onViewComments = onViewComments,
                onReactionToggle = onReactionToggle
            )
        }

        if (message.reactions.isNotEmpty()) {
            ReactionList(
                reactions = message.reactions,
                currentUserId = currentUserId,
                isFromMe = isFromMe,
                onReactionToggle = onReactionToggle
            )
        }
    }
}

/**
 * Renders the content of a message.
 *
 * @param message The message to render.
 * @param isFromMe Whether the message is sent by the current user.
 * @param isOnlyCircle Whether the message contains only a circle attachment.
 * @param attachmentUrls The map of attachment URLs.
 * @param thumbnailUrls The map of thumbnail URLs.
 * @param onResolveAttachmentUrl The callback to resolve an attachment URL.
 * @param onAttachmentClick The callback when an attachment is clicked.
 * @param onResend The callback to resend the message.
 */
@Composable
private fun MessageContent(
    message: Message,
    isFromMe: Boolean,
    isOnlyCircle: Boolean,
    attachmentUrls: Map<String, String>,
    thumbnailUrls: Map<String, String>,
    onResolveAttachmentUrl: (String, Boolean) -> Unit,
    onAttachmentClick: (ru.kubsu.borshchevyk.core.model.domain.Attachment) -> Unit,
    onResend: () -> Unit
) {
    Column(
        modifier = Modifier.padding(
            horizontal = if (isOnlyCircle) 0.dp else 16.dp, 
            vertical = if (isOnlyCircle) 0.dp else 10.dp
        )
    ) {
        if (message.forwardedFromUser != null) {
            ForwardedInfo(message, isFromMe, isOnlyCircle)
        }

        if (message.attachments.isNotEmpty()) {
            AttachmentGallery(
                attachments = message.attachments,
                attachmentUrls = attachmentUrls,
                thumbnailUrls = thumbnailUrls,
                onResolveAttachmentUrl = onResolveAttachmentUrl,
                onAttachmentClick = onAttachmentClick,
                isFromMe = isFromMe
            )
            if (!isOnlyCircle) Spacer(modifier = Modifier.height(8.dp))
        }

        if (message.text.isNotBlank()) {
            Text(
                text = message.text,
                style = BorshchevykTheme.typography.bodyLarge,
                color = if (isFromMe) BorshchevykTheme.colors.onPrimary else BorshchevykTheme.colors.onSurface
            )
        }

        MessageTimeAndStatus(message, isFromMe, isOnlyCircle, onResend)
    }
}

/**
 * Renders information about a forwarded message.
 *
 * @param message The message containing forwarding information.
 * @param isFromMe Whether the message is sent by the current user.
 * @param isOnlyCircle Whether the message contains only a circle attachment.
 */
@Composable
private fun ForwardedInfo(message: Message, isFromMe: Boolean, isOnlyCircle: Boolean) {
    val forwardedName = remember(message.forwardedFromUser) {
        message.forwardedFromUser?.let { "${it.firstName} ${it.lastName ?: ""}".trim() }?.ifBlank { "User" } ?: "User"
    }
    val color = if (isOnlyCircle) Color.White.copy(alpha = 0.7f) 
                else if (isFromMe) BorshchevykTheme.colors.onPrimary.copy(alpha = 0.7f) 
                else BorshchevykTheme.colors.primary.copy(alpha = 0.7f)

    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(12.dp), tint = color)
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "Forwarded from $forwardedName",
            style = BorshchevykTheme.typography.labelSmall.copy(fontStyle = FontStyle.Italic),
            color = color
        )
    }
    Spacer(modifier = Modifier.height(4.dp))
}

@Composable
private fun ColumnScope.MessageTimeAndStatus(
    message: Message,
    isFromMe: Boolean,
    isOnlyCircle: Boolean,
    onResend: () -> Unit
) {
    val color = if (isOnlyCircle) Color.White.copy(alpha = 0.8f) 
                else if (isFromMe) BorshchevykTheme.colors.onPrimary.copy(alpha = 0.7f) 
                else BorshchevykTheme.colors.onSurfaceVariant.copy(alpha = 0.7f)

    val baseModifier = if (isOnlyCircle) {
        Modifier
            .padding(top = 4.dp)
            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    } else {
        Modifier.padding(top = 4.dp)
    }

    Row(modifier = baseModifier.align(Alignment.End), verticalAlignment = Alignment.CenterVertically) {
        if (message.source == MessageSource.OFFLINE) {
             Text(
                 text = "Offline",
                 style = BorshchevykTheme.typography.labelSmall.copy(fontSize = 10.sp, fontStyle = FontStyle.Italic),
                 color = color
             )
             Spacer(modifier = Modifier.width(4.dp))
         }

        Text(
            text = MessageTimeFormatter.format(message.createdAt),
            style = BorshchevykTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = color
        )

        if (isFromMe) {
            Spacer(modifier = Modifier.width(4.dp))
            StatusIcon(message.status, color, onResend)
        }
    }
}

/**
 * Renders the status icon of a message.
 *
 * @param status The current status of the message.
 * @param color The color of the status icon.
 * @param onResend The callback to resend the message.
 */
@Composable
private fun StatusIcon(status: MessageStatus?, color: Color, onResend: () -> Unit) {
    when (status) {
        MessageStatus.SENDING -> androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.dp, color = color)
        MessageStatus.READ -> Icon(Icons.Default.DoneAll, contentDescription = "Read", modifier = Modifier.size(14.dp), tint = color)
        MessageStatus.ERROR -> Icon(Icons.Default.ErrorOutline, contentDescription = "Retry", modifier = Modifier.size(14.dp).clickable { onResend() }, tint = BorshchevykTheme.colors.error)
        else -> Icon(Icons.Default.Done, contentDescription = "Sent", modifier = Modifier.size(14.dp), tint = color)
    }
}

@Composable
private fun MessageDropdownMenu(
    showMenu: Boolean,
    onDismiss: () -> Unit,
    message: Message,
    isFromMe: Boolean,
    onForward: () -> Unit,
    onPinToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: (Boolean) -> Unit,
    onViewReaders: () -> Unit,
    onViewComments: () -> Unit,
    onReactionToggle: (String) -> Unit
) {
    DropdownMenu(
        expanded = showMenu,
        onDismissRequest = onDismiss,
        containerColor = BorshchevykTheme.colors.surface
    ) {
        if (message.updatedAt != null && message.updatedAt != message.createdAt) {
            DropdownMenuItem(
                text = { 
                    Text(
                        text = "Отредактировано: ${MessageTimeFormatter.format(message.updatedAt!!)}",
                        style = BorshchevykTheme.typography.labelSmall, 
                        color = BorshchevykTheme.colors.onSurfaceVariant
                    ) 
                },
                onClick = { },
                enabled = false
            )
            HorizontalDivider(color = BorshchevykTheme.colors.outline.copy(alpha = 0.5f))
        }

        DropdownMenuItem(text = { Text("Forward") }, onClick = { onDismiss(); onForward() })
        DropdownMenuItem(text = { Text(if (message.isPinned) "Unpin" else "Pin") }, onClick = { onDismiss(); onPinToggle() })
        if (isFromMe) {
            DropdownMenuItem(text = { Text("Edit") }, onClick = { onDismiss(); onEdit() })
            DropdownMenuItem(text = { Text("Delete for Everyone") }, onClick = { onDismiss(); onDelete(true) })
        }
        DropdownMenuItem(text = { Text("Delete for Me") }, onClick = { onDismiss(); onDelete(false) })
        if (isFromMe) DropdownMenuItem(text = { Text("View Readers") }, onClick = { onDismiss(); onViewReaders() })
        DropdownMenuItem(text = { Text("Comments (${message.commentsCount})") }, onClick = { onDismiss(); onViewComments() })

        HorizontalDivider(color = BorshchevykTheme.colors.outline.copy(alpha = 0.5f))
        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            listOf("👍", "❤️", "😂", "😢", "🔥").forEach { emoji ->
                Text(
                    text = emoji,
                    modifier = Modifier.clickable { onDismiss(); onReactionToggle(emoji) }.padding(8.dp),
                    fontSize = 20.sp
                )
            }
        }
    }
}

@Composable
private fun ReactionList(
    reactions: List<ru.kubsu.borshchevyk.core.model.domain.MessageReaction>,
    currentUserId: String,
    isFromMe: Boolean,
    onReactionToggle: (String) -> Unit
) {
    val reactionCounts = remember(reactions) { reactions.groupBy { it.reaction }.mapValues { it.value.size } }
    Row(
        modifier = Modifier.padding(top = 4.dp, start = if (isFromMe) 0.dp else 8.dp, end = if (isFromMe) 8.dp else 0.dp),
        horizontalArrangement = if (isFromMe) Arrangement.End else Arrangement.Start
    ) {
        reactionCounts.forEach { (emoji, count) ->
            val iReacted = reactions.any { it.reaction == emoji && it.userId == currentUserId }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (iReacted) BorshchevykTheme.colors.primaryContainer else BorshchevykTheme.colors.surfaceVariant,
                border = if (iReacted) androidx.compose.foundation.BorderStroke(1.dp, BorshchevykTheme.colors.primary) else null,
                modifier = Modifier.padding(end = 4.dp).clickable { onReactionToggle(emoji) }
            ) {
                Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = emoji, fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = count.toString(), style = BorshchevykTheme.typography.labelSmall, color = if (iReacted) BorshchevykTheme.colors.primary else BorshchevykTheme.colors.onSurfaceVariant)
                }
            }
        }
    }
}
