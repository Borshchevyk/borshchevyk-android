package ru.kubsu.borshchevyk.core.network.dto

import kotlinx.serialization.Serializable

/**
 * The standard response object containing details about a contact relationship.
 *
 * @property id The unique identifier of the contact entry.
 * @property ownerId The unique identifier of the user who owns this contact entry.
 * @property contactUserId The unique identifier of the target user in the contact entry.
 * @property contactFirstName The first name of the contact as saved by the owner.
 * @property contactLastName The last name of the contact as saved by the owner.
 * @property addedAt The timestamp when this contact was added.
 */
@Serializable
data class ContactResponse(
    val id: String,
    val ownerId: String,
    val contactUserId: String,
    val contactFirstName: String? = null,
    val contactLastName: String? = null,
    val addedAt: String
)

/**
 * Request object used to add a new contact.
 *
 * @property targetUserId The unique identifier of the user to be added as a contact.
 * @property firstName The first name to assign to the contact.
 * @property lastName The optional last name to assign to the contact.
 */
@Serializable
data class AddContactRequest(
    val targetUserId: String,
    val firstName: String,
    val lastName: String? = null
)
