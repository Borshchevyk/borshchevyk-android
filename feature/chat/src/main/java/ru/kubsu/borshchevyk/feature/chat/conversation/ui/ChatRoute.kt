package ru.kubsu.borshchevyk.feature.chat.conversation.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.kubsu.borshchevyk.core.ui.theme.BorshchevykTheme
import ru.kubsu.borshchevyk.feature.chat.conversation.ChatViewModel
import ru.kubsu.borshchevyk.feature.chat.conversation.mvi.ChatEffect
import ru.kubsu.borshchevyk.feature.chat.conversation.mvi.ChatIntent
import ru.kubsu.borshchevyk.feature.chat.conversation.mvi.ChatUiState
import ru.kubsu.borshchevyk.feature.chat.conversation.ui.components.ChatTopAppBar
import ru.kubsu.borshchevyk.feature.chat.conversation.ui.components.MessageInput

/**
 * Route for the Chat screen. Handles ViewModel interaction, UI state observation, and navigation events.
 *
 * @param onBackClick Callback invoked when the user navigates back.
 * @param onSettingsClick Callback invoked when the user opens chat settings.
 * @param onNavigateToForwardSelection Callback invoked when the user forwards a message.
 * @param onNavigateToCall Callback invoked when the user initiates a call.
 * @param modifier The modifier to be applied to the layout.
 * @param chatViewModel The view model managing the state for this screen.
 */
@Composable
fun ChatRoute(
    onBackClick: () -> Unit,
    onSettingsClick: (String) -> Unit,
    onNavigateToForwardSelection: (String) -> Unit,
    onNavigateToCall: (String) -> Unit,
    modifier: Modifier = Modifier,
    chatViewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by chatViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        chatViewModel.effect.collect { effect ->
            when (effect) {
                is ChatEffect.ShowError -> Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                ChatEffect.NavigateBack -> onBackClick()
                is ChatEffect.NavigateToSettings -> onSettingsClick(effect.chatId)
                is ChatEffect.NavigateToForwardSelection -> onNavigateToForwardSelection(effect.payloadJson)
                is ChatEffect.NavigateToCall -> onNavigateToCall(effect.callId)
            }
        }
    }

    when (val state = uiState) {
        ChatUiState.Loading -> LoadingScreen(modifier)
        is ChatUiState.Error -> ErrorScreen(state.message, modifier)
        is ChatUiState.Content -> {
            LaunchedEffect(state.isChatDeleted) {
                if (state.isChatDeleted) onBackClick()
            }

            Scaffold(
                topBar = {
                    ChatTopAppBar(
                        context = state.context,
                        typingUsers = state.input.typingUsers,
                        onBackClick = onBackClick,
                        onCallClick = { chatViewModel.handleIntent(ChatIntent.InitiateCall) },
                        onSettingsClick = { chatViewModel.handleIntent(ChatIntent.OpenSettings) }
                    )
                },
                bottomBar = {
                    MessageInput(
                        editingMessage = state.input.editingMessage,
                        isSending = state.input.isSending,
                        onSendMessage = { text, atts -> chatViewModel.handleIntent(ChatIntent.SendMessage(text, atts)) },
                        onSendVoice = { bytes, dur -> chatViewModel.handleIntent(ChatIntent.SendVoice(bytes, dur)) },
                        onSendCircle = { bytes, dur -> chatViewModel.handleIntent(ChatIntent.SendCircle(bytes, dur)) },
                        onEditMessage = { id, text -> chatViewModel.handleIntent(ChatIntent.EditMessage(id, text)) },
                        onCancelEdit = { chatViewModel.handleIntent(ChatIntent.SetEditingMessage(null)) },
                        onTyping = { chatViewModel.handleIntent(ChatIntent.Typing) },
                        forwardPayload = state.input.forwardPayload
                    )
                },
                containerColor = BorshchevykTheme.colors.background,
                modifier = modifier
            ) { padding ->
                ChatScreen(
                    contentState = state,
                    onIntent = chatViewModel::handleIntent,
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

/**
 * Displays a loading indicator centered on the screen.
 *
 * @param modifier The modifier to be applied to the layout.
 */
@Composable
private fun LoadingScreen(modifier: Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = BorshchevykTheme.colors.primary)
    }
}

/**
 * An error screen displaying a message when the chat fails to load or encounters a critical error.
 *
 * @param message The error message to display.
 * @param modifier The modifier to be applied to the layout.
 */
@Composable
private fun ErrorScreen(message: String, modifier: Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Error: $message", color = BorshchevykTheme.colors.error)
    }
}
