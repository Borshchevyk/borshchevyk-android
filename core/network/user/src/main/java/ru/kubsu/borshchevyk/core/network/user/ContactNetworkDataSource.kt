package ru.kubsu.borshchevyk.core.network.user

import ru.kubsu.borshchevyk.core.network.client.NetworkResult
import ru.kubsu.borshchevyk.core.network.dto.AddContactRequest
import ru.kubsu.borshchevyk.core.network.dto.ContactResponse

/**
 * Data source interface defining network operations related to managing contacts.
 */
interface ContactNetworkDataSource {
    /**
     * Retrieves the list of contacts for the current user.
     *
     * @return A [NetworkResult] containing a list of [ContactResponse].
     */
    suspend fun getContacts(): NetworkResult<List<ContactResponse>>

    /**
     * Adds a new user to the current user's contact list.
     *
     * @param request The [AddContactRequest] detailing the target user and assigned names.
     * @return A [NetworkResult] containing the newly created [ContactResponse].
     */
    suspend fun addContact(request: AddContactRequest): NetworkResult<ContactResponse>

    /**
     * Deletes a contact from the current user's contact list.
     *
     * @param contactUserId The ID of the target user to remove from contacts.
     * @return A [NetworkResult] indicating success or failure.
     */
    suspend fun deleteContact(contactUserId: String): NetworkResult<Unit>
}
