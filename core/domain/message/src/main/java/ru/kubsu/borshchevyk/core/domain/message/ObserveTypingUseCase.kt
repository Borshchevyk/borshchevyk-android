package ru.kubsu.borshchevyk.core.domain.message

import kotlinx.coroutines.flow.Flow
import ru.kubsu.borshchevyk.core.model.domain.DomainTypingEvent
import javax.inject.Inject

class ObserveTypingUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    operator fun invoke(chatId: String): Flow<DomainTypingEvent> {
        return messageRepository.observeTyping(chatId)
    }
}
