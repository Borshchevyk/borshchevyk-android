package ru.kubsu.borshchevyk.feature.chat.settings.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.kubsu.borshchevyk.core.model.domain.ChatMember
import ru.kubsu.borshchevyk.core.ui.theme.BorshchevykTheme
import ru.kubsu.borshchevyk.feature.chat.conversation.ui.components.PermissionRow

@Composable
internal fun ClearHistoryDialog(
    isGroupChat: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Boolean) -> Unit
) {
    var forAll by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Clear History") },
        text = {
            Column {
                Text("Are you sure you want to clear the history of this chat? This action cannot be undone.")
                if (!isGroupChat) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 16.dp)) {
                        Checkbox(checked = forAll, onCheckedChange = { forAll = it })
                        Text("Clear for everyone")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(forAll) },
                colors = ButtonDefaults.buttonColors(containerColor = BorshchevykTheme.colors.error)
            ) { Text("Clear", color = BorshchevykTheme.colors.onError) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
internal fun DeleteChatDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Chat") },
        text = { Text("Are you sure you want to delete this chat? This action cannot be undone.") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = BorshchevykTheme.colors.error)
            ) { Text("Delete", color = BorshchevykTheme.colors.onError) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
internal fun InviteUserDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var userIdToInvite by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BorshchevykTheme.colors.surface,
        title = { Text("Invite User", color = BorshchevykTheme.colors.onSurface) },
        text = {
            OutlinedTextField(
                value = userIdToInvite,
                onValueChange = { userIdToInvite = it },
                label = { Text("User ID") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BorshchevykTheme.colors.primary,
                    unfocusedBorderColor = BorshchevykTheme.colors.outline
                )
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(userIdToInvite) },
                enabled = userIdToInvite.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = BorshchevykTheme.colors.primary)
            ) { Text("Invite") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
internal fun UpdatePermissionsDialog(
    member: ChatMember,
    currentUserId: String,
    canManagePermissions: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, Boolean, Boolean, Boolean, Boolean) -> Unit,
    onKickUser: (String) -> Unit
) {
    var canSend by remember { mutableStateOf(member.canSendMessages) }
    var canDelete by remember { mutableStateOf(member.canDeleteMessages) }
    var canInvite by remember { mutableStateOf(member.canInviteUsers) }
    var canChangeInfo by remember { mutableStateOf(member.canChangeInfo) }

    val displayName = member.user?.let { "${it.firstName ?: ""} ${it.lastName ?: ""}".trim().ifBlank { it.tag } } ?: "User"

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BorshchevykTheme.colors.surface,
        title = { Text("Permissions for $displayName", style = BorshchevykTheme.typography.titleMedium, color = BorshchevykTheme.colors.onSurface) },
        text = {
            Column {
                PermissionRow("Send Messages", canSend) { canSend = it }
                PermissionRow("Delete Messages", canDelete) { canDelete = it }
                PermissionRow("Invite Users", canInvite) { canInvite = it }
                PermissionRow("Change Chat Info", canChangeInfo) { canChangeInfo = it }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        member.userId,
                        canSend,
                        canDelete,
                        canInvite,
                        canChangeInfo
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = BorshchevykTheme.colors.primary)
            ) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (canManagePermissions && member.userId != currentUserId) {
                    TextButton(
                        onClick = { onKickUser(member.userId) },
                        colors = ButtonDefaults.textButtonColors(contentColor = BorshchevykTheme.colors.error)
                    ) { Text("Kick User") }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

@Composable
internal fun AddContactDialog(
    initialFirstName: String?,
    initialLastName: String?,
    onDismiss: () -> Unit,
    onConfirm: (String, String?) -> Unit
) {
    var firstName by remember { mutableStateOf(initialFirstName ?: "") }
    var lastName by remember { mutableStateOf(initialLastName ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Contact") },
        text = {
            Column {
                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = { Text("First Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = { Text("Last Name (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(firstName, lastName) },
                enabled = firstName.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
internal fun UpdateChatInfoDialog(
    onDismiss: () -> Unit,
    onConfirm: (String?, String?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update Chat Info") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        title.ifBlank { null },
                        description.ifBlank { null }
                    )
                }
            ) { Text("Update") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
