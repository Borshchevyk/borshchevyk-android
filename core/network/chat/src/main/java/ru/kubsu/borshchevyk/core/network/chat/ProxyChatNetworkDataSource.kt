package ru.kubsu.borshchevyk.core.network.chat

import ru.kubsu.borshchevyk.core.network.client.NetworkMode
import ru.kubsu.borshchevyk.core.network.client.NetworkResult
import ru.kubsu.borshchevyk.core.network.client.TransportModeManager
import ru.kubsu.borshchevyk.core.network.dto.ChatMemberResponse
import ru.kubsu.borshchevyk.core.network.dto.ChatResponse
import ru.kubsu.borshchevyk.core.network.dto.CreateChatRequest
import ru.kubsu.borshchevyk.core.network.dto.GlobalSearchResponse
import ru.kubsu.borshchevyk.core.network.dto.PageResponse
import ru.kubsu.borshchevyk.core.network.dto.TargetUserRequest
import ru.kubsu.borshchevyk.core.network.dto.UpdateChatInfoRequest
import ru.kubsu.borshchevyk.core.network.dto.UpdatePermissionsRequest
import javax.inject.Inject

class ProxyChatNetworkDataSource @Inject constructor(
    private val ktorDataSource: KtorChatNetworkDataSource,
    private val meshDataSource: MeshChatNetworkDataSource,
    private val transportModeManager: TransportModeManager
) : ChatNetworkDataSource {

    private val dataSource: ChatNetworkDataSource
        get() = if (transportModeManager.networkMode.value == NetworkMode.MESH) {
            meshDataSource
        } else {
            ktorDataSource
        }

    override suspend fun createChat(request: CreateChatRequest): NetworkResult<ChatResponse> =
        dataSource.createChat(request)

    override suspend fun createPrivateChat(request: TargetUserRequest): NetworkResult<ChatResponse> =
        dataSource.createPrivateChat(request)

    override suspend fun getUserChats(): NetworkResult<List<ChatResponse>> =
        dataSource.getUserChats()

    override suspend fun updatePermissions(
        chatId: String,
        targetUserId: String,
        request: UpdatePermissionsRequest
    ): NetworkResult<Unit> = dataSource.updatePermissions(chatId, targetUserId, request)

    override suspend fun updateChatInfo(
        chatId: String,
        request: UpdateChatInfoRequest
    ): NetworkResult<Unit> = dataSource.updateChatInfo(chatId, request)

    override suspend fun clearChatHistory(chatId: String, forAll: Boolean): NetworkResult<Unit> =
        dataSource.clearChatHistory(chatId, forAll)

    override suspend fun deleteChat(chatId: String): NetworkResult<Unit> =
        dataSource.deleteChat(chatId)

    override suspend fun getChatMembers(
        chatId: String,
        page: Int,
        size: Int
    ): NetworkResult<PageResponse<ChatMemberResponse>> =
        dataSource.getChatMembers(chatId, page, size)

    override suspend fun inviteUser(chatId: String, request: TargetUserRequest): NetworkResult<Unit> =
        dataSource.inviteUser(chatId, request)

    override suspend fun kickUser(chatId: String, targetUserId: String): NetworkResult<Unit> =
        dataSource.kickUser(chatId, targetUserId)

    override suspend fun leaveChat(chatId: String): NetworkResult<Unit> =
        dataSource.leaveChat(chatId)

    override suspend fun generateInviteLink(chatId: String): NetworkResult<String> =
        dataSource.generateInviteLink(chatId)

    override suspend fun joinChatByLink(inviteCode: String): NetworkResult<ChatResponse> =
        dataSource.joinChatByLink(inviteCode)

    override suspend fun pinChat(chatId: String): NetworkResult<Unit> =
        dataSource.pinChat(chatId)

    override suspend fun unpinChat(chatId: String): NetworkResult<Unit> =
        dataSource.unpinChat(chatId)

    override suspend fun globalSearch(query: String): NetworkResult<GlobalSearchResponse> =
        dataSource.globalSearch(query)
}
