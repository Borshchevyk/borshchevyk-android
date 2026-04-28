package ru.kubsu.borshchevyk.feature.chat

import android.net.Uri
import ru.kubsu.borshchevyk.core.model.domain.ChatEvent
import ru.kubsu.borshchevyk.core.model.domain.Message
import ru.kubsu.borshchevyk.core.model.domain.User

data class AttachmentFile(
    val uri: Uri,
    val bytes: ByteArray,
    val originalFilename: String,
    val contentType: String,
    val extension: String,
    val width: Int? = null,
    val height: Int? = null,
    val duration: Int? = null
)

sealed interface ChatStateAction {
    data class LoadingStarted(val isFullLoad: Boolean = true) : ChatStateAction
    data class LoadFailed(val error: String) : ChatStateAction
    data class InitialDataLoaded(
        val chatId: String,
        val currentUserId: String,
        val isGroup: Boolean,
        val chatTitle: String,
        val history: List<Message>,
        val pinned: List<Message>,
        val forwardPayload: ru.kubsu.borshchevyk.core.model.domain.ForwardPayload?
    ) : ChatStateAction
    data class PresenceUpdated(val isOnline: Boolean, val lastSeenAt: Long?) : ChatStateAction
    data class ProcessDomainEvent(val event: ChatEvent) : ChatStateAction
    data class MessageSending(val tempId: String, val message: Message) : ChatStateAction
    data class MessageSent(val tempId: String) : ChatStateAction
    data class MessageSendFailed(val tempId: String) : ChatStateAction
    data class HistoryUpdated(val history: List<Message>) : ChatStateAction
    data class SetEditingMessage(val message: Message?) : ChatStateAction
    data class MessageUpdated(val message: Message) : ChatStateAction
    data class UpdateAttachmentUrl(val attachmentId: String, val url: String) : ChatStateAction
    data class SetReaders(val messageId: String, val readers: List<User>) : ChatStateAction
    data class SetComments(val messageId: String, val comments: List<Message>) : ChatStateAction
    data class MessageRemoved(val messageId: String) : ChatStateAction
    data class SetPinnedMessages(val pinned: List<Message>) : ChatStateAction
    data class ReactionToggled(val messageId: String, val reaction: String, val currentUserId: String, val isAdded: Boolean) : ChatStateAction
    object ChatDeleted : ChatStateAction
}

sealed interface ChatIntent {
    object OpenSettings : ChatIntent
    object ChatDeletedLocally : ChatIntent
    object Typing : ChatIntent
    
    data class SendMessage(val text: String, val attachments: List<AttachmentFile>) : ChatIntent
    data class SendVoice(val bytes: ByteArray, val duration: Double) : ChatIntent
    data class SendCircle(val bytes: ByteArray, val duration: Double) : ChatIntent
    data class SetEditingMessage(val message: Message?) : ChatIntent
    data class EditMessage(val messageId: String, val newText: String) : ChatIntent
    data class DeleteMessage(val messageId: String, val forAll: Boolean) : ChatIntent
    data class MessageVisible(val messageId: String) : ChatIntent
    data class LoadReaders(val messageId: String) : ChatIntent
    data class LoadComments(val messageId: String) : ChatIntent
    data class PinMessage(val messageId: String) : ChatIntent
    data class UnpinMessage(val messageId: String) : ChatIntent
    data class ToggleReaction(val messageId: String, val reaction: String) : ChatIntent
    data class ResolveAttachmentUrl(val attachmentId: String) : ChatIntent
    data class ResendMessage(val messageId: String) : ChatIntent
    data class ForwardMessage(val message: Message) : ChatIntent
    object InitiateCall : ChatIntent
}

sealed interface ChatEffect {
    data class ShowError(val message: String) : ChatEffect
    object NavigateBack : ChatEffect
    data class NavigateToSettings(val chatId: String) : ChatEffect
    data class NavigateToForwardSelection(val payloadJson: String) : ChatEffect
    data class NavigateToCall(val callId: String) : ChatEffect
}
