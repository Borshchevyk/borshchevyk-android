package ru.kubsu.borshchevyk.core.network.sync

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import ru.kubsu.borshchevyk.core.model.domain.VectorClock
import ru.kubsu.borshchevyk.core.network.client.NetworkResult
import ru.kubsu.borshchevyk.core.network.dto.SyncEventDto
import ru.kubsu.borshchevyk.core.network.ktor.client.safeRequest
import javax.inject.Inject

class KtorSyncNetworkDataSource @Inject constructor(
    private val httpClient: HttpClient
) : SyncNetworkDataSource {

    override suspend fun pullEvents(clientClock: VectorClock, limit: Int): NetworkResult<ru.kubsu.borshchevyk.core.network.dto.PullSyncEventsResponse> {
        return safeRequest {
            httpClient.post("api/v1/sync/pull") {
                parameter("limit", limit)
                setBody(clientClock)
            }
        }
    }

    override suspend fun pushEvents(events: List<SyncEventDto>): NetworkResult<VectorClock> {
        return safeRequest {
            httpClient.post("api/v1/sync/push") {
                setBody(events)
            }
        }
    }
}
