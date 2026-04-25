package ru.kubsu.borshchevyk.feature.chat.ui.chat.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.kubsu.borshchevyk.core.model.domain.Message
import ru.kubsu.borshchevyk.core.model.domain.User
import ru.kubsu.borshchevyk.core.ui.theme.BorshchevykTheme

@Composable
internal fun ReadersDialog(
    readers: List<User>?,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BorshchevykTheme.colors.surface,
        title = { Text("Read by", color = BorshchevykTheme.colors.onSurface) },
        text = {
            if (readers == null) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BorshchevykTheme.colors.primary)
                }
            } else if (readers.isEmpty()) {
                Text("No one has read this yet.", color = BorshchevykTheme.colors.onSurfaceVariant)
            } else {
                LazyColumn {
                    items(readers) { reader ->
                        val displayName = "${reader.firstName} ${reader.lastName ?: ""}".trim().ifBlank { reader.tag }
                        Text(
                            text = displayName,
                            style = BorshchevykTheme.typography.bodyMedium,
                            color = BorshchevykTheme.colors.onSurface,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
internal fun CommentsDialog(
    comments: List<Message>?,
    resolvedUsers: Map<String, User>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BorshchevykTheme.colors.surface,
        title = { Text("Comments", color = BorshchevykTheme.colors.onSurface) },
        text = {
            if (comments == null) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BorshchevykTheme.colors.primary)
                }
            } else if (comments.isEmpty()) {
                Text("No comments yet.", color = BorshchevykTheme.colors.onSurfaceVariant)
            } else {
                LazyColumn {
                    items(comments) { comment ->
                        val user = resolvedUsers[comment.authorId]
                        val displayName = user?.let { "${it.firstName} ${it.lastName ?: ""}".trim() } ?: "User (${comment.authorId})"
                        Column(Modifier.padding(vertical = 8.dp)) {
                            Text(
                                text = displayName,
                                style = BorshchevykTheme.typography.labelSmall,
                                color = BorshchevykTheme.colors.primary
                            )
                            Text(
                                text = comment.text,
                                style = BorshchevykTheme.typography.bodyMedium,
                                color = BorshchevykTheme.colors.onSurface
                            )
                        }
                        HorizontalDivider(color = BorshchevykTheme.colors.outline.copy(alpha = 0.2f))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
