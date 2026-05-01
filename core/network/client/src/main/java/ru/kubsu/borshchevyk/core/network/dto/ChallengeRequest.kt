package ru.kubsu.borshchevyk.core.network.dto

import kotlinx.serialization.Serializable

/**
 * Request object to retrieve a cryptographic challenge during authentication.
 *
 * @property userId The unique identifier of the user requesting the challenge.
 */
@Serializable
data class ChallengeRequest(
    val userId: String
)
