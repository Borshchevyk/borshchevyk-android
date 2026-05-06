package ru.kubsu.borshchevyk.core.network.dto

import kotlinx.serialization.Serializable

/**
 * Control message used in the Mesh network to request the raw bytes of a file.
 * Sent by a node that previously received a FILE_HEADER but didn't download the file.
 */
@Serializable
data class FilePullRequestDto(
    val attachmentId: String,
    val requesterEndpointId: String
)
