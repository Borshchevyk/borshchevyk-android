package ru.kubsu.borshchevyk.core.network.chat

import ru.kubsu.borshchevyk.core.network.client.NetworkResult
import ru.kubsu.borshchevyk.core.network.dto.ChatMemberResponse
import ru.kubsu.borshchevyk.core.network.dto.ChatResponse
import ru.kubsu.borshchevyk.core.network.dto.CreateChatRequest
import ru.kubsu.borshchevyk.core.network.dto.GlobalSearchResponse
import ru.kubsu.borshchevyk.core.network.dto.PageResponse
import ru.kubsu.borshchevyk.core.network.dto.TargetUserRequest
import ru.kubsu.borshchevyk.core.network.dto.UpdateChatInfoRequest
import ru.kubsu.borshchevyk.core.network.dto.UpdatePermissionsRequest

/**
 * Data source interface defining operations for chat and group management.
 */
interface ChatNetworkDataSource {
    /**
     * Creates a new chat (e.g., a group chat).
     *
     * @param request The [CreateChatRequest] defining the chat details.
     * @return A [NetworkResult] containing the created [ChatResponse].
     */
    suspend fun createChat(request: CreateChatRequest): NetworkResult<ChatResponse>

    /**
     * Creates or retrieves an existing direct chat with a specific user.
     *
     * @param request The [TargetUserRequest] identifying the target user.
     * @return A [NetworkResult] containing the [ChatResponse].
     */
    suspend fun createPrivateChat(request: TargetUserRequest): NetworkResult<ChatResponse>

    /**
     * Fetches all chats that the current user is a participant in.
     *
     * @return A [NetworkResult] containing a list of [ChatResponse].
     */
    suspend fun getUserChats(): NetworkResult<List<ChatResponse>>

    /**
     * Updates the permissions of a specific user within a chat.
     *
     * @param chatId The ID of the chat.
     * @param targetUserId The ID of the user whose permissions are being modified.
     * @param request The [UpdatePermissionsRequest] containing the new permissions.
     * @return A [NetworkResult] indicating success or failure.
     */
    suspend fun updatePermissions(chatId: String, targetUserId: String, request: UpdatePermissionsRequest): NetworkResult<Unit>

    /**
     * Updates general chat information (e.g., title, description).
     *
     * @param chatId The ID of the chat.
     * @param request The [UpdateChatInfoRequest] containing the updated info.
     * @return A [NetworkResult] indicating success or failure.
     */
    suspend fun updateChatInfo(chatId: String, request: UpdateChatInfoRequest): NetworkResult<Unit>

    /**
     * Clears the message history of a chat.
     *
     * @param chatId The ID of the chat.
     * @param forAll If true, clears history for all participants (if permitted).
     * @return A [NetworkResult] indicating success or failure.
     */
    suspend fun clearChatHistory(chatId: String, forAll: Boolean): NetworkResult<Unit>

    /**
     * Permanently deletes a chat and all its history.
     *
     * @param chatId The ID of the chat.
     * @return A [NetworkResult] indicating success or failure.
     */
    suspend fun deleteChat(chatId: String): NetworkResult<Unit>

    /**
     * Retrieves a paginated list of members in a specific chat.
     *
     * @param chatId The ID of the chat.
     * @param page The zero-based page index.
     * @param size The requested number of items per page.
     * @return A [NetworkResult] containing a [PageResponse] of [ChatMemberResponse].
     */
    suspend fun getChatMembers(chatId: String, page: Int, size: Int): NetworkResult<PageResponse<ChatMemberResponse>>

    /**
     * Invites a specific user to join a chat.
     *
     * @param chatId The ID of the chat.
     * @param request The [TargetUserRequest] indicating the user to invite.
     * @return A [NetworkResult] indicating success or failure.
     */
    suspend fun inviteUser(chatId: String, request: TargetUserRequest): NetworkResult<Unit>

    /**
     * Removes a user from a chat.
     *
     * @param chatId The ID of the chat.
     * @param targetUserId The ID of the user to be removed.
     * @return A [NetworkResult] indicating success or failure.
     */
    suspend fun kickUser(chatId: String, targetUserId: String): NetworkResult<Unit>

    /**
     * Leaves the specified chat.
     *
     * @param chatId The ID of the chat to leave.
     * @return A [NetworkResult] indicating success or failure.
     */
    suspend fun leaveChat(chatId: String): NetworkResult<Unit>

    /**
     * Generates an invite link for a chat.
     *
     * @param chatId The ID of the chat.
     * @return A [NetworkResult] containing the generated invite link string.
     */
    suspend fun generateInviteLink(chatId: String): NetworkResult<String>

    /**
     * Joins a chat using an invite link code.
     *
     * @param inviteCode The code extracted from an invite link.
     * @return A [NetworkResult] containing the [ChatResponse] of the joined chat.
     */
    suspend fun joinChatByLink(inviteCode: String): NetworkResult<ChatResponse>

    /**
     * Pins a chat to the top of the user's chat list.
     *
     * @param chatId The ID of the chat to pin.
     * @return A [NetworkResult] indicating success or failure.
     */
    suspend fun pinChat(chatId: String): NetworkResult<Unit>

    /**
     * Unpins a previously pinned chat.
     *
     * @param chatId The ID of the chat to unpin.
     * @return A [NetworkResult] indicating success or failure.
     */
    suspend fun unpinChat(chatId: String): NetworkResult<Unit>

    /**
     * Performs a global search across all users and public chats.
     *
     * @param query The search query string.
     * @return A [NetworkResult] containing the [GlobalSearchResponse].
     */
    suspend fun globalSearch(query: String): NetworkResult<GlobalSearchResponse>
}
