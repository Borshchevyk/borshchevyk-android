package ru.kubsu.borshchevyk.feature.chat.conversation

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import ru.kubsu.borshchevyk.core.model.domain.ChatType
import ru.kubsu.borshchevyk.core.model.domain.ForwardPayload
import ru.kubsu.borshchevyk.core.model.domain.MessageStatus
import ru.kubsu.borshchevyk.core.ui.mvi.mviContainer
import ru.kubsu.borshchevyk.feature.chat.common.model.AttachmentFile
import ru.kubsu.borshchevyk.feature.chat.conversation.interactor.CallHandler
import ru.kubsu.borshchevyk.feature.chat.conversation.interactor.ChatHistoryHandler
import ru.kubsu.borshchevyk.feature.chat.conversation.interactor.ChatMediaHandler
import ru.kubsu.borshchevyk.feature.chat.conversation.interactor.ChatMessageHandler
import ru.kubsu.borshchevyk.feature.chat.conversation.interactor.ChatPresenceHandler
import ru.kubsu.borshchevyk.feature.chat.conversation.interactor.LocalMediaInteractor
import ru.kubsu.borshchevyk.feature.chat.conversation.interactor.MediaVoiceHandler
import ru.kubsu.borshchevyk.feature.chat.conversation.interactor.VoiceRecorder
import ru.kubsu.borshchevyk.feature.chat.conversation.mvi.ChatEffect
import ru.kubsu.borshchevyk.feature.chat.conversation.mvi.ChatIntent
import ru.kubsu.borshchevyk.feature.chat.conversation.mvi.ChatUiState
import ru.kubsu.borshchevyk.feature.chat.conversation.mvi.FailedMessageData
import ru.kubsu.borshchevyk.feature.chat.conversation.mvi.processDomainEvent
import ru.kubsu.borshchevyk.feature.chat.conversation.mvi.removeMessage
import ru.kubsu.borshchevyk.feature.chat.conversation.mvi.setChatDeleted
import ru.kubsu.borshchevyk.feature.chat.conversation.mvi.setComments
import ru.kubsu.borshchevyk.feature.chat.conversation.mvi.setEditingMessage
import ru.kubsu.borshchevyk.feature.chat.conversation.mvi.setInitialDataLoaded
import ru.kubsu.borshchevyk.feature.chat.conversation.mvi.setLoadFailed
import ru.kubsu.borshchevyk.feature.chat.conversation.mvi.setLoading
import ru.kubsu.borshchevyk.feature.chat.conversation.mvi.setMessageSendFailed
import ru.kubsu.borshchevyk.feature.chat.conversation.mvi.setMessageSending
import ru.kubsu.borshchevyk.feature.chat.conversation.mvi.setMessageSent
import ru.kubsu.borshchevyk.feature.chat.conversation.mvi.setReaders
import ru.kubsu.borshchevyk.feature.chat.conversation.mvi.setRecordingVoice
import ru.kubsu.borshchevyk.feature.chat.conversation.mvi.toggleReaction
import ru.kubsu.borshchevyk.feature.chat.conversation.mvi.updateAttachmentUrl
import ru.kubsu.borshchevyk.feature.chat.conversation.mvi.updateHistory
import ru.kubsu.borshchevyk.feature.chat.conversation.mvi.updatePresence
import ru.kubsu.borshchevyk.feature.chat.conversation.mvi.updateTitle
import ru.kubsu.borshchevyk.feature.chat.conversation.ui.model.toUiModel
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val chatMessageHandler: ChatMessageHandler,
    private val callHandler: CallHandler,
    private val mediaVoiceHandler: MediaVoiceHandler,
    private val chatHistoryHandler: ChatHistoryHandler,
    private val chatPresenceHandler: ChatPresenceHandler,
    private val chatMediaHandler: ChatMediaHandler,
    private val localMediaInteractor: LocalMediaInteractor,
    private val voiceRecorder: VoiceRecorder
) : ViewModel() {

    private val container = mviContainer<ChatUiState, ChatEffect>(ChatUiState.Loading, viewModelScope)
    val uiState = container.uiState
    val effect = container.effect

    private fun sendEffect(effect: ChatEffect) {
        container.sendEffect(effect)
    }

    fun observeAttachmentProgress(attachmentId: String): Flow<Float> {
        return chatMediaHandler.observeAttachmentProgress(attachmentId)
    }

    private val chatId: String = checkNotNull(savedStateHandle["chatId"])
    private val forwardPayloadJson: String? = savedStateHandle["forwardPayloadJson"]

    private var typingJob: Job? = null

    init {
        val initialForwardPayload: ForwardPayload? = try {
            forwardPayloadJson?.let { Json.decodeFromString<ForwardPayload>(it) }
        } catch (e: Exception) { null }

        viewModelScope.launch {
            container.updateState { it.setLoading() }
            try {
                val data = chatHistoryHandler.loadInitialData(chatId, initialForwardPayload)
                container.updateState { it.setInitialDataLoaded(
                    chatId = data.chatId,
                    currentUserId = data.currentUserId,
                    isGroup = data.isGroup,
                    chatTitle = data.chatTitle,
                    chatAvatarUrl = data.chatAvatarUrl,
                    history = data.history,
                    pinned = data.pinned,
                    forwardPayload = data.forwardPayload
                ) }
                
                chatPresenceHandler.getPartnerId(chatId)?.let { partnerId ->
                    chatPresenceHandler.observePresence(partnerId).onEach { presence ->
                        container.updateState { it.updatePresence(presence.isOnline, presence.lastSeenAt) }
                    }.launchIn(viewModelScope)
                }
            } catch (e: Exception) {
                container.updateState { it.setLoadFailed(e.message ?: "Failed") }
            }
        }

        chatHistoryHandler.observeHistory(chatId).onEach { history ->
            container.updateState { it.updateHistory(history) }
        }.launchIn(viewModelScope)

        chatHistoryHandler.observeEvents(chatId).onEach { event -> 
            container.updateState { it.processDomainEvent(event) }
        }.launchIn(viewModelScope)

        chatHistoryHandler.observeChats().onEach { chats ->
            val chat = chats.find { it.id == chatId }
            if (chat != null) {
                val isGroup = chat.type == ChatType.GROUP
                val chatTitle = chat.title ?: chat.partnerName ?: if (isGroup) "Group Chat" else "Private Chat"
                container.updateState { it.updateTitle(chatTitle, chat.partnerAvatarUrl) }
            } else {
                handleIntent(ChatIntent.ChatDeletedLocally)
            }
        }.launchIn(viewModelScope)
    }

    fun handleIntent(intent: ChatIntent) {
        val state = uiState.value as? ChatUiState.Content
        when (intent) {
            is ChatIntent.OpenSettings -> sendEffect(ChatEffect.NavigateToSettings(chatId))
            is ChatIntent.ChatDeletedLocally -> container.updateState { it.setChatDeleted() }
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

                viewModelScope.launch {
                    try {
                        val currentUserId = state.context.currentUserId
                        val resolvedAttachments = intent.attachments.mapNotNull { localMediaInteractor.resolveAttachment(it) }
                        
                        val optimisticMessages = chatMessageHandler.createOptimisticMessages(
                            chatId = chatId,
                            text = intent.text,
                            attachments = resolvedAttachments,
                            currentUserId = currentUserId,
                            forwardPayload = forwardPayload
                        )

                        optimisticMessages.forEach { optData ->
                            val failedData = FailedMessageData(optData.text, optData.attachments, optData.forwardPayload)
                            container.updateState { it.setMessageSending(optData.tempId, optData.message.toUiModel(), failedData) }
                            performSendMessage(optData.tempId, optData.text, optData.attachments, optData.forwardPayload)
                        }
                    } catch (e: Exception) {
                        sendEffect(ChatEffect.ShowError("Failed to prepare message"))
                    }
                }
            }
            is ChatIntent.SendVoice -> {
                viewModelScope.launch {
                    try {
                        val audioFile = localMediaInteractor.resolveAudio(intent.uri)
                        if (audioFile != null) {
                            val provider = localMediaInteractor.getInputStreamProvider(audioFile.uri)
                            mediaVoiceHandler.sendVoice(chatId, provider, audioFile.sizeBytes, audioFile.duration?.toDouble() ?: 0.0)
                        } else {
                            sendEffect(ChatEffect.ShowError("Could not resolve audio"))
                        }
                    } catch (e: Exception) { sendEffect(ChatEffect.ShowError("Voice send failed")) }
                }
            }
            is ChatIntent.SendCircle -> {
                viewModelScope.launch {
                    try {
                        val circleFile = localMediaInteractor.resolveAttachment(intent.uri)
                        if (circleFile != null) {
                            val provider = localMediaInteractor.getInputStreamProvider(circleFile.uri)
                            mediaVoiceHandler.sendCircle(chatId, provider, circleFile.sizeBytes, circleFile.duration?.toDouble() ?: 0.0)
                        } else {
                            sendEffect(ChatEffect.ShowError("Could not resolve circle video"))
                        }
                    } catch (e: Exception) { sendEffect(ChatEffect.ShowError("Circle send failed")) }
                }
            }
            is ChatIntent.SetEditingMessage -> container.updateState { it.setEditingMessage(intent.message) }
            is ChatIntent.EditMessage -> {
                if (intent.newText.isBlank()) return
                viewModelScope.launch {
                    try {
                        chatMessageHandler.editMessage(chatId, intent.messageId, intent.newText)
                        container.updateState { it.setEditingMessage(null) }
                    } catch (e: Exception) { sendEffect(ChatEffect.ShowError("Edit failed: ${e.message}")) }
                }
            }
            is ChatIntent.DeleteMessage -> {
                viewModelScope.launch {
                    try {
                        chatMessageHandler.deleteMessage(chatId, intent.messageId, intent.forAll)
                        container.updateState { it.removeMessage(intent.messageId) }
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
                        val readers = chatHistoryHandler.getReaders(chatId, intent.messageId)
                        container.updateState { it.setReaders(intent.messageId, readers) }
                    } catch (e: Exception) { sendEffect(ChatEffect.ShowError("Readers load failed")) }
                }
            }
            is ChatIntent.LoadComments -> {
                viewModelScope.launch {
                    try {
                        val comments = chatHistoryHandler.getComments(chatId, intent.messageId)
                        container.updateState { it.setComments(intent.messageId, comments) }
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
                        container.updateState { it.toggleReaction(intent.messageId, intent.reaction, currentUserId, !hasReaction) }
                    } catch (e: Exception) { sendEffect(ChatEffect.ShowError("Reaction failed")) }
                }
            }
            is ChatIntent.ResolveAttachmentUrl -> {
                if (intent.attachment.id.startsWith("temp_")) return
                if (state != null) {
                    if (intent.isThumbnail && state.feed.thumbnailUrls.containsKey(intent.attachment.id)) return
                    if (!intent.isThumbnail && state.feed.attachmentUrls.containsKey(intent.attachment.id)) return
                }
                viewModelScope.launch {
                    try {
                        val urls = chatMediaHandler.resolveAttachmentUrls(intent.attachment, intent.isThumbnail)
                        urls.forEach { (id, url) ->
                            val isThumb = id.endsWith("_thumb")
                            val realId = id.removeSuffix("_thumb")
                            container.updateState { it.updateAttachmentUrl(realId, url, isThumb) }
                        }
                    } catch (e: Exception) { Log.w("ChatVM", "Resolve failed", e) }
                }
            }
            is ChatIntent.DownloadAttachment -> {
                viewModelScope.launch {
                    try {
                        val result = chatMediaHandler.exportAttachment(intent.attachmentId)
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
                container.updateState { it.setMessageSending(intent.messageId, msg.copy(status = MessageStatus.SENDING), data) }
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
            is ChatIntent.StartRecording -> {
                voiceRecorder.startRecording().onFailure { 
                    sendEffect(ChatEffect.ShowError("Recording failed to start")) 
                }.onSuccess {
                    container.updateState { it.setRecordingVoice(true) }
                }
            }
            is ChatIntent.StopRecording -> {
                voiceRecorder.stopRecording().onFailure {
                    sendEffect(ChatEffect.ShowError("Recording failed"))
                    container.updateState { it.setRecordingVoice(false) }
                }.onSuccess { uri ->
                    container.updateState { it.setRecordingVoice(false) }
                    handleIntent(ChatIntent.SendVoice(uri))
                }
            }
            is ChatIntent.CancelRecording -> {
                voiceRecorder.cancelRecording()
                container.updateState { it.setRecordingVoice(false) }
            }
        }
    }

    private fun performSendMessage(tempId: String, text: String, attachments: List<AttachmentFile>, forwardPayload: ForwardPayload?) {
        viewModelScope.launch {
            try {
                chatMessageHandler.sendMessage(chatId, text, attachments, forwardPayload)
                container.updateState { it.setMessageSent(tempId) }
                chatMessageHandler.sendTypingEvent(chatId, false)
                typingJob?.cancel()
            } catch (e: Exception) {
                container.updateState { it.setMessageSendFailed(tempId) }
                sendEffect(ChatEffect.ShowError("Failed to send: ${e.message}"))
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceRecorder.cancelRecording()
    }
}
