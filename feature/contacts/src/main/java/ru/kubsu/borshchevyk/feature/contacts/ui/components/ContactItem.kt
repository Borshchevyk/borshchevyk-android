package ru.kubsu.borshchevyk.feature.contacts.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.kubsu.borshchevyk.core.model.domain.Contact
import ru.kubsu.borshchevyk.core.ui.theme.BorshchevykTheme

/**
 * A Jetpack Compose component that displays a single contact item.
 *
 * This component renders the contact's avatar (or initial), display name, and a delete button.
 * It provides callbacks for clicking the entire item or specifically the delete button.
 *
 * @param contact The contact information to display.
 * @param onClick Callback invoked when the contact item is clicked.
 * @param onDeleteClick Callback invoked when the delete icon is clicked.
 */
@Composable
internal fun ContactItem(
    contact: Contact,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val displayName = "${contact.contactFirstName ?: ""} ${contact.contactLastName ?: ""}".trim().ifBlank { "Unknown Contact" }
    val initial = displayName.firstOrNull()?.uppercase() ?: "?"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(BorshchevykTheme.colors.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                style = BorshchevykTheme.typography.titleMedium,
                color = BorshchevykTheme.colors.primary
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayName,
                style = BorshchevykTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = BorshchevykTheme.colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        IconButton(onClick = onDeleteClick) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete Contact",
                tint = BorshchevykTheme.colors.error
            )
        }
    }
}