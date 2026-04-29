package ru.kubsu.borshchevyk.feature.chat.handlers

import ru.kubsu.borshchevyk.core.domain.user.AddContactUseCase
import ru.kubsu.borshchevyk.core.domain.user.GetContactsUseCase
import ru.kubsu.borshchevyk.core.domain.user.RemoveContactUseCase
import javax.inject.Inject

/**
 * Handles contact-related actions.
 */
class ContactHandler @Inject constructor(
    private val getContactsUseCase: GetContactsUseCase,
    private val addContactUseCase: AddContactUseCase,
    private val removeContactUseCase: RemoveContactUseCase
) {
    suspend fun isContact(userId: String): Boolean {
        val contacts = getContactsUseCase()
        return contacts.any { it.contactUserId == userId }
    }
    
    suspend fun addContact(targetUserId: String, firstName: String, lastName: String?) {
        addContactUseCase(targetUserId, firstName, lastName?.ifBlank { null })
    }
    
    suspend fun removeContact(targetUserId: String) {
        removeContactUseCase(targetUserId)
    }
}
