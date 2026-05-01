package ru.kubsu.borshchevyk.feature.chat.ui.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.kubsu.borshchevyk.feature.chat.ChatSettingsViewModel

/**
 * Route for the Chat Settings screen. Manages ViewModel interaction and state observation.
 *
 * @param onBackClick Callback invoked when the user navigates back.
 * @param onChatDeletedLocally Callback invoked when the chat is deleted locally.
 * @param onNavigateToInviteSearch Callback invoked when the user navigates to search for users to invite.
 * @param selectedUserIdToInvite The ID of the user selected to be invited, if any.
 * @param onInviteConsumed Callback invoked when the invite action has been consumed.
 * @param viewModel The view model managing the chat settings state.
 */
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
