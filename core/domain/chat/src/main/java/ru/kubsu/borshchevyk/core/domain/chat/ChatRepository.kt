package ru.kubsu.borshchevyk.core.domain.chat

import ru.kubsu.borshchevyk.core.model.domain.Chat
import ru.kubsu.borshchevyk.core.model.domain.ChatMember
import ru.kubsu.borshchevyk.core.model.domain.DomainCreateChatParam
import ru.kubsu.borshchevyk.core.model.domain.DomainPage
import ru.kubsu.borshchevyk.core.model.domain.DomainTargetUserParam
import ru.kubsu.borshchevyk.core.model.domain.DomainUpdateChatInfoParam
import ru.kubsu.borshchevyk.core.model.domain.DomainUpdatePermissionsParam
import ru.kubsu.borshchevyk.core.model.domain.GlobalSearchResults

import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing chats and their associated metadata.
 * 
 * This repository abstracts the data sources (e.g., local database, global server API,
 * or P2P mesh network) and provides a unified interface for chat operations. It handles
 * the creation, retrieval, modification, and deletion of both private and group chats,
 * as well as managing chat members and permissions.
 */
interface ChatRepository {
    /**
     * Creates a new chat (typically a group chat) with the specified parameters.
     *
     * @param request The parameters for creating the chat, including type, title, description, and initial members.
     * @return The unique identifier (UUID) of the newly created chat.
     */
    suspend fun createChat(request: DomainCreateChatParam): String

    /**
     * Creates a direct, private chat with a specific user.
     *
     * @param request The parameters containing the target user's ID.
     * @return The unique identifier (UUID) of the newly created private chat.
     */
    suspend fun createPrivateChat(request: DomainTargetUserParam): String

    /**
     * Returns a reactive flow of the user's chat list.
     * 
     * The emitted list represents the latest known state of all chats the user is a participant of.
     * It should automatically emit a new list whenever chats are added, removed, or updated.
     *
     * @return A [Flow] emitting lists of [Chat] objects.
     */
    fun observeUserChats(): Flow<List<Chat>>

    /**
     * Triggers a synchronization of the user's chats with the remote source
     * (server or mesh peers). This updates the local cache, which in turn
     * should cause [observeUserChats] to emit new data.
     */
    suspend fun syncUserChats()

    /**
     * Fetches the current list of the user's chats directly without observing it.
     *
     * @return A list of [Chat] objects representing the user's current chats.
     */
    suspend fun getUserChats(): List<Chat>

    /**
     * Updates the permissions of a specific member in a chat.
     *
     * @param chatId The ID of the chat.
     * @param targetUserId The ID of the user whose permissions are being modified.
     * @param request The new permissions to apply.
     */
    suspend fun updatePermissions(chatId: String, targetUserId: String, request: DomainUpdatePermissionsParam)

    /**
     * Updates the metadata (e.g., title, description) of an existing chat.
     *
     * @param chatId The ID of the chat to update.
     * @param request The parameters containing the updated chat information.
     */
    suspend fun updateChatInfo(chatId: String, request: DomainUpdateChatInfoParam)

    /**
     * Clears the message history of a specific chat.
     *
     * @param chatId The ID of the chat.
     * @param forAll If true, attempts to clear the history for all participants (requires appropriate permissions).
     *               If false, only clears the history locally for the current user.
     */
    suspend fun clearChatHistory(chatId: String, forAll: Boolean)

    /**
     * Deletes a chat entirely or removes the current user from it, depending on the chat type and permissions.
     *
     * @param chatId The ID of the chat to delete.
     */
    suspend fun deleteChat(chatId: String)
    
    /**
     * Retrieves a paginated list of members for a specific chat.
     *
     * @param chatId The ID of the chat.
     * @param page The zero-based page index.
     * @param size The number of members to retrieve per page.
     * @return A [DomainPage] containing the requested chunk of [ChatMember]s.
     */
    suspend fun getChatMembers(chatId: String, page: Int, size: Int): DomainPage<ChatMember>

    /**
     * Invites a new user to an existing chat.
     *
     * @param chatId The ID of the chat.
     * @param request The parameters containing the target user's ID to invite.
     */
    suspend fun inviteUser(chatId: String, request: DomainTargetUserParam)

    /**
     * Removes (kicks) a specific user from a chat.
     *
     * @param chatId The ID of the chat.
     * @param targetUserId The ID of the user to remove.
     */
    suspend fun kickUser(chatId: String, targetUserId: String)

    /**
     * Causes the current authenticated user to leave the specified chat.
     *
     * @param chatId The ID of the chat to leave.
     */
    suspend fun leaveChat(chatId: String)

    /**
     * Generates an invitation link or code for a chat, allowing others to join.
     *
     * @param chatId The ID of the chat.
     * @return A string representing the generated invite link or code.
     */
    suspend fun generateInviteLink(chatId: String): String

    /**
     * Joins a chat using an invitation code or link.
     *
     * @param inviteCode The invitation string.
     * @return The [Chat] that was joined.
     */
    suspend fun joinChatByLink(inviteCode: String): Chat

    /**
     * Pins a chat to the top of the user's chat list.
     *
     * @param chatId The ID of the chat to pin.
     */
    suspend fun pinChat(chatId: String)

    /**
     * Unpins a previously pinned chat from the top of the user's chat list.
     *
     * @param chatId The ID of the chat to unpin.
     */
    suspend fun unpinChat(chatId: String)

    /**
     * Performs a global search across chats and messages.
     *
     * @param query The search term.
     * @return A [GlobalSearchResults] object containing the found chats, messages, or users.
     */
    suspend fun globalSearch(query: String): GlobalSearchResults
}
