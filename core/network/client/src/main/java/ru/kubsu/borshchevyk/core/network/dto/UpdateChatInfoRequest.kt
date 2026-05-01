package ru.kubsu.borshchevyk.core.network.dto

import kotlinx.serialization.Serializable

/**
 * Request object used to update the general information of a chat.
 *
 * @property title The new title for the chat, if updating.
 * @property description The new description for the chat, if updating.
 * @property commentsEnabled Indicates whether comments should be enabled for the chat (e.g., for channel-like groups).
 */
@Serializable
data class UpdateChatInfoRequest(
    val title: String? = null,
    val description: String? = null,
    val commentsEnabled: Boolean? = null
)