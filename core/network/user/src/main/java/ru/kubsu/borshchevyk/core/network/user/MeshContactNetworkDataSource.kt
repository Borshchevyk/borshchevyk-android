package ru.kubsu.borshchevyk.core.network.user

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.kubsu.borshchevyk.core.database.dao.ContactDao
import ru.kubsu.borshchevyk.core.database.dao.UserDao
import ru.kubsu.borshchevyk.core.database.entity.ContactEntity
import ru.kubsu.borshchevyk.core.network.client.NetworkResult
import ru.kubsu.borshchevyk.core.network.di.IoDispatcher
import ru.kubsu.borshchevyk.core.network.dto.AddContactRequest
import ru.kubsu.borshchevyk.core.network.dto.ContactResponse
import ru.kubsu.borshchevyk.core.network.mesh.MeshEnvelope
import ru.kubsu.borshchevyk.core.network.mesh.MeshFloodingProtocol
import ru.kubsu.borshchevyk.core.network.mesh.MeshSignatureService
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class MeshContactNetworkDataSource @Inject constructor(
    private val contactDao: ContactDao,
    private val userDao: UserDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val gossipProtocol: MeshFloodingProtocol,
    private val signatureService: MeshSignatureService,
    private val json: Json
) : ContactNetworkDataSource {

    override suspend fun getContacts(): NetworkResult<List<ContactResponse>> {
        return withContext(ioDispatcher) {
            val localUserId = signatureService.getUserId() ?: "self"
            val contacts = contactDao.getContacts(localUserId).map {
                ContactResponse(
                    id = it.id,
                    ownerId = it.ownerId,
                    contactUserId = it.contactUserId,
                    contactFirstName = it.contactFirstName,
                    contactLastName = it.contactLastName,
                    addedAt = it.addedAt
                )
            }
            NetworkResult.Success(contacts)
        }
    }

    override suspend fun addContact(request: AddContactRequest): NetworkResult<ContactResponse> {
        return withContext(ioDispatcher) {
            val localUserId = signatureService.getUserId() ?: "self"
            val addedAtIso = Instant.now().toString()

            val contactEntity = ContactEntity(
                id = UUID.randomUUID().toString(),
                ownerId = localUserId,
                contactUserId = request.targetUserId,
                contactFirstName = request.firstName,
                contactLastName = request.lastName,
                addedAt = addedAtIso
            )

            // Save locally
            contactDao.upsertContact(contactEntity)

            val contactResponse = ContactResponse(
                id = contactEntity.id,
                ownerId = contactEntity.ownerId,
                contactUserId = contactEntity.contactUserId,
                contactFirstName = contactEntity.contactFirstName,
                contactLastName = contactEntity.contactLastName,
                addedAt = contactEntity.addedAt
            )

            // In Mesh mode, we send a direct payload so the other side knows we added them.
            val envelope = MeshEnvelope(
                envelopeId = UUID.randomUUID().toString(),
                originEndpointId = "", // Filled by protocol
                action = MeshContactActions.CONTACT_ADD,
                payload = json.encodeToString(contactResponse)
            )
            gossipProtocol.broadcast(envelope)

            NetworkResult.Success(contactResponse)
        }
    }

    override suspend fun deleteContact(contactUserId: String): NetworkResult<Unit> {
        return withContext(ioDispatcher) {
            val localUserId = signatureService.getUserId() ?: "self"
            
            // Delete locally
            contactDao.deleteContact(ownerId = localUserId, contactUserId = contactUserId)

            val envelope = MeshEnvelope(
                envelopeId = UUID.randomUUID().toString(),
                originEndpointId = "", // Filled by protocol
                action = MeshContactActions.CONTACT_DELETE,
                payload = contactUserId // Just sending the ID of the person we removed
            )
            gossipProtocol.broadcast(envelope)

            NetworkResult.Success(Unit)
        }
    }
}

object MeshContactActions {
    const val CONTACT_ADD = "CONTACT_ADD"
    const val CONTACT_DELETE = "CONTACT_DELETE"
}
