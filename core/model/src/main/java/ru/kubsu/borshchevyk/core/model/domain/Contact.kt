package ru.kubsu.borshchevyk.core.model.domain

import kotlinx.serialization.Serializable

@Serializable
data class Contact(
    val id: String,
    val ownerId: String,
    val contactUserId: String,
    val contactFirstName: String? = null,
    val contactLastName: String? = null,
    val addedAt: String
)
