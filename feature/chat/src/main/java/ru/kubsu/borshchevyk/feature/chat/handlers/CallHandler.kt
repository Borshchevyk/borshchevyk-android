package ru.kubsu.borshchevyk.feature.chat.handlers

import ru.kubsu.borshchevyk.core.domain.call.usecase.CreateCallUseCase
import ru.kubsu.borshchevyk.core.domain.chat.GetChatMembersUseCase
import javax.inject.Inject

/**
 * Handler responsible for initiating calls within a chat.
 *
 * @property getChatMembersUseCase Use case to retrieve members of a chat.
 * @property createCallUseCase Use case to create a new call with participants.
 */
class CallHandler @Inject constructor(
    private val getChatMembersUseCase: GetChatMembersUseCase,
    private val createCallUseCase: CreateCallUseCase
) {
    /**
     * Initiates a call with all other members of the specified chat.
     *
     * Retrieves the members of the chat, filters out the current user,
     * and attempts to create a call with the remaining participants.
     *
     * @param chatId The unique identifier of the chat where the call is initiated.
     * @param currentUserId The unique identifier of the user initiating the call.
     * @return The ID of the created call, or `null` if the call could not be created
     *         (e.g., if there are no other participants in the chat).
     */
    suspend fun initiateCall(chatId: String, currentUserId: String): String? {
        val members = getChatMembersUseCase(chatId, 0, 100)
        val participantIds = members.content.map { it.userId }.filter { it != currentUserId }
        
        if (participantIds.isNotEmpty()) {
            return createCallUseCase(participantIds)
        }
        return null
    }
}
