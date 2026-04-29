package ru.kubsu.borshchevyk.core.network.user

import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import ru.kubsu.borshchevyk.core.network.client.NetworkResult
import ru.kubsu.borshchevyk.core.network.dto.AddContactRequest
import ru.kubsu.borshchevyk.core.network.dto.ContactResponse
import ru.kubsu.borshchevyk.core.network.client.safeRequest
import ru.kubsu.borshchevyk.core.network.di.IoDispatcher
import javax.inject.Inject

class KtorContactNetworkDataSource @Inject constructor(
    private val httpClient: HttpClient,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ContactNetworkDataSource {

    override suspend fun getContacts(): NetworkResult<List<ContactResponse>> {
        return withContext(ioDispatcher) {
            safeRequest {
                httpClient.get("api/v1/contacts")
            }
        }
    }

    override suspend fun addContact(request: AddContactRequest): NetworkResult<ContactResponse> {
        return withContext(ioDispatcher) {
            safeRequest {
                httpClient.post("api/v1/contacts") {
                    setBody(request)
                }
            }
        }
    }

    override suspend fun deleteContact(contactUserId: String): NetworkResult<Unit> {
        return withContext(ioDispatcher) {
            safeRequest {
                httpClient.delete("api/v1/contacts/$contactUserId")
            }
        }
    }
}
