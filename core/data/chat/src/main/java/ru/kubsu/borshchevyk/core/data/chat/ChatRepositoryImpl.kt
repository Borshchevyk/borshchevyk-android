package ru.kubsu.borshchevyk.core.data.chat

import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import ru.kubsu.borshchevyk.core.database.dao.ChatDao
import ru.kubsu.borshchevyk.core.database.dao.MessageDao
import ru.kubsu.borshchevyk.core.domain.chat.ChatRepository
import ru.kubsu.borshchevyk.core.model.domain.Chat
import ru.kubsu.borshchevyk.core.model.domain.ChatMember
import ru.kubsu.borshchevyk.core.model.domain.ChatType
import ru.kubsu.borshchevyk.core.model.domain.DomainCreateChatParam
import ru.kubsu.borshchevyk.core.model.domain.DomainPage
import ru.kubsu.borshchevyk.core.model.domain.DomainTargetUserParam
import ru.kubsu.borshchevyk.core.model.domain.DomainUpdateChatInfoParam
import ru.kubsu.borshchevyk.core.model.domain.DomainUpdatePermissionsParam
import ru.kubsu.borshchevyk.core.model.domain.GlobalSearchResults
import ru.kubsu.borshchevyk.core.network.client.getOrThrow
import ru.kubsu.borshchevyk.core.network.dto.CreateChatRequest
import ru.kubsu.borshchevyk.core.network.dto.TargetUserRequest
import ru.kubsu.borshchevyk.core.network.dto.UpdateChatInfoRequest
import ru.kubsu.borshchevyk.core.network.dto.UpdatePermissionsRequest
import ru.kubsu.borshchevyk.core.network.chat.ChatNetworkDataSource
import ru.kubsu.borshchevyk.core.network.di.IoDispatcher
import javax.inject.Inject

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Implementation of [ChatRepository] that manages chat list and chat actions.
 *
 * Provides a single source of truth for chats by syncing network data into a local database
 * and serving cached data to the UI to support offline capabilities.
 *
 * @property networkDataSource Source for chat REST API operations.
 * @property chatDao Local Room database DAO for caching chat entities.
 * @property messageDao Local Room database DAO for clearing chat messages when leaving a chat.
 * @property ioDispatcher Coroutine dispatcher for executing I/O bound database and network operations.
 */
class ChatRepositoryImpl @Inject constructor(
    private val networkDataSource: ChatNetworkDataSource,
    private val chatDao: ChatDao,
    private val messageDao: MessageDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ChatRepository {

    private val repositoryScope = CoroutineScope(SupervisorJob() + ioDispatcher)

    /**
     * Observes a continuous stream of the user's chats from the local database.
     *
     * @return A [Flow] emitting a list of [Chat] domain models.
     */
    override fun observeUserChats(): Flow<List<Chat>> = chatDao.observeAllChats().map { entities -> 
        entities.map { it.toDomain() }
    }

    /**
     * Creates a new chat (group or channel).
     *
     * @param request Parameters for creating a chat, including title, type, and members.
     * @return The ID of the newly created chat.
     */
    override suspend fun createChat(request: DomainCreateChatParam): String = withContext(ioDispatcher) {
        val chatEntity = networkDataSource.createChat(
            CreateChatRequest(
                type = request.type,
                title = request.title,
                description = request.description,
                initialMemberIds = request.initialMemberIds
            )
        ).getOrThrow().toEntity()
        chatDao.upsertChat(chatEntity)
        chatEntity.id
    }

    /**
     * Creates a private (one-on-one) chat with a specific user.
     *
     * @param request Parameters containing the target user's ID.
     * @return The ID of the newly created private chat.
     */
    override suspend fun createPrivateChat(request: DomainTargetUserParam): String = withContext(ioDispatcher) {
        val chatEntity = networkDataSource.createPrivateChat(
            TargetUserRequest(request.targetUserId)
        ).getOrThrow().toEntity()
        chatDao.upsertChat(chatEntity)
        chatEntity.id
    }

    /**
     * Synchronizes the user's chats from the remote backend to the local database.
     */
    override suspend fun syncUserChats() {
        withContext(ioDispatcher) {
            val networkChats = networkDataSource.getUserChats().getOrThrow()
            chatDao.upsertChats(networkChats.map { it.toEntity() })
        }
    }

    /**
     * Retrieves the user's chats. 
     * Prioritizes the local cache, falling back to network fetch if cache is empty.
     * Triggers a background sync if cache is used.
     *
     * @return A list of [Chat] domain models.
     */
    override suspend fun getUserChats(): List<Chat> = withContext(ioDispatcher) {
        val cached = chatDao.observeAllChats().firstOrNull()
        if (!cached.isNullOrEmpty()) {
            repositoryScope.launch {
                try {
                    syncUserChats()
                } catch (e: Exception) {
                    Log.w("ChatRepository", "Failed background sync for chats, using cache", e)
                }
            }
            return@withContext cached.map { it.toDomain() }
        }
        val networkChats = networkDataSource.getUserChats().getOrThrow()
        chatDao.upsertChats(networkChats.map { it.toEntity() })
        networkChats.map { it.toEntity().toDomain() }
    }

    /**
     * Updates the permissions of a specific member in a chat.
     *
     * @param chatId The ID of the chat.
     * @param targetUserId The ID of the user whose permissions are being updated.
     * @param request The new permissions.
     */
    override suspend fun updatePermissions(chatId: String, targetUserId: String, request: DomainUpdatePermissionsParam) {
        networkDataSource.updatePermissions(
            chatId, 
            targetUserId, 
            UpdatePermissionsRequest(
                canSendMessages = request.canSendMessages,
                canDeleteMessages = request.canDeleteMessages,
                canInviteUsers = request.canInviteUsers,
                canChangeInfo = request.canChangeInfo
            )
        ).getOrThrow()
    }

    /**
     * Clears the message history of a chat.
     *
     * @param chatId The ID of the chat.
     * @param forAll Whether to clear the history for all participants or just the current user.
     */
    override suspend fun clearChatHistory(chatId: String, forAll: Boolean) {
        networkDataSource.clearChatHistory(chatId, forAll).getOrThrow()
        withContext(ioDispatcher) {
            messageDao.deleteMessagesByChat(chatId)
        }
    }

    /**
     * Deletes a chat entirely.
     *
     * @param chatId The ID of the chat to delete.
     */
    override suspend fun deleteChat(chatId: String) {
        networkDataSource.deleteChat(chatId).getOrThrow()
    }

    /**
     * Retrieves a paginated list of members in a chat.
     *
     * @param chatId The ID of the chat.
     * @param page The zero-based page index.
     * @param size The number of items per page.
     * @return A [DomainPage] containing [ChatMember] objects.
     */
    override suspend fun getChatMembers(chatId: String, page: Int, size: Int): DomainPage<ChatMember> {
        val response = networkDataSource.getChatMembers(chatId, page, size).getOrThrow()
        return DomainPage(
            content = response.content.map { it.toDomain() },
            pageNumber = response.number,
            pageSize = response.size,
            totalElements = response.totalElements,
            totalPages = response.totalPages,
            last = response.content.isEmpty() // simplified last check
        )
    }

    /**
     * Invites a target user to an existing chat.
     *
     * @param chatId The ID of the chat.
     * @param request Parameters containing the target user's ID.
     */
    override suspend fun inviteUser(chatId: String, request: DomainTargetUserParam) {
        networkDataSource.inviteUser(chatId, TargetUserRequest(request.targetUserId)).getOrThrow()
    }

    /**
     * Removes a target user from an existing chat.
     *
     * @param chatId The ID of the chat.
     * @param targetUserId The ID of the user to be removed.
     */
    override suspend fun kickUser(chatId: String, targetUserId: String) {
        networkDataSource.kickUser(chatId, targetUserId).getOrThrow()
    }

    /**
     * Leaves a chat and optionally removes local cached data.
     *
     * @param chatId The ID of the chat to leave.
     */
    override suspend fun leaveChat(chatId: String) {
        networkDataSource.leaveChat(chatId).getOrThrow()
        withContext(ioDispatcher) {
            chatDao.deleteChat(chatId)
            messageDao.deleteMessagesByChat(chatId)
        }
    }

    /**
     * Updates the information (title, description) of an existing chat.
     *
     * @param chatId The ID of the chat.
     * @param request Parameters containing the new chat information.
     */
    override suspend fun updateChatInfo(chatId: String, request: DomainUpdateChatInfoParam) {
        networkDataSource.updateChatInfo(
            chatId, 
            UpdateChatInfoRequest(
                title = request.title,
                description = request.description
            )
        ).getOrThrow()
    }

    /**
     * Generates a unique invite link for a chat.
     *
     * @param chatId The ID of the chat.
     * @return The generated invite link string.
     */
    override suspend fun generateInviteLink(chatId: String): String {
        return networkDataSource.generateInviteLink(chatId).getOrThrow()
    }

    /**
     * Joins a chat using an invite link code.
     *
     * @param inviteCode The invite code extracted from the link.
     * @return The [Chat] domain model of the joined chat.
     */
    override suspend fun joinChatByLink(inviteCode: String): Chat {
        return networkDataSource.joinChatByLink(inviteCode).getOrThrow().toEntity().toDomain()
    }

    /**
     * Pins a chat to the top of the user's chat list.
     *
     * @param chatId The ID of the chat to pin.
     */
    override suspend fun pinChat(chatId: String) {
        networkDataSource.pinChat(chatId).getOrThrow()
    }

    /**
     * Unpins a previously pinned chat from the user's chat list.
     *
     * @param chatId The ID of the chat to unpin.
     */
    override suspend fun unpinChat(chatId: String) {
        networkDataSource.unpinChat(chatId).getOrThrow()
    }

    /**
     * Performs a global search across users and public chats by query.
     *
     * @param query The search term.
     * @return The [GlobalSearchResults] containing matching users and chats.
     */
    override suspend fun globalSearch(query: String): GlobalSearchResults {
        val response = networkDataSource.globalSearch(query).getOrThrow()
        return GlobalSearchResults(
            users = response.users.map { 
                ru.kubsu.borshchevyk.core.model.domain.User(
                    userId = it.id,
                    firstName = it.firstName,
                    lastName = it.lastName,
                    tag = it.tag ?: "",
                    avatarUrl = it.avatarUrl
                )
            },
            chats = response.chats.map {
                Chat(
                    id = it.id,
                    type = ChatType.GROUP, // Global search chats are groups or channels
                    title = it.name,
                    createdAt = "" // default since search dto is short
                )
            }
        )
    }
}
