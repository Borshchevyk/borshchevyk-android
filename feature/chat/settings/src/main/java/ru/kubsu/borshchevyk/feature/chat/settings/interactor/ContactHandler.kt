package ru.kubsu.borshchevyk.feature.chat.settings.interactor

import ru.kubsu.borshchevyk.core.domain.user.AddContactUseCase
import ru.kubsu.borshchevyk.core.domain.user.GetContactsUseCase
import ru.kubsu.borshchevyk.core.domain.user.RemoveContactUseCase
import javax.inject.Inject

class ContactHandler @Inject constructor(
    private val getContactsUseCase: GetContactsUseCase,
    private val addContactUseCase: AddContactUseCase,
    private val removeContactUseCase: RemoveContactUseCase
) {
    suspend fun isContact(userId: String): Boolean {
        val contacts = getContactsUseCase()
        return contacts.any { it.contactUserId == userId }
    }
    
    suspend fun addContact(partnerId: String, firstName: String, lastName: String?) {
        addContactUseCase(partnerId, firstName, lastName?.ifBlank { null })
    }
    
    suspend fun removeContact(partnerId: String) {
        removeContactUseCase(partnerId)
    }
}
