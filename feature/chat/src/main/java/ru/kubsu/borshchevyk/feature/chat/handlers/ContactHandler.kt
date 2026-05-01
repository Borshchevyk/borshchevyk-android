package ru.kubsu.borshchevyk.feature.chat.handlers

import ru.kubsu.borshchevyk.core.domain.user.AddContactUseCase
import ru.kubsu.borshchevyk.core.domain.user.GetContactsUseCase
import ru.kubsu.borshchevyk.core.domain.user.RemoveContactUseCase
import javax.inject.Inject

/**
 * Handles operations related to managing the user's contacts.
 * Provides functionality to check contact status, add new contacts, and remove existing ones.
 *
 * @property getContactsUseCase Use case for retrieving the current user's contact list.
 * @property addContactUseCase Use case for adding a new user to contacts.
 * @property removeContactUseCase Use case for removing an existing user from contacts.
 */
class ContactHandler @Inject constructor(
    private val getContactsUseCase: GetContactsUseCase,
    private val addContactUseCase: AddContactUseCase,
    private val removeContactUseCase: RemoveContactUseCase
) {
    /**
     * Checks if a specific user is currently in the contact list.
     *
     * @param userId The unique identifier of the user to check.
     * @return `true` if the user is a contact, `false` otherwise.
     */
    suspend fun isContact(userId: String): Boolean {
        val contacts = getContactsUseCase()
        return contacts.any { it.contactUserId == userId }
    }
    
    /**
     * Adds a new user to the contact list with the specified name details.
     *
     * @param targetUserId The unique identifier of the user to add as a contact.
     * @param firstName The contact's first name.
     * @param lastName The contact's last name, optional.
     */
    suspend fun addContact(targetUserId: String, firstName: String, lastName: String?) {
        addContactUseCase(targetUserId, firstName, lastName?.ifBlank { null })
    }
    
    /**
     * Removes an existing user from the contact list.
     *
     * @param targetUserId The unique identifier of the user to remove from contacts.
     */
    suspend fun removeContact(targetUserId: String) {
        removeContactUseCase(targetUserId)
    }
}
