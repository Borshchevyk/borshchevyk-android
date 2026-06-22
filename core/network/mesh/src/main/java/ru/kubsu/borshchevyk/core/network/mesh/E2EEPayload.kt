package ru.kubsu.borshchevyk.core.network.mesh

import kotlinx.serialization.Serializable

/**
 * Represents an End-to-End Encrypted payload for Mesh messages.
 * 
 * @property encryptedSessionKey The AES-256 session key, encrypted with the recipient's RSA Public Key (Base64 encoded).
 * @property encryptedData The actual payload (e.g. JSON string), encrypted with the AES-256 session key (Base64 encoded).
 */
@Serializable
data class E2EEPayload(
    val encryptedSessionKey: String,
    val encryptedData: String
)
