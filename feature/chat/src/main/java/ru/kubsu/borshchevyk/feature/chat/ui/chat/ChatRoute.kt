package ru.kubsu.borshchevyk.feature.chat.ui.chat

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import ru.kubsu.borshchevyk.feature.chat.ui.chat.components.MessageInput

@OptIn(ExperimentalMaterial3Api::class)
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
                is ChatEffect.ShowError -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is ChatEffect.NavigateBack -> {
                    onBackClick()
                }
                is ChatEffect.NavigateToSettings -> {
                    onSettingsClick(effect.chatId)
                }
                is ChatEffect.NavigateToForwardSelection -> {
                    onNavigateToForwardSelection(effect.payloadJson)
                }
                is ChatEffect.NavigateToCall -> {
                    onNavigateToCall(effect.callId)
                }
            }
        }
    }

    when (val state = uiState) {
        is ChatUiState.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BorshchevykTheme.colors.primary)
            }
        }
        is ChatUiState.Error -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Error: ${state.message}", color = BorshchevykTheme.colors.error)
            }
        }
        is ChatUiState.Content -> {
            LaunchedEffect(state.isChatDeleted) {
                if (state.isChatDeleted) {
                    onBackClick()
                }
            }

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { 
                            androidx.compose.foundation.layout.Column {
                                Text(
                                    text = state.context.chatName,
                                    style = BorshchevykTheme.typography.titleMedium,
                                    color = BorshchevykTheme.colors.onSurface
                                ) 
                                val typingUsers = state.input.typingUsers.filter { it != state.context.currentUserId }
                                if (typingUsers.isNotEmpty()) {
                                    Text(
                                        text = if (typingUsers.size == 1) "User is typing..." else "Multiple users are typing...",
                                        style = BorshchevykTheme.typography.labelSmall,
                                        color = BorshchevykTheme.colors.primary
                                    )
                                } else {
                                    // Empty text to keep the height consistent so title doesn't jump
                                    Text(
                                        text = " ",
                                        style = BorshchevykTheme.typography.labelSmall,
                                        color = androidx.compose.ui.graphics.Color.Transparent
                                    )
                                }
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onBackClick) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack, 
                                    contentDescription = "Back",
                                    tint = BorshchevykTheme.colors.onSurface
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = { chatViewModel.handleIntent(ChatIntent.InitiateCall) }) {
                                Icon(
                                    Icons.Default.Call, 
                                    contentDescription = "Call",
                                    tint = BorshchevykTheme.colors.onSurface
                                )
                            }
                            IconButton(onClick = { chatViewModel.handleIntent(ChatIntent.OpenSettings) }) {
                                Icon(
                                    Icons.Default.Info, 
                                    contentDescription = "Chat Info",
                                    tint = BorshchevykTheme.colors.onSurface
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = BorshchevykTheme.colors.background
                        )
                    )
                },
                bottomBar = {
                    MessageInput(
                        editingMessage = state.input.editingMessage,
                        isSending = state.input.isSending,
                        onSendMessage = { text, attachments -> chatViewModel.handleIntent(ChatIntent.SendMessage(text, attachments)) },
                        onSendVoice = { bytes, duration -> chatViewModel.handleIntent(ChatIntent.SendVoice(bytes, duration)) },
                        onSendCircle = { bytes, duration -> chatViewModel.handleIntent(ChatIntent.SendCircle(bytes, duration)) },
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
                    onResolveAttachmentUrl = { attId -> chatViewModel.handleIntent(ChatIntent.ResolveAttachmentUrl(attId)) },
                    onPinToggle = { msg ->
                        if (msg.isPinned) chatViewModel.handleIntent(ChatIntent.UnpinMessage(msg.id))
                        else chatViewModel.handleIntent(ChatIntent.PinMessage(msg.id))
                    },
                    onReactionToggle = { msgId, reaction ->
                        chatViewModel.handleIntent(ChatIntent.ToggleReaction(msgId, reaction))
                    },
                    onEdit = { msg -> chatViewModel.handleIntent(ChatIntent.SetEditingMessage(msg)) },
                    onDelete = { msgId, forAll ->
                        chatViewModel.handleIntent(ChatIntent.DeleteMessage(msgId, forAll))
                    },
                    onMessageVisible = { msgId ->
                        chatViewModel.handleIntent(ChatIntent.MessageVisible(msgId))
                    },
                    onLoadReaders = { msgId ->
                        chatViewModel.handleIntent(ChatIntent.LoadReaders(msgId))
                    },
                    onLoadComments = { msgId ->
                        chatViewModel.handleIntent(ChatIntent.LoadComments(msgId))
                    },
                    onIntent = chatViewModel::handleIntent,
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}
