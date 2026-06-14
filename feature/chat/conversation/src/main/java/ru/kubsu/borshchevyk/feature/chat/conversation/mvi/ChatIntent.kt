package ru.kubsu.borshchevyk.feature.chat.conversation.mvi

import ru.kubsu.borshchevyk.core.model.domain.Message
import ru.kubsu.borshchevyk.feature.chat.common.model.AttachmentFile

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
    data class ResolveAttachmentUrl(val attachmentId: String, val isThumbnail: Boolean = false) : ChatIntent
    data class DownloadAttachment(val attachmentId: String) : ChatIntent
    data class ResendMessage(val messageId: String) : ChatIntent
    data class ForwardMessage(val message: Message) : ChatIntent
    object InitiateCall : ChatIntent
}
