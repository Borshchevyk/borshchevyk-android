package ru.kubsu.borshchevyk.core.network.user

import ru.kubsu.borshchevyk.core.network.client.NetworkMode
import ru.kubsu.borshchevyk.core.network.client.NetworkResult
import ru.kubsu.borshchevyk.core.network.client.TransportModeManager
import ru.kubsu.borshchevyk.core.network.dto.AddContactRequest
import ru.kubsu.borshchevyk.core.network.dto.ContactResponse
import javax.inject.Inject

class ProxyContactNetworkDataSource @Inject constructor(
    private val ktorDataSource: KtorContactNetworkDataSource,
    private val meshDataSource: MeshContactNetworkDataSource,
    private val transportModeManager: TransportModeManager
) : ContactNetworkDataSource {

    private val dataSource: ContactNetworkDataSource
        get() = if (transportModeManager.networkMode.value == NetworkMode.MESH) {
            meshDataSource
        } else {
            ktorDataSource
        }

    override suspend fun getContacts(): NetworkResult<List<ContactResponse>> =
        dataSource.getContacts()

    override suspend fun addContact(request: AddContactRequest): NetworkResult<ContactResponse> =
        dataSource.addContact(request)

    override suspend fun deleteContact(contactUserId: String): NetworkResult<Unit> =
        dataSource.deleteContact(contactUserId)
}
