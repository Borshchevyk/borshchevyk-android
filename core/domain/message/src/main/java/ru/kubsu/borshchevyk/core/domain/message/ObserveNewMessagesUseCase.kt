package ru.kubsu.borshchevyk.core.domain.message

import kotlinx.coroutines.flow.Flow
import ru.kubsu.borshchevyk.core.model.domain.Message
import javax.inject.Inject

class ObserveNewMessagesUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    operator fun invoke(): Flow<Message> {
        return messageRepository.observeNewMessages()
    }
}
