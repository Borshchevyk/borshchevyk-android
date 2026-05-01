package ru.kubsu.borshchevyk.feature.profile.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import ru.kubsu.borshchevyk.core.model.domain.Visibility
import ru.kubsu.borshchevyk.core.ui.theme.BorshchevykTheme

/**
 * A UI component that displays a title and a row of filter chips representing 
 * different [Visibility] levels (e.g., EVERYONE, CONTACTS, NOBODY).
 *
 * @param title The label for this privacy setting.
 * @param currentValue The currently selected visibility level.
 * @param onValueChange Callback invoked when a new visibility level is selected.
 */
@Composable
fun PrivacyOption(
    title: String,
    currentValue: Visibility,
    onValueChange: (Visibility) -> Unit
) {
    Column {
        Text(text = title, style = BorshchevykTheme.typography.titleMedium, color = BorshchevykTheme.colors.onSurface)
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Visibility.entries.forEach { visibility ->
                val isSelected = visibility == currentValue
                FilterChip(
                    selected = isSelected,
                    onClick = { onValueChange(visibility) },
                    label = { Text(visibility.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BorshchevykTheme.colors.primaryContainer,
                        selectedLabelColor = BorshchevykTheme.colors.primary
                    )
                )
            }
        }
    }
}

/**
 * Displays an individual item within a settings list.
 *
 * @param icon The icon to display alongside the setting title.
 * @param title The text label for the setting.
 * @param onClick Callback invoked when the settings item is clicked.
 * @param isDestructive Indicates whether this action is destructive (e.g., deleting an account), applying appropriate styling. Defaults to false.
 */
@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val contentColor = if (isDestructive) BorshchevykTheme.colors.error else BorshchevykTheme.colors.onSurfaceVariant
        val titleColor = if (isDestructive) BorshchevykTheme.colors.error else BorshchevykTheme.colors.onSurface

        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Text(
            text = title,
            style = BorshchevykTheme.typography.bodyLarge,
            color = titleColor
        )
    }
}
