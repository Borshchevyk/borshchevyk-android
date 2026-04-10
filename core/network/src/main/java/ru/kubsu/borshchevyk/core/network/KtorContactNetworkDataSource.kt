package ru.kubsu.borshchevyk.core.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import ru.kubsu.borshchevyk.core.model.dto.AddContactRequest
import ru.kubsu.borshchevyk.core.model.dto.ContactResponse
import javax.inject.Inject

class KtorContactNetworkDataSource @Inject constructor(
    private val httpClient: HttpClient
) : ContactNetworkDataSource {

    override suspend fun getContacts(): List<ContactResponse> {
        return httpClient.get("api/v1/contacts").body()
    }

    override suspend fun addContact(request: AddContactRequest): ContactResponse {
        return httpClient.post("api/v1/contacts") {
            setBody(request)
        }.body()
    }

    override suspend fun deleteContact(contactUserId: String) {
        httpClient.delete("api/v1/contacts/$contactUserId")
    }
}
