package ru.kubsu.borshchevyk.core.network.dto

import kotlinx.serialization.Serializable

/**
 * Request object containing a cryptographic signature to verify an authentication challenge.
 *
 * @property userId The unique identifier of the user verifying the challenge.
 * @property signature The cryptographic signature of the challenge.
 */
@Serializable
data class VerifyRequest(
    val userId: String,
    val signature: String
)
