package ru.kubsu.borshchevyk.feature.auth.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import ru.kubsu.borshchevyk.core.ui.theme.BorshchevykTheme

@Composable
fun TabButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) BorshchevykTheme.colors.background else Color.Transparent,
            contentColor = if (isSelected) BorshchevykTheme.colors.primary else BorshchevykTheme.colors.onSurfaceVariant
        ),
        shape = RoundedCornerShape(8.dp),
        elevation = if (isSelected) ButtonDefaults.buttonElevation(defaultElevation = 2.dp) else null,
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        Text(
            text = text, 
            style = BorshchevykTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
        )
    }
}

@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    isPassword: Boolean = false,
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            isError = error != null,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BorshchevykTheme.colors.primary,
                unfocusedBorderColor = BorshchevykTheme.colors.outline,
                errorBorderColor = MaterialTheme.colorScheme.error,
                focusedTextColor = BorshchevykTheme.colors.onSurface,
                unfocusedTextColor = BorshchevykTheme.colors.onSurface,
                cursorColor = BorshchevykTheme.colors.primary,
                focusedLabelColor = BorshchevykTheme.colors.primary,
                errorLabelColor = MaterialTheme.colorScheme.error
            ),
            singleLine = true
        )
        AnimatedVisibility(visible = error != null) {
            error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }
        }
    }
}
