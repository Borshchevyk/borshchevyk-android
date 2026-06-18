package ru.kubsu.borshchevyk.core.data.user

import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import ru.kubsu.borshchevyk.core.database.dao.ContactDao
import ru.kubsu.borshchevyk.core.database.dao.UserDao
import ru.kubsu.borshchevyk.core.database.entity.ContactEntity
import ru.kubsu.borshchevyk.core.network.di.ApplicationScope
import ru.kubsu.borshchevyk.core.network.di.IoDispatcher
import ru.kubsu.borshchevyk.core.network.dto.ContactResponse
import ru.kubsu.borshchevyk.core.network.mesh.MeshFloodingProtocol
import ru.kubsu.borshchevyk.core.network.mesh.MeshSignatureService
import ru.kubsu.borshchevyk.core.network.user.MeshContactActions
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeshContactListener @Inject constructor(
    private val gossipProtocol: MeshFloodingProtocol,
    private val contactDao: ContactDao,
    private val userDao: UserDao,
    private val json: Json,
    private val signatureService: MeshSignatureService,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @ApplicationScope private val applicationScope: CoroutineScope
) {
    fun startListening() {
        applicationScope.launch(ioDispatcher) {
            gossipProtocol.incomingEnvelopes.collect { envelope ->
                val localUserId = signatureService.getUserId() ?: return@collect
                
                // Security: Verify signature before processing any contact actions
                val dataToVerify = envelope.payload.toByteArray(Charsets.UTF_8)
                val isValid = signatureService.verifySignature(envelope.originEndpointId, envelope.signature ?: return@collect, dataToVerify)
                if (!isValid) {
                    Log.e("MeshContactListener", "SECURITY ALERT: Invalid signature for contact action ${envelope.action} from ${envelope.originEndpointId}")
                    return@collect
                }

                when (envelope.action) {
                    MeshContactActions.CONTACT_ADD -> {
                        try {
                            val contactResponse = json.decodeFromString<ContactResponse>(envelope.payload)
                            if (contactResponse.contactUserId == localUserId) {
                                val senderId = contactResponse.ownerId
                                Log.d("MeshContactListener", "Incoming CONTACT_ADD from $senderId.")
                                
                                val senderProfile = userDao.getUser(senderId)
                                
                                val newContact = ContactEntity(
                                    id = contactResponse.id,
                                    ownerId = localUserId,
                                    contactUserId = senderId,
                                    contactFirstName = senderProfile?.firstName ?: senderProfile?.tag ?: "Unknown",
                                    contactLastName = senderProfile?.lastName,
                                    addedAt = contactResponse.addedAt ?: ""
                                )
                                contactDao.upsertContact(newContact)

                                // Profile Watcher: If the name is unknown, wait for profile discovery
                                if (newContact.contactFirstName == "Unknown") {
                                    watchProfileAndHealContact(senderId, localUserId)
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("MeshContactListener", "Failed to parse CONTACT_ADD payload", e)
                        }
                    }
                    MeshContactActions.CONTACT_DELETE -> {
                        try {
                            val deletedContactId = envelope.payload.trim().removeSurrounding("\"")
                            Log.d("MeshContactListener", "Incoming CONTACT_DELETE. Payload: '$deletedContactId', origin: '${envelope.originEndpointId}', localUserId: '$localUserId'")
                            if (deletedContactId == localUserId) {
                                Log.d("MeshContactListener", "Processing CONTACT_DELETE from ${envelope.originEndpointId}. Deleting local contact.")
                                contactDao.deleteContact(localUserId, envelope.originEndpointId)
                            } else {
                                Log.w("MeshContactListener", "Ignoring CONTACT_DELETE because payload '$deletedContactId' != localUserId '$localUserId'")
                            }
                        } catch (e: Exception) {
                            Log.e("MeshContactListener", "Failed to process CONTACT_DELETE", e)
                        }
                    }
                }
            }
        }
    }

    private fun watchProfileAndHealContact(userId: String, ownerId: String) {
        applicationScope.launch(ioDispatcher) {
            Log.d("MeshContactListener", "Starting profile watcher for unknown contact: $userId")
            userDao.observeUser(userId)
                .takeWhile { user -> 
                    val isHealed = user != null && (user.firstName != null || user.tag.isNotEmpty())
                    if (isHealed) {
                        Log.d("MeshContactListener", "Healing contact $userId with discovered profile data.")
                        val existing = contactDao.getContact(ownerId, userId)
                        if (existing != null) {
                            contactDao.upsertContact(existing.copy(
                                contactFirstName = user?.firstName ?: user?.tag ?: "Unknown",
                                contactLastName = user?.lastName
                            ))
                        }
                    }
                    !isHealed // Continue while NOT healed
                }
                .collect { } 
        }
    }
}
