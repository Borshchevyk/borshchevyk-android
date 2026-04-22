package ru.kubsu.borshchevyk.core.domain.message

import kotlinx.coroutines.flow.Flow
import ru.kubsu.borshchevyk.core.model.dto.NotificationDto
import javax.inject.Inject

class ObserveNewMessagesUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    operator fun invoke(): Flow<NotificationDto.MessageDto> {
        return messageRepository.observeNewMessages()
    }
}
