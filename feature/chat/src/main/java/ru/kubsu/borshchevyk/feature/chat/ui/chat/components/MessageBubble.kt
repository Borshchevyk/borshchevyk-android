package ru.kubsu.borshchevyk.feature.chat.ui.chat.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.kubsu.borshchevyk.core.model.domain.Message
import ru.kubsu.borshchevyk.core.model.domain.User
import ru.kubsu.borshchevyk.core.ui.theme.BorshchevykTheme

private fun formatMessageTime(timeStr: String): String {
    return try {
        val cleanStr = timeStr.removeSuffix("Z")
        val ldt = java.time.LocalDateTime.parse(cleanStr)
        val instant = ldt.toInstant(java.time.ZoneOffset.UTC)
        val localTime = java.time.LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault())
        localTime.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
    } catch (e: Exception) {
        timeStr.substringAfter("T").substringBeforeLast(":")
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun MessageBubble(
    message: Message,
    isFromMe: Boolean,
    currentUserId: String,
    attachmentUrls: Map<String, String>,
    resolvedUsers: Map<String, User>,
    onResolveAttachmentUrl: (String) -> Unit,
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
    val author = resolvedUsers[message.authorId]
    val authorName = author?.let { "${it.firstName} ${it.lastName ?: ""}".trim() } ?: "User"

    LaunchedEffect(message.id) {
        if (!isFromMe) {
            onMessageVisible()
        }
    }
    
    val bubbleShape = if (isFromMe) {
        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 4.dp, bottomEnd = 20.dp)
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        horizontalAlignment = if (isFromMe) Alignment.End else Alignment.Start
    ) {
        if (!isFromMe) {
            Text(
                text = authorName,
                style = BorshchevykTheme.typography.labelSmall,
                color = BorshchevykTheme.colors.primary,
                modifier = Modifier.padding(start = 12.dp, bottom = 4.dp)
            )
        }

        Box {
            Surface(
                color = if (isFromMe) BorshchevykTheme.colors.primary else BorshchevykTheme.colors.surfaceVariant,
                shape = bubbleShape,
                shadowElevation = 2.dp,
                modifier = Modifier.combinedClickable(
                    onClick = {},
                    onLongClick = { showMenu = true }
                )
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    if (message.forwardedFromUserId != null) {
                        val forwardedAuthor = resolvedUsers[message.forwardedFromUserId]
                        val forwardedName = forwardedAuthor?.let { "${it.firstName} ${it.lastName ?: ""}".trim() } ?: "User"
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Forwarded",
                                modifier = Modifier.size(12.dp),
                                tint = if (isFromMe) BorshchevykTheme.colors.onPrimary.copy(alpha = 0.7f) else BorshchevykTheme.colors.primary.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Forwarded from $forwardedName",
                                style = BorshchevykTheme.typography.labelSmall.copy(fontStyle = FontStyle.Italic),
                                color = if (isFromMe) BorshchevykTheme.colors.onPrimary.copy(alpha = 0.7f) else BorshchevykTheme.colors.primary.copy(alpha = 0.7f)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    if (message.attachments.isNotEmpty()) {
                        AttachmentGallery(
                            attachments = message.attachments,
                            attachmentUrls = attachmentUrls,
                            onResolveAttachmentUrl = onResolveAttachmentUrl,
                            isFromMe = isFromMe
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (message.text.isNotBlank()) {
                        Text(
                            text = message.text,
                            style = BorshchevykTheme.typography.bodyLarge,
                            color = if (isFromMe) BorshchevykTheme.colors.onPrimary else BorshchevykTheme.colors.onSurface
                        )
                    }

                    Row(
                        modifier = Modifier.padding(top = 4.dp).align(Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isEdited = message.updatedAt != null && message.updatedAt != message.createdAt
                        if (isEdited) {
                            Text(
                                text = "edited",
                                style = BorshchevykTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = if (isFromMe) BorshchevykTheme.colors.onPrimary.copy(alpha = 0.5f) else BorshchevykTheme.colors.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        
                        Text(
                            text = formatMessageTime(message.createdAt),
                            style = BorshchevykTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = if (isFromMe) BorshchevykTheme.colors.onPrimary.copy(alpha = 0.7f) else BorshchevykTheme.colors.onSurfaceVariant.copy(alpha = 0.7f)
                        )

                        if (isFromMe) {
                            Spacer(modifier = Modifier.width(4.dp))
                            when (message.status) {
                                ru.kubsu.borshchevyk.core.model.domain.MessageStatus.SENDING -> {
                                    androidx.compose.material3.CircularProgressIndicator(
                                        modifier = Modifier.size(12.dp),
                                        strokeWidth = 1.dp,
                                        color = BorshchevykTheme.colors.onPrimary.copy(alpha = 0.7f)
                                    )
                                }
                                ru.kubsu.borshchevyk.core.model.domain.MessageStatus.READ -> {
                                    Icon(
                                        imageVector = Icons.Default.DoneAll,
                                        contentDescription = "Read",
                                        modifier = Modifier.size(14.dp),
                                        tint = BorshchevykTheme.colors.onPrimary
                                    )
                                }
                                ru.kubsu.borshchevyk.core.model.domain.MessageStatus.ERROR -> {
                                    Icon(
                                        imageVector = Icons.Default.ErrorOutline,
                                        contentDescription = "Error, tap to retry",
                                        modifier = Modifier.size(14.dp).clickable { onResend() },
                                        tint = BorshchevykTheme.colors.error
                                    )
                                }
                                else -> {
                                    Icon(
                                        imageVector = Icons.Default.Done,
                                        contentDescription = "Sent",
                                        modifier = Modifier.size(14.dp),
                                        tint = BorshchevykTheme.colors.onPrimary.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                containerColor = BorshchevykTheme.colors.surface
            ) {
                DropdownMenuItem(
                    text = { Text("Forward Message") },
                    onClick = {
                        showMenu = false
                        onForward()
                    }
                )
                DropdownMenuItem(
                    text = { Text(if (message.isPinned) "Unpin Message" else "Pin Message") },
                    onClick = {
                        showMenu = false
                        onPinToggle()
                    }
                )
                if (isFromMe) {
                    DropdownMenuItem(
                        text = { Text("Edit Message") },
                        onClick = {
                            showMenu = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete for Everyone") },
                        onClick = {
                            showMenu = false
                            onDelete(true)
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Delete for Me") },
                    onClick = {
                        showMenu = false
                        onDelete(false)
                    }
                )
                if (isFromMe) {
                    DropdownMenuItem(
                        text = { Text("View Readers") },
                        onClick = {
                            showMenu = false
                            onViewReaders()
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text("View Comments (${message.commentsCount})") },
                    onClick = {
                        showMenu = false
                        onViewComments()
                    }
                )

                HorizontalDivider(color = BorshchevykTheme.colors.outline.copy(alpha = 0.5f))
                val reactions = listOf("👍", "❤️", "😂", "😢", "🔥")
                Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                    reactions.forEach { emoji ->
                        Text(
                            text = emoji,
                            modifier = Modifier
                                .clickable {
                                    showMenu = false
                                    onReactionToggle(emoji)
                                }
                                .padding(8.dp),
                            fontSize = 20.sp
                        )
                    }
                }
            }
        }

        if (message.reactions.isNotEmpty()) {
            val reactionCounts = message.reactions.groupBy { it.reaction }.mapValues { it.value.size }
            Row(
                modifier = Modifier.padding(top = 4.dp, start = if (isFromMe) 0.dp else 8.dp, end = if (isFromMe) 8.dp else 0.dp),
                horizontalArrangement = if (isFromMe) Arrangement.End else Arrangement.Start
            ) {
                reactionCounts.forEach { (emoji, count) ->
                    val iReacted = message.reactions.any { it.reaction == emoji && it.userId == currentUserId }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (iReacted) BorshchevykTheme.colors.primaryContainer else BorshchevykTheme.colors.surfaceVariant,
                        border = if (iReacted) androidx.compose.foundation.BorderStroke(1.dp, BorshchevykTheme.colors.primary) else null,
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .clickable { onReactionToggle(emoji) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = emoji, fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = count.toString(),
                                style = BorshchevykTheme.typography.labelSmall,
                                color = if (iReacted) BorshchevykTheme.colors.primary else BorshchevykTheme.colors.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
