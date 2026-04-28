package ru.kubsu.borshchevyk.core.domain.message

import kotlinx.coroutines.flow.Flow
import ru.kubsu.borshchevyk.core.model.domain.DomainReactionEvent
import javax.inject.Inject

class ObserveReactionsUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    operator fun invoke(chatId: String): Flow<DomainReactionEvent> {
        return messageRepository.observeReactions(chatId)
    }
}
