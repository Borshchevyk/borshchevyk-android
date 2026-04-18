package ru.kubsu.borshchevyk.core.domain.message

import kotlinx.coroutines.flow.Flow
import ru.kubsu.borshchevyk.core.model.dto.TypingEvent
import javax.inject.Inject

class ObserveTypingUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    operator fun invoke(chatId: String): Flow<TypingEvent> {
        return messageRepository.observeTyping(chatId)
    }
}
