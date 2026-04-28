package ru.kubsu.borshchevyk.feature.chat.handlers

import ru.kubsu.borshchevyk.core.domain.call.usecase.CreateCallUseCase
import ru.kubsu.borshchevyk.core.domain.chat.GetChatMembersUseCase
import javax.inject.Inject

class CallHandler @Inject constructor(
    private val getChatMembersUseCase: GetChatMembersUseCase,
    private val createCallUseCase: CreateCallUseCase
) {
    suspend fun initiateCall(chatId: String, currentUserId: String): String? {
        val members = getChatMembersUseCase(chatId, 0, 100)
        val participantIds = members.content.map { it.userId }.filter { it != currentUserId }
        
        if (participantIds.isNotEmpty()) {
            return createCallUseCase(participantIds)
        }
        return null
    }
}
