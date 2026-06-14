package ru.kubsu.borshchevyk.feature.chat.conversation.interactor

import kotlinx.coroutines.flow.Flow
import ru.kubsu.borshchevyk.core.domain.chat.GetUserChatsUseCase
import ru.kubsu.borshchevyk.core.domain.message.usecase.ObserveUserPresenceUseCase
import ru.kubsu.borshchevyk.core.model.domain.DomainPresenceStatus
import javax.inject.Inject

class ChatPresenceHandler @Inject constructor(
    private val getUserChatsUseCase: GetUserChatsUseCase,
    private val observeUserPresenceUseCase: ObserveUserPresenceUseCase
) {
    suspend fun getPartnerId(chatId: String): String? {
        return getUserChatsUseCase().find { it.id == chatId }?.partnerId
    }

    fun observePresence(partnerId: String): Flow<DomainPresenceStatus> = observeUserPresenceUseCase(partnerId)  
}
