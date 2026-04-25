package ru.kubsu.borshchevyk.feature.chat.ui.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.kubsu.borshchevyk.feature.chat.ChatSettingsViewModel

@Composable
fun ChatSettingsRoute(
    onBackClick: () -> Unit,
    onChatDeletedLocally: () -> Unit,
    onNavigateToInviteSearch: () -> Unit,
    selectedUserIdToInvite: String?,
    onInviteConsumed: () -> Unit,
    viewModel: ChatSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isChatDeleted) {
        if (uiState.isChatDeleted) {
            onChatDeletedLocally()
        }
    }

    LaunchedEffect(selectedUserIdToInvite) {
        if (selectedUserIdToInvite != null) {
            viewModel.onInviteUser(selectedUserIdToInvite)
            onInviteConsumed()
        }
    }

    ChatSettingsScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onShowInviteSearch = { onNavigateToInviteSearch() },
        onGenerateLink = viewModel::onGenerateInviteLink,
        onUpdatePermissions = viewModel::onUpdatePermissions,
        onClearHistory = viewModel::onClearHistory,
        onDeleteChat = viewModel::onDeleteChat,
        onKickUser = viewModel::onKickUser,
        onLeaveChat = viewModel::onLeaveChat,
        onUpdateChatInfo = viewModel::onUpdateChatInfo,
        onAddContact = viewModel::onAddContact,
        onRemoveContact = viewModel::onRemoveContact
    )
}
