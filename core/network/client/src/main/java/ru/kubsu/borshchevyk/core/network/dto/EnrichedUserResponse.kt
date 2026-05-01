package ru.kubsu.borshchevyk.core.network.dto

import kotlinx.serialization.Serializable

/**
 * Response object containing enriched details about a user.
 *
 * @property id The unique identifier of the user.
 * @property firstName The first name of the user.
 * @property lastName The last name of the user.
 * @property tag The user's tag or username handle.
 * @property avatarUrl The URL of the user's avatar image.
 */
@Serializable
data class EnrichedUserResponse(
    val id: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val tag: String? = null,
    val avatarUrl: String? = null
)
