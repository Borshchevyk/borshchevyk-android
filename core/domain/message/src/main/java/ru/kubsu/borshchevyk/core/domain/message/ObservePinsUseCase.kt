package ru.kubsu.borshchevyk.core.domain.message

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObservePinsUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    operator fun invoke(chatId: String): Flow<String> {
        return messageRepository.observePins(chatId)
    }
}
