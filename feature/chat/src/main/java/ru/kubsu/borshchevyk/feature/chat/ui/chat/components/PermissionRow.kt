package ru.kubsu.borshchevyk.feature.chat.ui.chat.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.kubsu.borshchevyk.core.ui.theme.BorshchevykTheme

/**
 * A row component displaying a permission toggle with a checkbox and label.
 *
 * @param label The text label for the permission.
 * @param checked The current checked state of the permission.
 * @param onCheckedChange Callback invoked when the checked state changes.
 */
@Composable
fun PermissionRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(checkedColor = BorshchevykTheme.colors.primary)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, style = BorshchevykTheme.typography.bodyLarge, color = BorshchevykTheme.colors.onSurface)
    }
}
