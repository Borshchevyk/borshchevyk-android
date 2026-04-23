package ru.kubsu.borshchevyk.core.network.chat

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import ru.kubsu.borshchevyk.core.model.dto.ChatMemberResponse
import ru.kubsu.borshchevyk.core.model.dto.ChatResponse
import ru.kubsu.borshchevyk.core.model.dto.CreateChatRequest
import ru.kubsu.borshchevyk.core.model.dto.PageResponse
import ru.kubsu.borshchevyk.core.model.dto.TargetUserRequest
import ru.kubsu.borshchevyk.core.model.dto.UpdateChatInfoRequest
import ru.kubsu.borshchevyk.core.model.dto.UpdatePermissionsRequest
import ru.kubsu.borshchevyk.core.network.di.IoDispatcher
import javax.inject.Inject

class KtorChatNetworkDataSource @Inject constructor(
    private val httpClient: HttpClient,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ChatNetworkDataSource {

    override suspend fun createChat(request: CreateChatRequest): ChatResponse {
        return withContext(ioDispatcher) {
            httpClient.post("api/v1/chats") {
                setBody(request)
            }.body()
        }
    }

    override suspend fun createPrivateChat(request: TargetUserRequest): ChatResponse {
        return withContext(ioDispatcher) {
            httpClient.post("api/v1/chats/private") {
                setBody(request)
            }.body()
        }
    }

    override suspend fun getUserChats(): List<ChatResponse> {
        return withContext(ioDispatcher) {
            httpClient.get("api/v1/chats").body()
        }
    }

    override suspend fun updatePermissions(
        chatId: String,
        targetUserId: String,
        request: UpdatePermissionsRequest
    ) {
        httpClient.patch("api/v1/chats/$chatId/members/$targetUserId/permissions") {
            setBody(request)
        }
    }

    override suspend fun clearChatHistory(chatId: String, forAll: Boolean) {
        return withContext(ioDispatcher) {
            httpClient.delete("api/v1/chats/$chatId/history") {
                parameter("forAll", forAll)
            }
        }
    }

    override suspend fun deleteChat(chatId: String) {
        return withContext(ioDispatcher) {
            httpClient.delete("api/v1/chats/$chatId")
        }
    }

    override suspend fun getChatMembers(chatId: String, page: Int, size: Int): PageResponse<ChatMemberResponse> {
        return withContext(ioDispatcher) {
            httpClient.get("api/v1/chats/$chatId/members") {
                parameter("page", page)
                parameter("size", size)
            }.body()
        }
    }

    override suspend fun inviteUser(chatId: String, request: TargetUserRequest) {
        return withContext(ioDispatcher) {
            httpClient.post("api/v1/chats/$chatId/members") {
                setBody(request)
            }
        }
    }

    override suspend fun kickUser(chatId: String, targetUserId: String) {
        return withContext(ioDispatcher) {
            httpClient.delete("api/v1/chats/$chatId/members/$targetUserId")
        }
    }

    override suspend fun leaveChat(chatId: String) {
        return withContext(ioDispatcher) {
            httpClient.delete("api/v1/chats/$chatId/members/me")
        }
    }

    override suspend fun updateChatInfo(chatId: String, request: UpdateChatInfoRequest) {
        return withContext(ioDispatcher) {
            httpClient.patch("api/v1/chats/$chatId") {
                setBody(request)
            }
        }
    }

    override suspend fun generateInviteLink(chatId: String): String {
        return withContext(ioDispatcher) {
            httpClient.post("api/v1/chats/$chatId/invite-link").body()
        }
    }

    override suspend fun joinChatByLink(inviteCode: String): ChatResponse {
        return withContext(ioDispatcher) {
            httpClient.post("api/v1/chats/join/$inviteCode").body()
        }
    }
}
