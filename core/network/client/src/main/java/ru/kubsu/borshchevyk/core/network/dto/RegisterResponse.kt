package ru.kubsu.borshchevyk.core.network.dto

import kotlinx.serialization.Serializable

/**
 * Response object returned upon successful registration.
 *
 * @property userId The unique identifier assigned to the newly created user account.
 */
@Serializable
data class RegisterResponse(
    val userId: String
)
