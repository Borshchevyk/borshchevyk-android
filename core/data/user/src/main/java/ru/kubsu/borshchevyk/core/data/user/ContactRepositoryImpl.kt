package ru.kubsu.borshchevyk.core.data.user

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import ru.kubsu.borshchevyk.core.database.dao.ContactDao
import ru.kubsu.borshchevyk.core.database.entity.ContactEntity
import ru.kubsu.borshchevyk.core.database.entity.ContactWithUser
import ru.kubsu.borshchevyk.core.domain.user.ContactRepository
import ru.kubsu.borshchevyk.core.model.domain.Contact
import ru.kubsu.borshchevyk.core.model.domain.DomainAddContactParam
import ru.kubsu.borshchevyk.core.network.client.getOrThrow
import ru.kubsu.borshchevyk.core.network.di.IoDispatcher
import ru.kubsu.borshchevyk.core.network.dto.AddContactRequest
import ru.kubsu.borshchevyk.core.network.dto.ContactResponse
import ru.kubsu.borshchevyk.core.network.mesh.MeshSignatureService
import ru.kubsu.borshchevyk.core.network.user.ContactNetworkDataSource
import javax.inject.Inject

/**
 * Implementation of [ContactRepository] managing the user's contact list.
 *
 * Handles adding, fetching, and deleting contacts via the network,
 * and maintains a local cache using Room.
 *
 * @property networkDataSource Source for contact-related REST/Mesh API operations.
 * @property contactDao Source for local persistence operations.
 * @property signatureService Service for obtaining current local user ID.
 * @property meshContactListener Background listener for P2P contact events.
 * @property ioDispatcher Coroutine dispatcher for I/O operations.
 */
class ContactRepositoryImpl @Inject constructor(
    private val networkDataSource: ContactNetworkDataSource,
    private val contactDao: ContactDao,
    private val signatureService: MeshSignatureService,
    private val meshContactListener: MeshContactListener,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ContactRepository {

    init {
        meshContactListener.startListening()
    }

    /**
     * Observes the user's contact list in real-time from the local database.
     *
     * @return A [Flow] emitting the current list of [Contact] objects.
     */
    override fun observeContacts(): Flow<List<Contact>> = flow {
        val localUserId = signatureService.getUserId() ?: "self"
        emitAll(
            contactDao.observeContacts(localUserId).map { entities ->
                entities.map { it.toDomain() }
            }
        )
    }

    /**
     * Fetches the current user's contact list from the backend and updates the local cache.
     * Uses an additive sync strategy to avoid UI 'blinking'.
     *
     * @return A list of [Contact] objects.
     */
    override suspend fun getContacts(): List<Contact> = withContext(ioDispatcher) {
        val localUserId = signatureService.getUserId() ?: "self"
        
        try {
            val remoteContacts = networkDataSource.getContacts().getOrThrow()
            
            // Additive Sync: only update or insert, don't clear everything to avoid UI flicker.
            val entities = remoteContacts.map { it.toEntity() }
            if (entities.isNotEmpty()) {
                contactDao.upsertContacts(entities)
            }
            
            remoteContacts.map { it.toDomain() }
        } catch (e: Exception) {
            // Fallback to local DB if network is unavailable
            contactDao.getContacts(localUserId).map { it.toDomain() }
        }
    }

    /**
     * Adds a new contact by target user ID.
     *
     * @param request The parameters including the target user's ID, first name, and last name.
     * @return The newly added [Contact].
     */
    override suspend fun addContact(request: DomainAddContactParam): Contact = withContext(ioDispatcher) {
        val response = networkDataSource.addContact(
            AddContactRequest(
                targetUserId = request.targetUserId,
                firstName = request.firstName,
                lastName = request.lastName
            )
        ).getOrThrow()
        
        // Save to local cache
        contactDao.upsertContact(response.toEntity())
        
        response.toDomain()
    }

    /**
     * Deletes a contact by their user ID.
     *
     * @param contactUserId The user ID of the contact to remove.
     */
    override suspend fun deleteContact(contactUserId: String) = withContext(ioDispatcher) {
        val localUserId = signatureService.getUserId() ?: "self"
        
        networkDataSource.deleteContact(contactUserId).getOrThrow()
        
        // Remove from local cache
        contactDao.deleteContact(localUserId, contactUserId)
    }

    /**
     * Extension to map [ContactResponse] DTO to its domain equivalent [Contact].
     */
    private fun ContactResponse.toDomain(): Contact = Contact(
        id = id,
        ownerId = ownerId,
        contactUserId = contactUserId,
        contactFirstName = contactFirstName,
        contactLastName = contactLastName,
        addedAt = addedAt ?: "",
        contactAvatarUrl = null
    )
    
    /**
     * Extension to map [ContactResponse] DTO to its database entity [ContactEntity].
     */
    private fun ContactResponse.toEntity(): ContactEntity = ContactEntity(
        id = id,
        ownerId = ownerId,
        contactUserId = contactUserId,
        contactFirstName = contactFirstName ?: "Unknown",
        contactLastName = contactLastName,
        addedAt = addedAt ?: ""
    )
    
    /**
     * Extension to map [ContactEntity] to its domain equivalent [Contact].
     */
    private fun ContactEntity.toDomain(): Contact = Contact(
        id = id,
        ownerId = ownerId,
        contactUserId = contactUserId,
        contactFirstName = contactFirstName,
        contactLastName = contactLastName,
        addedAt = addedAt,
        contactAvatarUrl = null
    )

    /**
     * Extension to map [ContactWithUser] to its domain equivalent [Contact].
     * This prioritizes the custom contact name over the user's real name.
     */
    private fun ContactWithUser.toDomain(): Contact = Contact(
        id = contact.id,
        ownerId = contact.ownerId,
        contactUserId = contact.contactUserId,
        contactFirstName = contact.contactFirstName,
        contactLastName = contact.contactLastName,
        addedAt = contact.addedAt,
        contactAvatarUrl = user?.avatarUrl
    )
}
