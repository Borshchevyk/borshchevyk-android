package ru.kubsu.borshchevyk.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a paginated response for lists of items.
 *
 * @param T The type of the content items in the page.
 * @property content The list of items in the current page.
 * @property totalElements The total number of elements available across all pages.
 * @property totalPages The total number of pages available.
 * @property size The requested or maximum size of the page.
 * @property number The zero-based index of the current page.
 */
@Serializable
data class PageResponse<T>(
    val content: List<T>,
    val totalElements: Long = 0,
    val totalPages: Int = 0,
    val size: Int = 0,
    val number: Int = 0
)

/**
 * Details of a member within a chat.
 *
 * @property chatId The unique identifier of the chat.
 * @property userId The unique identifier of the user who is a member.
 * @property userDetails Summarized details of the user.
 * @property role The role of the user in the chat (e.g., MEMBER, ADMIN).
 * @property joinedAt The timestamp when the user joined the chat.
 * @property canSendMessages Indicates if the user has permission to send messages.
 * @property canDeleteMessages Indicates if the user has permission to delete messages.
 * @property canInviteUsers Indicates if the user has permission to invite others.
 * @property canChangeInfo Indicates if the user has permission to change chat info.
 * @property isPinned Indicates if the chat is pinned for this member.
 * @property historyClearedAt The timestamp when the user last cleared their history, if applicable.
 */
@Serializable
data class ChatMemberResponse(
    val chatId: String,
    val userId: String,
    val userDetails: ShortUserDto? = null,
    val role: String,
    val joinedAt: String,
    val canSendMessages: Boolean,
    val canDeleteMessages: Boolean,
    val canInviteUsers: Boolean,
    val canChangeInfo: Boolean,
    @SerialName("pinned")
    val isPinned: Boolean = false,
    val historyClearedAt: String? = null
)
