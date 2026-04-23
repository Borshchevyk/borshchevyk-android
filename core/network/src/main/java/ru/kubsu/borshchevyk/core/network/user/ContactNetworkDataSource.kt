package ru.kubsu.borshchevyk.core.network.user

import ru.kubsu.borshchevyk.core.model.dto.AddContactRequest
import ru.kubsu.borshchevyk.core.model.dto.ContactResponse

interface ContactNetworkDataSource {
    suspend fun getContacts(): List<ContactResponse>
    suspend fun addContact(request: AddContactRequest): ContactResponse
    suspend fun deleteContact(contactUserId: String)
}
