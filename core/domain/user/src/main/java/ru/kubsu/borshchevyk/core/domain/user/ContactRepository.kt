package ru.kubsu.borshchevyk.core.domain.user

import ru.kubsu.borshchevyk.core.model.domain.Contact
import ru.kubsu.borshchevyk.core.model.dto.AddContactRequest

interface ContactRepository {
    suspend fun getContacts(): List<Contact>
    suspend fun addContact(request: AddContactRequest): Contact
    suspend fun deleteContact(contactUserId: String)
}
