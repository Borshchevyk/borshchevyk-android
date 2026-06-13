package ru.kubsu.borshchevyk.feature.chat.conversation

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import ru.kubsu.borshchevyk.core.model.domain.ChatType
import ru.kubsu.borshchevyk.core.model.domain.ForwardPayload
import ru.kubsu.borshchevyk.core.model.domain.MessageStatus
import ru.kubsu.borshchevyk.feature.chat.common.model.AttachmentFile
import ru.kubsu.borshchevyk.feature.chat.common.mvi.BaseMviViewModel
import ru.kubsu.borshchevyk.feature.chat.conversation.interactor.CallHandler
import ru.kubsu.borshchevyk.feature.chat.conversation.interactor.ChatEventHandler
import ru.kubsu.borshchevyk.feature.chat.conversation.interactor.ChatMessageHandler
import ru.kubsu.borshchevyk.feature.chat.conversation.interactor.MediaVoiceHandler
import ru.kubsu.borshchevyk.feature.chat.conversation.mvi.ChatEffect
import ru.kubsu.borshchevyk.feature.chat.conversation.mvi.ChatIntent
import ru.kubsu.borshchevyk.feature.chat.conversation.mvi.ChatStateAction
import ru.kubsu.borshchevyk.feature.chat.conversation.mvi.ChatUiState
import ru.kubsu.borshchevyk.feature.chat.conversation.mvi.FailedMessageData
import ru.kubsu.borshchevyk.feature.chat.conversation.mvi.reduce
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val chatMessageHandler: ChatMessageHandler,
    private val callHandler: CallHandler,
    private val mediaVoiceHandler: MediaVoiceHandler,
    private val chatEventHandler: ChatEventHandler
) : BaseMviViewModel<ChatUiState, ChatStateAction, ChatIntent, ChatEffect>(ChatUiState.Loading) {

    private val chatId: String = checkNotNull(savedStateHandle["chatId"])
    private val forwardPayloadJson: String? = savedStateHandle["forwardPayloadJson"]

    private var typingJob: Job? = null

    override fun reduce(state: ChatUiState, action: ChatStateAction): ChatUiState = state.reduce(action)

    init {
        val initialForwardPayload: ForwardPayload? = try {
            forwardPayloadJson?.let { Json.decodeFromString<ForwardPayload>(it) }
        } catch (e: Exception) { null }

        viewModelScope.launch {
            dispatch(ChatStateAction.LoadingStarted())
            try {
                val data = chatEventHandler.loadInitialData(chatId, initialForwardPayload)
                dispatch(ChatStateAction.InitialDataLoaded(
                    chatId = data.chatId,
                    currentUserId = data.currentUserId,
                    isGroup = data.isGroup,
                    chatTitle = data.chatTitle,
                    chatAvatarUrl = data.chatAvatarUrl,
                    history = data.history,
                    pinned = data.pinned,
                    forwardPayload = data.forwardPayload
                ))
                
                chatEventHandler.getPartnerId(chatId)?.let { partnerId ->
                    chatEventHandler.observePresence(partnerId).onEach { presence ->
                        dispatch(ChatStateAction.PresenceUpdated(presence.isOnline, presence.lastSeenAt))
                    }.launchIn(viewModelScope)
                }
            } catch (e: Exception) {
                dispatch(ChatStateAction.LoadFailed(e.message ?: "Failed"))
            }
        }

        chatEventHandler.observeHistory(chatId).onEach { history ->
            dispatch(ChatStateAction.HistoryUpdated(history))
            history.forEach { msg ->
                msg.attachments.forEach { att ->
                    handleIntent(ChatIntent.ResolveAttachmentUrl(att.id, false))
                }
            }
        }.launchIn(viewModelScope)

        chatEventHandler.observeEvents(chatId).onEach { event -> 
            dispatch(ChatStateAction.ProcessDomainEvent(event)) 
            if (event is ru.kubsu.borshchevyk.core.model.domain.ChatEvent.NewMessage) {
                event.message.attachments.forEach { att ->
                    handleIntent(ChatIntent.ResolveAttachmentUrl(att.id, false))
                }
            }
        }.launchIn(viewModelScope)

        chatEventHandler.observeChats().onEach { chats ->
            val chat = chats.find { it.id == chatId }
            if (chat != null) {
                val isGroup = chat.type == ChatType.GROUP
                val chatTitle = chat.title ?: chat.partnerName ?: if (isGroup) "Group Chat" else "Private Chat"
                dispatch(ChatStateAction.TitleUpdated(chatTitle, chat.partnerAvatarUrl))
            }
        }.launchIn(viewModelScope)
    }

    override fun handleIntent(intent: ChatIntent) {
        val state = uiState.value as? ChatUiState.Content
        when (intent) {
            is ChatIntent.OpenSettings -> sendEffect(ChatEffect.NavigateToSettings(chatId))
            is ChatIntent.ChatDeletedLocally -> dispatch(ChatStateAction.ChatDeleted)
            is ChatIntent.Typing -> {
                viewModelScope.launch {
                    try {
                        chatMessageHandler.sendTypingEvent(chatId, true)
                        typingJob?.cancel()
                        typingJob = launch {
                            delay(3000)
                            chatMessageHandler.sendTypingEvent(chatId, false)
                        }
                    } catch (e: Exception) { Log.w("ChatVM", "Typing event failed", e) }
                }
            }
            is ChatIntent.SendMessage -> {
                if (state == null) return
                val forwardPayload = state.input.forwardPayload
                if (intent.text.isBlank() && intent.attachments.isEmpty() && forwardPayload == null) return

                val currentUserId = state.context.currentUserId
                
                val optimisticMessages = chatMessageHandler.createOptimisticMessages(
                    chatId = chatId,
                    text = intent.text,
                    attachments = intent.attachments,
                    currentUserId = currentUserId,
                    forwardPayload = forwardPayload
                )

                optimisticMessages.forEach { optData ->
                    val failedData = FailedMessageData(optData.text, optData.attachments, optData.forwardPayload)
                    dispatch(ChatStateAction.MessageSending(optData.tempId, optData.message, failedData))
                    performSendMessage(optData.tempId, optData.text, optData.attachments, optData.forwardPayload)
                }
            }
            is ChatIntent.SendVoice -> {
                viewModelScope.launch {
                    try {
                        mediaVoiceHandler.sendVoice(chatId, intent.bytes, intent.duration)
                    } catch (e: Exception) { sendEffect(ChatEffect.ShowError("Voice send failed")) }
                }
            }
            is ChatIntent.SendCircle -> {
                viewModelScope.launch {
                    try {
                        mediaVoiceHandler.sendCircle(chatId, intent.bytes, intent.duration)
                    } catch (e: Exception) { sendEffect(ChatEffect.ShowError("Circle send failed")) }
                }
            }
            is ChatIntent.SetEditingMessage -> dispatch(ChatStateAction.SetEditingMessage(intent.message))
            is ChatIntent.EditMessage -> {
                if (intent.newText.isBlank()) return
                viewModelScope.launch {
                    try {
                        chatMessageHandler.editMessage(chatId, intent.messageId, intent.newText)
                        dispatch(ChatStateAction.SetEditingMessage(null))
                    } catch (e: Exception) { sendEffect(ChatEffect.ShowError("Edit failed: ${e.message}")) }
                }
            }
            is ChatIntent.DeleteMessage -> {
                viewModelScope.launch {
                    try {
                        chatMessageHandler.deleteMessage(chatId, intent.messageId, intent.forAll)
                        dispatch(ChatStateAction.MessageRemoved(intent.messageId))
                    } catch (e: Exception) { sendEffect(ChatEffect.ShowError("Delete failed: ${e.message}")) }
                }
            }
            is ChatIntent.MessageVisible -> {
                viewModelScope.launch {
                    try { chatMessageHandler.markAsRead(chatId, intent.messageId) } catch (e: Exception) { Log.w("ChatVM", "Mark as read failed", e) }
                }
            }
            is ChatIntent.LoadReaders -> {
                viewModelScope.launch {
                    try {
                        val readers = chatEventHandler.getReaders(chatId, intent.messageId)
                        dispatch(ChatStateAction.SetReaders(intent.messageId, readers))
                    } catch (e: Exception) { sendEffect(ChatEffect.ShowError("Readers load failed")) }
                }
            }
            is ChatIntent.LoadComments -> {
                viewModelScope.launch {
                    try {
                        val comments = chatEventHandler.getComments(chatId, intent.messageId)
                        dispatch(ChatStateAction.SetComments(intent.messageId, comments))
                    } catch (e: Exception) { sendEffect(ChatEffect.ShowError("Comments load failed")) }
                }
            }
            is ChatIntent.PinMessage -> {
                viewModelScope.launch {
                    try { chatMessageHandler.pinMessage(chatId, intent.messageId) } catch (e: Exception) { sendEffect(ChatEffect.ShowError("Pin failed")) }
                }
            }
            is ChatIntent.UnpinMessage -> {
                viewModelScope.launch {
                    try { chatMessageHandler.unpinMessage(chatId, intent.messageId) } catch (e: Exception) { sendEffect(ChatEffect.ShowError("Unpin failed")) }
                }
            }
            is ChatIntent.ToggleReaction -> {
                if (state == null) return
                val currentUserId = state.context.currentUserId
                val message = state.feed.messages.find { it.id == intent.messageId } ?: return
                val hasReaction = message.reactions.any { it.reaction == intent.reaction && it.userId == currentUserId }
                viewModelScope.launch {
                    try {
                        chatMessageHandler.toggleReaction(chatId, intent.messageId, intent.reaction, hasReaction)
                        dispatch(ChatStateAction.ReactionToggled(intent.messageId, intent.reaction, currentUserId, !hasReaction))
                    } catch (e: Exception) { sendEffect(ChatEffect.ShowError("Reaction failed")) }
                }
            }
            is ChatIntent.ResolveAttachmentUrl -> {
                if (intent.attachmentId.startsWith("temp_")) return
                if (state != null) {
                    if (intent.isThumbnail && state.feed.thumbnailUrls.containsKey(intent.attachmentId)) return
                    if (!intent.isThumbnail && state.feed.attachmentUrls.containsKey(intent.attachmentId)) return
                }
                viewModelScope.launch {
                    try {
                        val attachment = state?.feed?.messages?.flatMap { it.attachments }?.find { it.id == intent.attachmentId }
                        if (attachment != null) {
                            val urls = chatEventHandler.resolveAttachmentUrls(attachment, intent.isThumbnail)
                            urls.forEach { (id, url) ->
                                val isThumb = id.endsWith("_thumb")
                                val realId = id.removeSuffix("_thumb")
                                dispatch(ChatStateAction.UpdateAttachmentUrl(realId, url, isThumb))
                            }
                        }
                    } catch (e: Exception) { Log.w("ChatVM", "Resolve failed", e) }
                }
            }
            is ChatIntent.DownloadAttachment -> {
                viewModelScope.launch {
                    try {
                        val result = chatEventHandler.exportAttachment(intent.attachmentId)
                        result.fold(
                            onSuccess = { sendEffect(ChatEffect.ShowError("File saved to Downloads")) },
                            onFailure = { sendEffect(ChatEffect.ShowError("Failed to download file")) }
                        )
                    } catch (e: Exception) { sendEffect(ChatEffect.ShowError("Failed to start download")) }
                }
            }
            is ChatIntent.ResendMessage -> {
                val data = state?.input?.pendingMessagesData?.get(intent.messageId) ?: return
                val msg = state.feed.messages.find { it.id == intent.messageId } ?: return
                dispatch(ChatStateAction.MessageSending(intent.messageId, msg.copy(status = MessageStatus.SENDING), data))
                performSendMessage(intent.messageId, data.text, data.attachments, data.forwardPayload)
            }
            is ChatIntent.ForwardMessage -> {
                val authorName = intent.message.author?.let { "${it.firstName} ${it.lastName ?: ""}".trim() } ?: "User"
                val payload = ForwardPayload(
                    text = intent.message.text,
                    attachmentIds = intent.message.attachments.map { it.id },
                    fromChatId = intent.message.chatId,
                    fromUserId = intent.message.authorId,
                    authorName = authorName
                )
                viewModelScope.launch {
                    try {
                        val json = Json.encodeToString(ForwardPayload.serializer(), payload)
                        sendEffect(ChatEffect.NavigateToForwardSelection(json))
                    } catch (e: Exception) { sendEffect(ChatEffect.ShowError("Forwarding failed")) }
                }
            }
            is ChatIntent.InitiateCall -> {
                val currentUserId = state?.context?.currentUserId ?: return
                viewModelScope.launch {
                    try {
                        val callId = callHandler.initiateCall(chatId, currentUserId)
                        sendEffect(ChatEffect.NavigateToCall(callId))
                    } catch (e: Exception) { sendEffect(ChatEffect.ShowError(e.message ?: "Call failed")) }
                }
            }
        }
    }

    private fun performSendMessage(tempId: String, text: String, attachments: List<AttachmentFile>, forwardPayload: ForwardPayload?) {
        viewModelScope.launch {
            try {
                chatMessageHandler.sendMessage(chatId, text, attachments, forwardPayload)
                dispatch(ChatStateAction.MessageSent(tempId))
                chatMessageHandler.sendTypingEvent(chatId, false)
                typingJob?.cancel()
            } catch (e: Exception) {
                dispatch(ChatStateAction.MessageSendFailed(tempId))
                sendEffect(ChatEffect.ShowError("Failed to send: ${e.message}"))
            }
        }
    }
}
