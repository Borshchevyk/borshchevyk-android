package ru.kubsu.borshchevyk.core.network.dto

import kotlinx.serialization.Serializable

/**
 * DTO for updating a member's state via CRDT over Mesh network.
 */
@Serializable
data class CrdtMemberUpdateEvent(
    val chatId: String,
    val targetUserId: String,
    val role: String? = null,
    val status: String? = null, // ACTIVE, KICKED, LEFT
    val canSendMessages: Boolean? = null,
    val canDeleteMessages: Boolean? = null,
    val canInviteUsers: Boolean? = null,
    val canChangeInfo: Boolean? = null,
    
    val roleUpdatedAt: Long = 0,
    val statusUpdatedAt: Long = 0,
    val permissionsUpdatedAt: Long = 0
)

/**
 * DTO for updating the group chat's title and description via CRDT.
 */
@Serializable
data class CrdtChatUpdateEvent(
    val chatId: String,
    val title: String? = null,
    val description: String? = null,
    val titleUpdatedAt: Long = 0,
    val descriptionUpdatedAt: Long = 0
)

/**
 * DTO for synchronizing the full state of a group to a newly joined member.
 */
@Serializable
data class FullStateSyncEvent(
    val chatUpdate: CrdtChatUpdateEvent,
    val memberUpdates: List<CrdtMemberUpdateEvent>
)