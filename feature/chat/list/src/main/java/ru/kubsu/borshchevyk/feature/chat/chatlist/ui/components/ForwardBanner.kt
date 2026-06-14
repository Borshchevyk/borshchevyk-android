package ru.kubsu.borshchevyk.feature.chat.chatlist.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.kubsu.borshchevyk.core.ui.theme.BorshchevykTheme

@Composable
internal fun ForwardBanner(
    onCancelForward: () -> Unit
) {
    Surface(
        color = BorshchevykTheme.colors.primaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Select chat to forward message",
                style = BorshchevykTheme.typography.bodyMedium,
                color = BorshchevykTheme.colors.onPrimaryContainer,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onCancelForward) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cancel forwarding",
                    tint = BorshchevykTheme.colors.onPrimaryContainer
                )
            }
        }
    }
}
