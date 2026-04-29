package ru.kubsu.borshchevyk.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class ContactResponse(
    val id: String,
    val ownerId: String,
    val contactUserId: String,
    val contactFirstName: String? = null,
    val contactLastName: String? = null,
    val addedAt: String
)

@Serializable
data class AddContactRequest(
    val targetUserId: String,
    val firstName: String,
    val lastName: String? = null
)
