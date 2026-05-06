package ru.kubsu.borshchevyk.core.network.dto

import kotlinx.serialization.Serializable

/**
 * Control message used in the Mesh network to map a dynamically generated
 * Nearby Connections payloadId to a domain attachmentId.
 * This is a 1-hop message that precedes the actual Payload.Type.FILE.
 */
@Serializable
data class FileHeaderDto(
    val payloadId: Long,
    val attachmentId: String,
    val filename: String,
    val mimeType: String
)
