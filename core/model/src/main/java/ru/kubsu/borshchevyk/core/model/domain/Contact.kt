package ru.kubsu.borshchevyk.core.model.domain

import kotlinx.serialization.Serializable

/**
 * Domain model representing an entry in the user's contact list.
 *
 * Associates the current user (owner) with another user (contact) and
 * allows overriding the contact's name for local display.
 *
 * @property id The unique identifier for this contact relationship.
 * @property ownerId The ID of the user who owns this contact entry.
 * @property contactUserId The ID of the actual user added as a contact.
 * @property contactFirstName A locally overridden first name for this contact, if set.
 * @property contactLastName A locally overridden last name for this contact, if set.
 * @property addedAt ISO 8601 formatted timestamp indicating when the contact was added.
 */
@Serializable
data class Contact(
    val id: String,
    val ownerId: String,
    val contactUserId: String,
    val contactFirstName: String? = null,
    val contactLastName: String? = null,
    val addedAt: String,
    val contactAvatarUrl: String? = null
)
