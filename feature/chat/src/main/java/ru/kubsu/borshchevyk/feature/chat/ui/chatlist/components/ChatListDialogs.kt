package ru.kubsu.borshchevyk.feature.chat.ui.chatlist.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.kubsu.borshchevyk.core.ui.theme.BorshchevykTheme

@Composable
internal fun CreateChatDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var peerId by rememberSaveable { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BorshchevykTheme.colors.surface,
        titleContentColor = BorshchevykTheme.colors.onSurface,
        textContentColor = BorshchevykTheme.colors.onSurfaceVariant,
        title = { 
            Text(
                "Start new private chat",
                style = BorshchevykTheme.typography.titleMedium
            ) 
        },
        text = {
            OutlinedTextField(
                value = peerId,
                onValueChange = { peerId = it },
                label = { Text("User ID") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BorshchevykTheme.colors.primary,
                    unfocusedBorderColor = BorshchevykTheme.colors.outline,
                    focusedTextColor = BorshchevykTheme.colors.onSurface,
                    unfocusedTextColor = BorshchevykTheme.colors.onSurface,
                    cursorColor = BorshchevykTheme.colors.primary
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = { onCreate(peerId) },
                enabled = peerId.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BorshchevykTheme.colors.primary,
                    contentColor = BorshchevykTheme.colors.onPrimary
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = BorshchevykTheme.colors.onSurfaceVariant
                )
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
internal fun CreateGroupChatDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String?) -> Unit
) {
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BorshchevykTheme.colors.surface,
        titleContentColor = BorshchevykTheme.colors.onSurface,
        textContentColor = BorshchevykTheme.colors.onSurfaceVariant,
        title = { Text("Create Group Chat", style = BorshchevykTheme.typography.titleMedium) },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BorshchevykTheme.colors.primary,
                        unfocusedBorderColor = BorshchevykTheme.colors.outline
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BorshchevykTheme.colors.primary,
                        unfocusedBorderColor = BorshchevykTheme.colors.outline
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(title, description.ifBlank { null }) },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BorshchevykTheme.colors.primary
                )
            ) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
internal fun JoinChatDialog(
    onDismiss: () -> Unit,
    onJoin: (String) -> Unit
) {
    var link by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BorshchevykTheme.colors.surface,
        titleContentColor = BorshchevykTheme.colors.onSurface,
        textContentColor = BorshchevykTheme.colors.onSurfaceVariant,
        title = { Text("Join via Link", style = BorshchevykTheme.typography.titleMedium) },
        text = {
            OutlinedTextField(
                value = link,
                onValueChange = { link = it },
                label = { Text("Invite Code") },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BorshchevykTheme.colors.primary,
                    unfocusedBorderColor = BorshchevykTheme.colors.outline
                )
            )
        },
        confirmButton = {
            Button(
                onClick = { onJoin(link) },
                enabled = link.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BorshchevykTheme.colors.primary
                )
            ) { Text("Join") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
