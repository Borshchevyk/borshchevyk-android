package ru.kubsu.borshchevyk.core.network.dto

import kotlinx.serialization.Serializable

/**
 * Response object containing a cryptographic challenge for authentication.
 *
 * @property challenge The cryptographic challenge string to be signed.
 */
@Serializable
data class ChallengeResponse(
    val challenge: String
)
