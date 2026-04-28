package ru.kubsu.borshchevyk.core.data.user

import ru.kubsu.borshchevyk.core.domain.user.ContactRepository
import ru.kubsu.borshchevyk.core.model.domain.Contact
import ru.kubsu.borshchevyk.core.model.domain.DomainAddContactParam
import ru.kubsu.borshchevyk.core.model.dto.AddContactRequest
import ru.kubsu.borshchevyk.core.model.dto.ContactResponse
import ru.kubsu.borshchevyk.core.network.user.ContactNetworkDataSource
import javax.inject.Inject

class ContactRepositoryImpl @Inject constructor(
    private val networkDataSource: ContactNetworkDataSource
) : ContactRepository {

    override suspend fun getContacts(): List<Contact> {
        return networkDataSource.getContacts().map { it.toDomain() }
    }

    override suspend fun addContact(request: DomainAddContactParam): Contact {
        return networkDataSource.addContact(
            AddContactRequest(
                targetUserId = request.targetUserId,
                firstName = request.firstName,
                lastName = request.lastName
            )
        ).toDomain()
    }

    override suspend fun deleteContact(contactUserId: String) {
        networkDataSource.deleteContact(contactUserId)
    }

    private fun ContactResponse.toDomain(): Contact = Contact(
        id = id,
        ownerId = ownerId,
        contactUserId = contactUserId,
        contactFirstName = contactFirstName,
        contactLastName = contactLastName,
        addedAt = addedAt
    )
}
