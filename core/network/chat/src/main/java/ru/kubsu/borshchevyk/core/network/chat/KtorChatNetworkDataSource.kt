package ru.kubsu.borshchevyk.core.network.chat

import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import ru.kubsu.borshchevyk.core.network.client.NetworkResult
import ru.kubsu.borshchevyk.core.network.di.IoDispatcher
import ru.kubsu.borshchevyk.core.network.dto.ChatMemberResponse
import ru.kubsu.borshchevyk.core.network.dto.ChatResponse
import ru.kubsu.borshchevyk.core.network.dto.CreateChatRequest
import ru.kubsu.borshchevyk.core.network.dto.GlobalSearchResponse
import ru.kubsu.borshchevyk.core.network.dto.PageResponse
import ru.kubsu.borshchevyk.core.network.dto.TargetUserRequest
import ru.kubsu.borshchevyk.core.network.dto.UpdateChatInfoRequest
import ru.kubsu.borshchevyk.core.network.dto.UpdatePermissionsRequest
import ru.kubsu.borshchevyk.core.network.ktor.client.safeRequest
import javax.inject.Inject

class KtorChatNetworkDataSource @Inject constructor(
    private val httpClient: HttpClient,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ChatNetworkDataSource {

    override suspend fun globalSearch(query: String): NetworkResult<GlobalSearchResponse> {
        return withContext(ioDispatcher) {
            safeRequest {
                httpClient.get("api/v1/search") {
                    parameter("query", query)
                }
            }
        }
    }

    override suspend fun createChat(request: CreateChatRequest): NetworkResult<ChatResponse> {
        return withContext(ioDispatcher) {
            safeRequest {
                httpClient.post("api/v1/chats") {
                    setBody(request)
                }
            }
        }
    }

    override suspend fun createPrivateChat(request: TargetUserRequest): NetworkResult<ChatResponse> {
        return withContext(ioDispatcher) {
            safeRequest {
                httpClient.post("api/v1/chats/private") {
                    setBody(request)
                }
            }
        }
    }

    override suspend fun getUserChats(): NetworkResult<List<ChatResponse>> {
        return withContext(ioDispatcher) {
            safeRequest {
                httpClient.get("api/v1/chats")
            }
        }
    }

    override suspend fun updatePermissions(
        chatId: String,
        targetUserId: String,
        request: UpdatePermissionsRequest
    ): NetworkResult<Unit> {
        return withContext(ioDispatcher) {
            safeRequest {
                httpClient.patch("api/v1/chats/$chatId/members/$targetUserId/permissions") {
                    setBody(request)
                }
            }
        }
    }

    override suspend fun clearChatHistory(chatId: String, forAll: Boolean): NetworkResult<Unit> {
        return withContext(ioDispatcher) {
            safeRequest {
                httpClient.delete("api/v1/chats/$chatId/history") {
                    parameter("forAll", forAll)
                }
            }
        }
    }

    override suspend fun deleteChat(chatId: String): NetworkResult<Unit> {
        return withContext(ioDispatcher) {
            safeRequest {
                httpClient.delete("api/v1/chats/$chatId")
            }
        }
    }

    override suspend fun getChatMembers(chatId: String, page: Int, size: Int): NetworkResult<PageResponse<ChatMemberResponse>> {
        return withContext(ioDispatcher) {
            safeRequest {
                httpClient.get("api/v1/chats/$chatId/members") {
                    parameter("page", page)
                    parameter("size", size)
                }
            }
        }
    }

    override suspend fun inviteUser(chatId: String, request: TargetUserRequest): NetworkResult<Unit> {
        return withContext(ioDispatcher) {
            safeRequest {
                httpClient.post("api/v1/chats/$chatId/members") {
                    setBody(request)
                }
            }
        }
    }

    override suspend fun kickUser(chatId: String, targetUserId: String): NetworkResult<Unit> {
        return withContext(ioDispatcher) {
            safeRequest {
                httpClient.delete("api/v1/chats/$chatId/members/$targetUserId")
            }
        }
    }

    override suspend fun leaveChat(chatId: String): NetworkResult<Unit> {
        return withContext(ioDispatcher) {
            safeRequest {
                httpClient.delete("api/v1/chats/$chatId/members/me")
            }
        }
    }

    override suspend fun updateChatInfo(chatId: String, request: UpdateChatInfoRequest): NetworkResult<Unit> {
        return withContext(ioDispatcher) {
            safeRequest {
                httpClient.patch("api/v1/chats/$chatId") {
                    setBody(request)
                }
            }
        }
    }

    override suspend fun generateInviteLink(chatId: String): NetworkResult<String> {
        return withContext(ioDispatcher) {
            safeRequest {
                httpClient.post("api/v1/chats/$chatId/invite-link")
            }
        }
    }

    override suspend fun joinChatByLink(inviteCode: String): NetworkResult<ChatResponse> {
        return withContext(ioDispatcher) {
            safeRequest {
                httpClient.post("api/v1/chats/join/$inviteCode")
            }
        }
    }

    override suspend fun pinChat(chatId: String): NetworkResult<Unit> {
        return withContext(ioDispatcher) {
            safeRequest {
                httpClient.post("api/v1/chats/$chatId/pin")
            }
        }
    }

    override suspend fun unpinChat(chatId: String): NetworkResult<Unit> {
        return withContext(ioDispatcher) {
            safeRequest {
                httpClient.delete("api/v1/chats/$chatId/pin")
            }
        }
    }
}
