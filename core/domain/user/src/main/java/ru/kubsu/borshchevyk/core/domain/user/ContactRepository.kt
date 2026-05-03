package ru.kubsu.borshchevyk.core.domain.user

import ru.kubsu.borshchevyk.core.model.domain.Contact
import ru.kubsu.borshchevyk.core.model.domain.DomainAddContactParam

/**
 * Repository interface for managing the user's contacts.
 *
 * This interface defines the contract for fetching, adding, and removing contacts.
 * Implementations should handle data synchronization between the local database
 * and any remote backend or P2P network.
 */
interface ContactRepository {
    /**
     * Retrieves the complete list of contacts for the current user.
     *
     * @return A list of [Contact] objects representing the user's contacts.
     */
    suspend fun getContacts(): List<Contact>

    /**
     * Adds a new contact to the user's contact list.
     *
     * @param request The parameters containing the details of the contact to add.
     * @return The newly created [Contact] object.
     */
    suspend fun addContact(request: DomainAddContactParam): Contact

    /**
     * Deletes an existing contact from the user's contact list.
     *
     * @param contactUserId The unique identifier of the user whose contact entry should be removed.
     */
    suspend fun deleteContact(contactUserId: String)
}
