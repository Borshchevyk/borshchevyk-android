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

            val editingMessageUi = state.input.editingMessage

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
                        editingMessage = editingMessageUi,
                        isSending = state.input.isSending,
                        isRecordingVoice = state.input.isRecordingVoice,
                        onSendMessage = { text, atts -> chatViewModel.handleIntent(ChatIntent.SendMessage(text, atts)) },
                        onSendCircle = { uri -> chatViewModel.handleIntent(ChatIntent.SendCircle(uri)) },
                        onEditMessage = { id, text -> chatViewModel.handleIntent(ChatIntent.EditMessage(id, text)) },
                        onCancelEdit = { chatViewModel.handleIntent(ChatIntent.SetEditingMessage(null)) },
                        onTyping = { chatViewModel.handleIntent(ChatIntent.Typing) },
                        onStartVoiceRecording = { chatViewModel.handleIntent(ChatIntent.StartRecording) },
                        onStopVoiceRecording = { chatViewModel.handleIntent(ChatIntent.StopRecording) },
                        forwardPayload = state.input.forwardPayload
                    )
                },
                containerColor = BorshchevykTheme.colors.background,
                modifier = modifier
            ) { padding ->
                ChatScreen(
                    contentState = state,
                    onIntent = chatViewModel::handleIntent,
                    onObserveProgress = chatViewModel::observeAttachmentProgress,
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
