package ru.kubsu.borshchevyk.core.domain.chat

import javax.inject.Inject

/**
 * Use case for generating an invitation link or code for a specific chat.
 *
 * This allows administrators or members with appropriate permissions to create
 * a shareable link that other users can use to join the chat. This is typically
 * used for group chats.
 *
 * @property chatRepository The repository handling the generation of the invite link.
 */
class GenerateInviteLinkUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    /**
     * Executes the action to generate an invite link.
     *
     * @param chatId The unique identifier of the chat for which to generate the link.
     * @return A string representing the generated invitation link or code.
     */
    suspend operator fun invoke(chatId: String): String {
        return chatRepository.generateInviteLink(chatId)
    }
}
