package ru.kubsu.borshchevyk.feature.chat.conversation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.kubsu.borshchevyk.core.model.domain.ForwardPayload
import ru.kubsu.borshchevyk.core.ui.theme.BorshchevykTheme

@Composable
internal fun ForwardPayloadBanner(
    forwardPayload: ForwardPayload
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BorshchevykTheme.colors.primaryContainer)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Forwarding from ${forwardPayload.authorName}", 
            style = BorshchevykTheme.typography.bodyMedium, 
            color = BorshchevykTheme.colors.onPrimaryContainer
        )
    }
}

@Composable
internal fun EditingMessageBanner(
    onCancelEdit: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BorshchevykTheme.colors.surface)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Editing message", 
            style = BorshchevykTheme.typography.bodyMedium, 
            color = BorshchevykTheme.colors.primary
        )
        IconButton(onClick = onCancelEdit, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Cancel edit", tint = BorshchevykTheme.colors.onSurfaceVariant)
        }
    }
}
