package ru.kubsu.borshchevyk.core.network.dto

import kotlinx.serialization.Serializable

/**
 * Represents a generic envelope for sending data over a mesh network.
 * 
 * @property action The operation to perform (e.g., "SEND_MESSAGE", "EDIT_MESSAGE").
 * @property payload The JSON string representation of the request data.
 */
@Serializable
data class MeshEnvelope(
    val action: String,
    val payload: String
)
