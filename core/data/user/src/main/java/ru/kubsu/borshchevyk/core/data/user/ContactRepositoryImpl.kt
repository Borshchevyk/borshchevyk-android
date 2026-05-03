package ru.kubsu.borshchevyk.core.data.user

import ru.kubsu.borshchevyk.core.domain.user.ContactRepository
import ru.kubsu.borshchevyk.core.model.domain.Contact
import ru.kubsu.borshchevyk.core.model.domain.DomainAddContactParam
import ru.kubsu.borshchevyk.core.network.client.getOrThrow
import ru.kubsu.borshchevyk.core.network.dto.AddContactRequest
import ru.kubsu.borshchevyk.core.network.dto.ContactResponse
import ru.kubsu.borshchevyk.core.network.user.ContactNetworkDataSource
import javax.inject.Inject

/**
 * Implementation of [ContactRepository] managing the user's contact list.
 *
 * Handles adding, fetching, and deleting contacts via the network.
 *
 * @property networkDataSource Source for contact-related REST API operations.
 */
class ContactRepositoryImpl @Inject constructor(
    private val networkDataSource: ContactNetworkDataSource
) : ContactRepository {

    /**
     * Fetches the current user's contact list from the backend.
     *
     * @return A list of [Contact] objects.
     */
    override suspend fun getContacts(): List<Contact> {
        return networkDataSource.getContacts().getOrThrow().map { it.toDomain() }
    }

    /**
     * Adds a new contact by target user ID.
     *
     * @param request The parameters including the target user's ID, first name, and last name.
     * @return The newly added [Contact].
     */
    override suspend fun addContact(request: DomainAddContactParam): Contact {
        return networkDataSource.addContact(
            AddContactRequest(
                targetUserId = request.targetUserId,
                firstName = request.firstName,
                lastName = request.lastName
            )
        ).getOrThrow().toDomain()
    }

    /**
     * Deletes a contact by their user ID.
     *
     * @param contactUserId The user ID of the contact to remove.
     */
    override suspend fun deleteContact(contactUserId: String) {
        networkDataSource.deleteContact(contactUserId).getOrThrow()
    }

    /**
     * Extension to map [ContactResponse] DTO to its domain equivalent [Contact].
     */
    private fun ContactResponse.toDomain(): Contact = Contact(
        id = id,
        ownerId = ownerId,
        contactUserId = contactUserId,
        contactFirstName = contactFirstName,
        contactLastName = contactLastName,
        addedAt = addedAt
    )
}
