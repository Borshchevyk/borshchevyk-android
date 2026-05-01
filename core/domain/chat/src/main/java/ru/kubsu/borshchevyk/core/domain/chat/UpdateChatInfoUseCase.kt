package ru.kubsu.borshchevyk.core.domain.chat

import ru.kubsu.borshchevyk.core.model.domain.DomainUpdateChatInfoParam
import javax.inject.Inject

/**
 * Use case for updating the basic metadata (info) of an existing chat.
 *
 * This allows changing the title or description of a group chat. The current user
 * typically needs the appropriate permissions to perform this modification.
 *
 * @property chatRepository The repository handling the update operation.
 */
class UpdateChatInfoUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    /**
     * Executes the action to update chat information.
     *
     * @param chatId The unique identifier of the chat to be updated.
     * @param title The new title for the chat. If null, the title remains unchanged.
     * @param description The new description for the chat. If null, the description remains unchanged.
     */
    suspend operator fun invoke(chatId: String, title: String?, description: String?) {
        chatRepository.updateChatInfo(chatId, DomainUpdateChatInfoParam(title, description))
    }
}
