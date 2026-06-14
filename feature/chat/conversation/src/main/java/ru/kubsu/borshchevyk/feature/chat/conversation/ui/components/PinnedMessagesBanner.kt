package ru.kubsu.borshchevyk.feature.chat.conversation.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.kubsu.borshchevyk.core.model.domain.Message
import ru.kubsu.borshchevyk.core.ui.theme.BorshchevykTheme

@Composable
internal fun PinnedMessagesBanner(
    messages: List<Message>,
    onUnpinClick: (Message) -> Unit
) {
    val message = messages.lastOrNull() ?: return
    Surface(
        color = BorshchevykTheme.colors.surfaceVariant,
        modifier = Modifier
//            .padding(top = 8.dp)
//            .padding(horizontal = 8.dp)
//            .clip(RoundedCornerShape(16.dp))
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.PushPin, contentDescription = "Pinned", tint = BorshchevykTheme.colors.primary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Pinned Message", style = BorshchevykTheme.typography.labelSmall, color = BorshchevykTheme.colors.primary)
                Text(text = message.text, style = BorshchevykTheme.typography.bodyMedium, color = BorshchevykTheme.colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
