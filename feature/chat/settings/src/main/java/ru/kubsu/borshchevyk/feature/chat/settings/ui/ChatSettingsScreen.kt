package ru.kubsu.borshchevyk.feature.chat.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ru.kubsu.borshchevyk.core.model.domain.ChatMemberRole
import ru.kubsu.borshchevyk.core.model.domain.Message
import ru.kubsu.borshchevyk.core.ui.theme.BorshchevykTheme
import ru.kubsu.borshchevyk.feature.chat.settings.mvi.ChatSettingsIntent
import ru.kubsu.borshchevyk.feature.chat.settings.mvi.ChatSettingsUiState
import ru.kubsu.borshchevyk.feature.chat.settings.ui.components.AddContactDialog
import ru.kubsu.borshchevyk.feature.chat.settings.ui.components.ChatActionsSection
import ru.kubsu.borshchevyk.feature.chat.settings.ui.components.ChatHeaderSection
import ru.kubsu.borshchevyk.feature.chat.settings.ui.components.ChatSharedMediaSection
import ru.kubsu.borshchevyk.feature.chat.settings.ui.components.ClearHistoryDialog
import ru.kubsu.borshchevyk.feature.chat.settings.ui.components.DangerZoneSection
import ru.kubsu.borshchevyk.feature.chat.settings.ui.components.DeleteChatDialog
import ru.kubsu.borshchevyk.feature.chat.settings.ui.components.MemberItemCard
import ru.kubsu.borshchevyk.feature.chat.settings.ui.components.UpdateChatInfoDialog
import ru.kubsu.borshchevyk.feature.chat.settings.ui.components.UpdatePermissionsDialog
import ru.kubsu.borshchevyk.feature.chat.sharedmedia.mvi.ChatSharedMediaUiState
import ru.kubsu.borshchevyk.feature.chat.sharedmedia.mvi.SharedMediaIntent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChatSettingsScreen(
    uiState: ChatSettingsUiState,
    sharedMediaUiState: ChatSharedMediaUiState,
    onIntent: (ChatSettingsIntent) -> Unit,
    onSharedMediaIntent: (SharedMediaIntent) -> Unit,
    onMessageClick: (Message) -> Unit,
    onBackClick: () -> Unit,
    onShowInviteSearch: () -> Unit
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
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (canChangeInfo) {
                        IconButton(onClick = { showUpdateInfoDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Info")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = BorshchevykTheme.colors.surface
                )
            )
        },
        containerColor = BorshchevykTheme.colors.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item {
                ChatHeaderSection(uiState)
            }

            item {
                ChatActionsSection(
                    uiState = uiState,
                    onIntent = onIntent,
                    onShowInvite = onShowInviteSearch,
                    onShowAddContact = { showAddContactDialog = true }
                )
            }

            item {
                ChatSharedMediaSection(
                    uiState = sharedMediaUiState,
                    onTabSelected = { onSharedMediaIntent(SharedMediaIntent.SelectTab(it)) },
                    onLoadNextPage = { onSharedMediaIntent(SharedMediaIntent.LoadNextPage) },
                    onMessageClick = onMessageClick,
                    onResolveUrl = { id, thumb -> onSharedMediaIntent(SharedMediaIntent.ResolveUrl(id, thumb)) }
                )
            }

            if (uiState.isGroupChat) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Members (${uiState.members.size})",
                            style = BorshchevykTheme.typography.titleMedium,
                            color = BorshchevykTheme.colors.primary
                        )
                        if (canManagePermissions) {
                            IconButton(onClick = onShowInviteSearch, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Add, contentDescription = "Add Member", tint = BorshchevykTheme.colors.primary)
                            }
                        }
                    }
                }

                items(uiState.members) { member ->
                    MemberItemCard(
                        member = member,
                        currentUserId = uiState.currentUserId,
                        canManagePermissions = canManagePermissions,
                        onMemberClick = { memberIdForPermissions = member.userId }
                    )
                }
            }

            item {
                DangerZoneSection(
                    uiState = uiState,
                    currentUserMember = currentUserMember,
                    onShowClearHistory = { showClearHistoryDialog = true },
                    onShowDeleteChat = { showDeleteChatDialog = true },
                    onLeaveChat = { onIntent(ChatSettingsIntent.LeaveChat) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        // Dialogs
        if (showClearHistoryDialog) {
            ClearHistoryDialog(
                isGroupChat = uiState.isGroupChat,
                onDismiss = { showClearHistoryDialog = false },
                onConfirm = { forAll -> 
                    onIntent(ChatSettingsIntent.ClearHistory(forAll))
                    showClearHistoryDialog = false 
                }
            )
        }

        if (showDeleteChatDialog) {
            DeleteChatDialog(
                onDismiss = { showDeleteChatDialog = false },
                onConfirm = { 
                    onIntent(ChatSettingsIntent.DeleteChat)
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
                        onIntent(ChatSettingsIntent.UpdatePermissions(userId, canSend, canDelete, canInvite, canChange))
                        memberIdForPermissions = null
                    },
                    onKickUser = { userId ->
                        onIntent(ChatSettingsIntent.KickUser(userId))
                        memberIdForPermissions = null
                    }
                )
            } ?: run { memberIdForPermissions = null }
        }

        if (showUpdateInfoDialog) {
            UpdateChatInfoDialog(
                initialTitle = uiState.chatName,
                initialDescription = uiState.chatDescription ?: "",
                onDismiss = { showUpdateInfoDialog = false },
                onConfirm = { title, desc ->
                    onIntent(ChatSettingsIntent.UpdateChatInfo(title ?: "", desc))
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
                    onIntent(ChatSettingsIntent.AddContact(firstName, lastName))
                    showAddContactDialog = false
                }
            )
        }
    }
}
