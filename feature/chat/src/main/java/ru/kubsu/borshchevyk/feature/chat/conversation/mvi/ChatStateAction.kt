package ru.kubsu.borshchevyk.feature.chat.conversation.mvi

import ru.kubsu.borshchevyk.core.model.domain.ChatEvent
import ru.kubsu.borshchevyk.core.model.domain.ForwardPayload
import ru.kubsu.borshchevyk.core.model.domain.Message
import ru.kubsu.borshchevyk.core.model.domain.User

sealed interface ChatStateAction {
    data class LoadingStarted(val isFullLoad: Boolean = true) : ChatStateAction
    data class LoadFailed(val error: String) : ChatStateAction
    data class InitialDataLoaded(
        val chatId: String,
        val currentUserId: String,
        val isGroup: Boolean,
        val chatTitle: String,
        val chatAvatarUrl: String?,
        val history: List<Message>,
        val pinned: List<Message>,
        val forwardPayload: ForwardPayload?
    ) : ChatStateAction
    data class PresenceUpdated(val isOnline: Boolean, val lastSeenAt: Long?) : ChatStateAction
    data class ProcessDomainEvent(val event: ChatEvent) : ChatStateAction
    data class MessageSending(val tempId: String, val message: Message, val data: FailedMessageData) : ChatStateAction
    data class MessageSent(val tempId: String) : ChatStateAction
    data class MessageSendFailed(val tempId: String) : ChatStateAction
    data class HistoryUpdated(val history: List<Message>) : ChatStateAction
    data class SetEditingMessage(val message: Message?) : ChatStateAction
    data class MessageUpdated(val message: Message) : ChatStateAction
    data class UpdateAttachmentUrl(val attachmentId: String, val url: String, val isThumbnail: Boolean = false) : ChatStateAction
    data class SetReaders(val messageId: String, val readers: List<User>) : ChatStateAction
    data class SetComments(val messageId: String, val comments: List<Message>) : ChatStateAction
    data class MessageRemoved(val messageId: String) : ChatStateAction
    data class SetPinnedMessages(val pinned: List<Message>) : ChatStateAction
    data class ReactionToggled(val messageId: String, val reaction: String, val currentUserId: String, val isAdded: Boolean) : ChatStateAction
    data class TitleUpdated(val title: String, val avatarUrl: String?) : ChatStateAction
    object ChatDeleted : ChatStateAction
}
