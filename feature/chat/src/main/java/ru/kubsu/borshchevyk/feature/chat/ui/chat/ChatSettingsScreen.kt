package ru.kubsu.borshchevyk.feature.chat.ui.chat

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.kubsu.borshchevyk.core.model.domain.ChatMember
import ru.kubsu.borshchevyk.core.model.domain.ChatMemberRole
import ru.kubsu.borshchevyk.core.model.dto.UpdatePermissionsRequest
import ru.kubsu.borshchevyk.core.ui.theme.BorshchevykTheme
import ru.kubsu.borshchevyk.feature.chat.ChatSettingsUiState
import ru.kubsu.borshchevyk.feature.chat.ui.chat.components.ClearHistoryDialog
import ru.kubsu.borshchevyk.feature.chat.ui.chat.components.DeleteChatDialog
import ru.kubsu.borshchevyk.feature.chat.ui.chat.components.InviteUserDialog
import ru.kubsu.borshchevyk.feature.chat.ui.chat.components.UpdateChatInfoDialog
import ru.kubsu.borshchevyk.feature.chat.ui.chat.components.UpdatePermissionsDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChatSettingsScreen(
    uiState: ChatSettingsUiState,
    onBackClick: () -> Unit,
    onInvite: (String) -> Unit,
    onGenerateLink: () -> Unit,
    onUpdatePermissions: (String, UpdatePermissionsRequest) -> Unit,
    onClearHistory: (Boolean) -> Unit,
    onDeleteChat: () -> Unit,
    onKickUser: (String) -> Unit,
    onLeaveChat: () -> Unit,
    onUpdateChatInfo: (String?, String?) -> Unit
) {
    var showInviteDialog by rememberSaveable { mutableStateOf(false) }
    var memberIdForPermissions by rememberSaveable { mutableStateOf<String?>(null) }
    var showClearHistoryDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteChatDialog by rememberSaveable { mutableStateOf(false) }
    var showUpdateInfoDialog by rememberSaveable { mutableStateOf(false) }

    val currentUserMember = uiState.members.find { it.userId == uiState.currentUserId }
    val canManagePermissions = uiState.isGroupChat &&
            (currentUserMember?.role == ChatMemberRole.OWNER || currentUserMember?.role == ChatMemberRole.ADMIN)
    val canChangeInfo = uiState.isGroupChat && (currentUserMember?.canChangeInfo == true || canManagePermissions)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat Settings", style = BorshchevykTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            item {
                ChatSettingsHeader(
                    uiState = uiState,
                    canChangeInfo = canChangeInfo,
                    onShowUpdateInfo = { showUpdateInfoDialog = true },
                    onShowInvite = { showInviteDialog = true },
                    onGenerateLink = onGenerateLink
                )
            }
            
            items(uiState.members) { member ->
                MemberItemRow(
                    member = member,
                    currentUserId = uiState.currentUserId,
                    canManagePermissions = canManagePermissions,
                    onMemberClick = { memberIdForPermissions = member.userId }
                )
            }

            item {
                ChatSettingsDangerZone(
                    uiState = uiState,
                    currentUserMember = currentUserMember,
                    onShowClearHistory = { showClearHistoryDialog = true },
                    onShowDeleteChat = { showDeleteChatDialog = true },
                    onLeaveChat = onLeaveChat
                )
            }
        }

        // Dialogs
        if (showClearHistoryDialog) {
            ClearHistoryDialog(
                isGroupChat = uiState.isGroupChat,
                onDismiss = { showClearHistoryDialog = false },
                onConfirm = { forAll -> 
                    onClearHistory(forAll)
                    showClearHistoryDialog = false 
                }
            )
        }

        if (showDeleteChatDialog) {
            DeleteChatDialog(
                onDismiss = { showDeleteChatDialog = false },
                onConfirm = { 
                    onDeleteChat()
                    showDeleteChatDialog = false 
                }
            )
        }

        if (showInviteDialog) {
            InviteUserDialog(
                onDismiss = { showInviteDialog = false },
                onConfirm = { userId -> 
                    onInvite(userId)
                    showInviteDialog = false 
                }
            )
        }

        memberIdForPermissions?.let { memberId ->
            uiState.members.find { it.userId == memberId }?.let { member ->
                UpdatePermissionsDialog(
                    member = member,
                    currentUserId = uiState.currentUserId,
                    canManagePermissions = canManagePermissions,
                    onDismiss = { memberIdForPermissions = null },
                    onConfirm = { userId, req ->
                        onUpdatePermissions(userId, req)
                        memberIdForPermissions = null
                    },
                    onKickUser = { userId ->
                        onKickUser(userId)
                        memberIdForPermissions = null
                    }
                )
            } ?: run { memberIdForPermissions = null }
        }

        if (showUpdateInfoDialog) {
            UpdateChatInfoDialog(
                onDismiss = { showUpdateInfoDialog = false },
                onConfirm = { title, desc ->
                    onUpdateChatInfo(title, desc)
                    showUpdateInfoDialog = false
                }
            )
        }
    }
}

@Composable
private fun ChatSettingsHeader(
    uiState: ChatSettingsUiState,
    canChangeInfo: Boolean,
    onShowUpdateInfo: () -> Unit,
    onShowInvite: () -> Unit,
    onGenerateLink: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Spacer(modifier = Modifier.height(16.dp))
    if (canChangeInfo) {
        Button(
            onClick = onShowUpdateInfo,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Edit Chat Info")
        }
        Spacer(modifier = Modifier.height(8.dp))
    }

    if (uiState.isGroupChat) {
        Button(
            onClick = onShowInvite,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = BorshchevykTheme.colors.primary)
        ) {
            Text("Invite User")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onGenerateLink,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = BorshchevykTheme.colors.primaryContainer, contentColor = BorshchevykTheme.colors.primary)
        ) {
            Text("Generate Invite Link")
        }

        if (uiState.inviteLink != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable {
                        clipboardManager.setText(AnnotatedString(uiState.inviteLink))
                        Toast.makeText(context, "Link copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Link: ${uiState.inviteLink}",
                    style = BorshchevykTheme.typography.bodyMedium,
                    color = BorshchevykTheme.colors.onSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy Link",
                    tint = BorshchevykTheme.colors.primary,
                    modifier = Modifier.size(20.dp).padding(start = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
    Text("Members (${uiState.members.size})", style = BorshchevykTheme.typography.titleMedium, color = BorshchevykTheme.colors.onSurface)
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun MemberItemRow(
    member: ChatMember,
    currentUserId: String,
    canManagePermissions: Boolean,
    onMemberClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = canManagePermissions && member.userId != currentUserId) {
                onMemberClick()
            }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = if (member.userId == currentUserId) "You (${member.userId})" else "User: ${member.userId}",
                style = BorshchevykTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = BorshchevykTheme.colors.onSurface
            )
            Text(
                text = "Role: ${member.role}",
                style = BorshchevykTheme.typography.bodyMedium,
                color = BorshchevykTheme.colors.onSurfaceVariant
            )
        }
        if (canManagePermissions && member.userId != currentUserId) {
            Icon(Icons.Default.Settings, contentDescription = "Edit Permissions", tint = BorshchevykTheme.colors.primary, modifier = Modifier.size(20.dp))
        }
    }
    HorizontalDivider(color = BorshchevykTheme.colors.outline.copy(alpha = 0.5f))
}

@Composable
private fun ChatSettingsDangerZone(
    uiState: ChatSettingsUiState,
    currentUserMember: ChatMember?,
    onShowClearHistory: () -> Unit,
    onShowDeleteChat: () -> Unit,
    onLeaveChat: () -> Unit
) {
    Spacer(modifier = Modifier.height(32.dp))

    Button(
        onClick = onShowClearHistory,
        colors = ButtonDefaults.buttonColors(containerColor = BorshchevykTheme.colors.error),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Clear History", color = BorshchevykTheme.colors.onError)
    }

    Spacer(modifier = Modifier.height(8.dp))

    if (uiState.isGroupChat) {
        Button(
            onClick = onLeaveChat,
            colors = ButtonDefaults.buttonColors(containerColor = BorshchevykTheme.colors.error),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Leave Chat", color = BorshchevykTheme.colors.onError)
        }
        Spacer(modifier = Modifier.height(8.dp))
    }

    if (!uiState.isGroupChat || currentUserMember?.role == ChatMemberRole.OWNER) {
        Button(
            onClick = onShowDeleteChat,
            colors = ButtonDefaults.buttonColors(containerColor = BorshchevykTheme.colors.error),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Delete Chat", color = BorshchevykTheme.colors.onError)
        }
    }

    Spacer(modifier = Modifier.height(32.dp))
}