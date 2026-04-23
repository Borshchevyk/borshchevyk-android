package ru.kubsu.borshchevyk.core.model.domain

import ru.kubsu.borshchevyk.core.model.dto.NotificationDto
import ru.kubsu.borshchevyk.core.model.dto.ReactionEvent
import ru.kubsu.borshchevyk.core.model.dto.ReadReceiptEvent
import ru.kubsu.borshchevyk.core.model.dto.TypingEvent

sealed interface ChatEvent {
    data class NewMessage(val message: NotificationDto.MessageDto) : ChatEvent
    data class MessageDeleted(val messageId: String) : ChatEvent
    data class Typing(val event: TypingEvent) : ChatEvent
    data class ReactionUpdated(val event: ReactionEvent) : ChatEvent
    data class MessagePinned(val messageId: String) : ChatEvent
    data class MessageUnpinned(val messageId: String) : ChatEvent
    data class ReadReceipt(val event: ReadReceiptEvent) : ChatEvent
}
