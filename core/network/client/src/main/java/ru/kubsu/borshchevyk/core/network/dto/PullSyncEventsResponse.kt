package ru.kubsu.borshchevyk.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class PullSyncEventsResponse(
    val events: List<SyncEventDto>,
    val hasMore: Boolean
)
