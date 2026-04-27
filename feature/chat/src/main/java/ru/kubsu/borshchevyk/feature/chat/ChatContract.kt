package ru.kubsu.borshchevyk.feature.chat

import android.net.Uri
import ru.kubsu.borshchevyk.core.model.domain.Message

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
