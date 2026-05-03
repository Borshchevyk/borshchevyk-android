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
import ru.kubsu.borshchevyk.core.network.dto.UpdatePermissionsRequest
import ru.kubsu.borshchevyk.core.ui.theme.BorshchevykTheme
import ru.kubsu.borshchevyk.feature.chat.ChatSettingsUiState
import ru.kubsu.borshchevyk.feature.chat.ChatSharedMediaUiState
import ru.kubsu.borshchevyk.feature.chat.MediaType
import ru.kubsu.borshchevyk.core.model.domain.Message
import ru.kubsu.borshchevyk.feature.chat.ui.chat.components.AddContactDialog
import ru.kubsu.borshchevyk.feature.chat.ui.chat.components.ClearHistoryDialog
import ru.kubsu.borshchevyk.feature.chat.ui.chat.components.DeleteChatDialog
import ru.kubsu.borshchevyk.feature.chat.ui.chat.components.UpdateChatInfoDialog
import ru.kubsu.borshchevyk.feature.chat.ui.chat.components.UpdatePermissionsDialog
import ru.kubsu.borshchevyk.feature.chat.ui.chat.components.ChatSharedMediaSection

/**
 * Screen displaying the settings for a specific chat.
 *
 * @param uiState The current UI state of the chat settings.
 * @param sharedMediaUiState The UI state for shared media.
 * @param onTabSelected Callback invoked when a media tab is selected.
 * @param onLoadNextPage Callback invoked to load next page of media.
 * @param onMessageClick Callback invoked when a media message is clicked.
 * @param onBackClick Callback invoked when the user navigates back.
 * @param onShowInviteSearch Callback invoked to show the user invite search screen.
 * @param onGenerateLink Callback invoked to generate an invite link for the group chat.
 * @param onUpdatePermissions Callback invoked to update a member's permissions.
 * @param onClearHistory Callback invoked to clear the chat history.
 * @param onDeleteChat Callback invoked to delete the chat.
 * @param onKickUser Callback invoked to kick a user from the group chat.
 * @param onLeaveChat Callback invoked when the current user leaves the group chat.
 * @param onUpdateChatInfo Callback invoked to update the chat's title and description.
 * @param onAddContact Callback invoked to add a user to contacts.
 * @param onRemoveContact Callback invoked to remove a user from contacts.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChatSettingsScreen(
    uiState: ChatSettingsUiState,
    sharedMediaUiState: ChatSharedMediaUiState,
    onTabSelected: (MediaType) -> Unit,
    onLoadNextPage: () -> Unit,
    onMessageClick: (Message) -> Unit,
    onResolveSharedMediaUrl: (String, Boolean) -> Unit,
    onBackClick: () -> Unit,
    onShowInviteSearch: () -> Unit,
    onGenerateLink: () -> Unit,
    onUpdatePermissions: (String, Boolean, Boolean, Boolean, Boolean) -> Unit,
    onClearHistory: (Boolean) -> Unit,
    onDeleteChat: () -> Unit,
    onKickUser: (String) -> Unit,
    onLeaveChat: () -> Unit,
    onUpdateChatInfo: (String?, String?) -> Unit,
    onAddContact: (String, String?) -> Unit,
    onRemoveContact: () -> Unit
) {
    var memberIdForPermissions by rememberSaveable { mutableStateOf<String?>(null) }
    var showClearHistoryDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteChatDialog by rememberSaveable { mutableStateOf(false) }
    var showUpdateInfoDialog by rememberSaveable { mutableStateOf(false) }
    var showAddContactDialog by rememberSaveable { mutableStateOf(false) }

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
                    onShowInvite = onShowInviteSearch,
                    onGenerateLink = onGenerateLink,
                    onShowAddContact = { showAddContactDialog = true },
                    onRemoveContact = onRemoveContact
                )
            }
            
            item {
                ChatSharedMediaSection(
                    uiState = sharedMediaUiState,
                    onTabSelected = onTabSelected,
                    onLoadNextPage = onLoadNextPage,
                    onMessageClick = onMessageClick,
                    onResolveUrl = onResolveSharedMediaUrl
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Members (${uiState.members.size})", style = BorshchevykTheme.typography.titleMedium, color = BorshchevykTheme.colors.onSurface)
                Spacer(modifier = Modifier.height(8.dp))
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

        memberIdForPermissions?.let { memberId ->
            uiState.members.find { it.userId == memberId }?.let { member ->
                UpdatePermissionsDialog(
                    member = member,
                    currentUserId = uiState.currentUserId,
                    canManagePermissions = canManagePermissions,
                    onDismiss = { memberIdForPermissions = null },
                    onConfirm = { userId, canSend, canDelete, canInvite, canChange ->
                        onUpdatePermissions(userId, canSend, canDelete, canInvite, canChange)
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
        
        if (showAddContactDialog) {
            AddContactDialog(
                initialFirstName = uiState.partnerFirstName,
                initialLastName = uiState.partnerLastName,
                onDismiss = { showAddContactDialog = false },
                onConfirm = { firstName, lastName ->
                    onAddContact(firstName, lastName)
                    showAddContactDialog = false
                }
            )
        }
    }
}

/**
 * Displays the header section of the chat settings, including options to add/remove contacts,
 * edit chat info, invite users, and generate invite links.
 *
 * @param uiState Current settings UI state.
 * @param canChangeInfo Whether the current user has permission to change chat info.
 * @param onShowUpdateInfo Callback to display the update info dialog.
 * @param onShowInvite Callback to display the invite user dialog.
 * @param onGenerateLink Callback to generate a new invite link.
 * @param onShowAddContact Callback to display the add contact dialog.
 * @param onRemoveContact Callback to remove the user from contacts.
 */
@Composable
private fun ChatSettingsHeader(
    uiState: ChatSettingsUiState,
    canChangeInfo: Boolean,
    onShowUpdateInfo: () -> Unit,
    onShowInvite: () -> Unit,
    onGenerateLink: () -> Unit,
    onShowAddContact: () -> Unit,
    onRemoveContact: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Spacer(modifier = Modifier.height(16.dp))
    
    if (!uiState.isGroupChat && uiState.partnerId != null) {
        if (uiState.isContact) {
            Button(
                onClick = onRemoveContact,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = BorshchevykTheme.colors.error, contentColor = BorshchevykTheme.colors.onError)
            ) {
                Text("Remove from Contacts")
            }
        } else {
            Button(
                onClick = onShowAddContact,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = BorshchevykTheme.colors.primaryContainer, contentColor = BorshchevykTheme.colors.onPrimaryContainer)
            ) {
                Text("Add to Contacts")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
    
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
}

/**
 * Displays a single row for a chat member, showing their name, role, and a settings icon
 * if the current user has permissions to manage them.
 *
 * @param member The chat member to display.
 * @param currentUserId The ID of the current user viewing the settings.
 * @param canManagePermissions Whether the current user can manage permissions for this member.
 * @param onMemberClick Callback invoked when the member row is clicked.
 */
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
            val displayName = if (member.userId == currentUserId) "You" else member.user?.let { "${it.firstName ?: ""} ${it.lastName ?: ""}".trim().ifBlank { it.tag } } ?: "Unknown User"
            Text(
                text = displayName,
                style = BorshchevykTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = BorshchevykTheme.colors.onSurface
            )
            val tagDisplay = if (member.userId != currentUserId) {
                member.user?.tag?.let { "@$it • " } ?: ""
            } else ""
            Text(
                text = "${tagDisplay}Role: ${member.role}",
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

/**
 * The danger zone section containing destructive actions like clearing history, 
 * leaving the chat, or deleting the chat entirely.
 *
 * @param uiState Current settings UI state.
 * @param currentUserMember The current user's membership details in the chat.
 * @param onShowClearHistory Callback to display the clear history confirmation dialog.
 * @param onShowDeleteChat Callback to display the delete chat confirmation dialog.
 * @param onLeaveChat Callback to leave the group chat.
 */
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

    if (uiState.isDeletable && (!uiState.isGroupChat || currentUserMember?.role == ChatMemberRole.OWNER)) {
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