package ru.kubsu.borshchevyk.core.network.sync

import ru.kubsu.borshchevyk.core.model.domain.VectorClock
import ru.kubsu.borshchevyk.core.network.client.NetworkResult
import ru.kubsu.borshchevyk.core.network.dto.SyncEventDto

/**
 * Interface representing network calls to the sync microservice.
 */
interface SyncNetworkDataSource {
    suspend fun pullEvents(clientClock: VectorClock, limit: Int = 100): NetworkResult<ru.kubsu.borshchevyk.core.network.dto.PullSyncEventsResponse>
    suspend fun pushEvents(events: List<SyncEventDto>): NetworkResult<VectorClock>
}
