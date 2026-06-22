package ru.kubsu.borshchevyk.feature.chat.settings.ui

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.kubsu.borshchevyk.feature.chat.settings.ChatSettingsViewModel
import ru.kubsu.borshchevyk.feature.chat.settings.mvi.ChatSettingsEffect
import ru.kubsu.borshchevyk.feature.chat.settings.mvi.ChatSettingsIntent
import ru.kubsu.borshchevyk.feature.chat.sharedmedia.ChatSharedMediaViewModel

/**
 * Route for the Chat Settings screen. Manages ViewModel interaction and state observation.
 *
 * @param onBackClick Callback invoked when the user navigates back.
 * @param onChatDeletedLocally Callback invoked when the chat is deleted locally.
 * @param onNavigateToInviteSearch Callback invoked when the user navigates to search for users to invite.
 * @param selectedUserIdToInvite The ID of the user selected to be invited, if any.
 * @param onInviteConsumed Callback invoked when the invite action has been consumed.
 * @param viewModel The view model managing the chat settings state.
 * @param sharedMediaViewModel The view model managing the shared media state.
 */
@Composable
fun ChatSettingsRoute(
    onBackClick: () -> Unit,
    onChatDeletedLocally: () -> Unit,
    onNavigateToInviteSearch: () -> Unit,
    selectedUserIdToInvite: String?,
    onInviteConsumed: () -> Unit,
    viewModel: ChatSettingsViewModel = hiltViewModel(),
    sharedMediaViewModel: ChatSharedMediaViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sharedMediaUiState by sharedMediaViewModel.uiState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ChatSettingsEffect.ShowError -> Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                is ChatSettingsEffect.NavigateBack -> onBackClick()
            }
        }
    }


    LaunchedEffect(uiState.isChatDeleted) {
        if (uiState.isChatDeleted) {
            onChatDeletedLocally()
        }
    }

    LaunchedEffect(selectedUserIdToInvite) {
        if (selectedUserIdToInvite != null) {
            viewModel.handleIntent(ChatSettingsIntent.InviteUser(selectedUserIdToInvite))
            onInviteConsumed()
        }
    }

    ChatSettingsScreen(
        uiState = uiState,
        sharedMediaUiState = sharedMediaUiState,
        onIntent = viewModel::handleIntent,
        onSharedMediaIntent = sharedMediaViewModel::handleIntent,
        onMessageClick = { /* Navigate to message if needed */ },
        onBackClick = onBackClick,
        onShowInviteSearch = onNavigateToInviteSearch
    )
}
