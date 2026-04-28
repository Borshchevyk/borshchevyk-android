package ru.kubsu.borshchevyk.core.network.user

import ru.kubsu.borshchevyk.core.model.domain.NetworkResult
import ru.kubsu.borshchevyk.core.model.dto.AddContactRequest
import ru.kubsu.borshchevyk.core.model.dto.ContactResponse

interface ContactNetworkDataSource {
    suspend fun getContacts(): NetworkResult<List<ContactResponse>>
    suspend fun addContact(request: AddContactRequest): NetworkResult<ContactResponse>
    suspend fun deleteContact(contactUserId: String): NetworkResult<Unit>
}
