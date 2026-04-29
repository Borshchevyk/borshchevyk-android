package ru.kubsu.borshchevyk.feature.chat.ui.chat

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
import ru.kubsu.borshchevyk.feature.chat.ChatEffect
import ru.kubsu.borshchevyk.feature.chat.ChatIntent
import ru.kubsu.borshchevyk.feature.chat.ChatUiState
import ru.kubsu.borshchevyk.feature.chat.ChatViewModel
import ru.kubsu.borshchevyk.feature.chat.ui.chat.components.ChatTopAppBar
import ru.kubsu.borshchevyk.feature.chat.ui.chat.components.MessageInput

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
                    onResolveAttachmentUrl = { chatViewModel.handleIntent(ChatIntent.ResolveAttachmentUrl(it)) },
                    onPinToggle = { msg ->
                        chatViewModel.handleIntent(if (msg.isPinned) ChatIntent.UnpinMessage(msg.id) else ChatIntent.PinMessage(msg.id))
                    },
                    onReactionToggle = { msgId, reaction -> chatViewModel.handleIntent(ChatIntent.ToggleReaction(msgId, reaction)) },
                    onEdit = { msg -> chatViewModel.handleIntent(ChatIntent.SetEditingMessage(msg)) },
                    onDelete = { msgId, forAll -> chatViewModel.handleIntent(ChatIntent.DeleteMessage(msgId, forAll)) },
                    onMessageVisible = { chatViewModel.handleIntent(ChatIntent.MessageVisible(it)) },
                    onLoadReaders = { chatViewModel.handleIntent(ChatIntent.LoadReaders(it)) },
                    onLoadComments = { chatViewModel.handleIntent(ChatIntent.LoadComments(it)) },
                    onIntent = chatViewModel::handleIntent,
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

@Composable
private fun LoadingScreen(modifier: Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = BorshchevykTheme.colors.primary)
    }
}

@Composable
private fun ErrorScreen(message: String, modifier: Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Error: $message", color = BorshchevykTheme.colors.error)
    }
}
