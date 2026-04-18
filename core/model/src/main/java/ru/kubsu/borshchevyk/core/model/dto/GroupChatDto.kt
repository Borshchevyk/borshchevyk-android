package ru.kubsu.borshchevyk.core.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class PageResponse<T>(
    val content: List<T>,
    val totalElements: Long = 0,
    val totalPages: Int = 0,
    val size: Int = 0,
    val number: Int = 0
)

@Serializable
data class ChatMemberResponse(
    val chatId: String,
    val userId: String,
    val role: String,
    val joinedAt: String,
    val canSendMessages: Boolean,
    val canDeleteMessages: Boolean,
    val canInviteUsers: Boolean,
    val canChangeInfo: Boolean,
    val historyClearedAt: String? = null
)
