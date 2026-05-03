package ru.kubsu.borshchevyk.core.network.mesh

import android.content.Context
import android.util.Log
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Strategy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeshConnectionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val payloadRouter: MeshPayloadRouter
) {
    private val connectionsClient: ConnectionsClient = Nearby.getConnectionsClient(context)
    
    private val _connectedEndpoints = MutableStateFlow<Set<String>>(emptySet())
    val connectedEndpoints: StateFlow<Set<String>> = _connectedEndpoints.asStateFlow()

    private val strategy = Strategy.P2P_CLUSTER
    private val serviceId = "ru.kubsu.borshchevyk.mesh"
    private val TAG = "MeshConnectionManager"

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            Log.d(TAG, "Connection initiated with endpoint: $endpointId")
            // Automatically accept connections for the cluster
            connectionsClient.acceptConnection(endpointId, payloadRouter.payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    Log.d(TAG, "Successfully connected to endpoint: $endpointId")
                    _connectedEndpoints.value = _connectedEndpoints.value + endpointId
                }
                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                    Log.w(TAG, "Connection rejected by endpoint: $endpointId")
                }
                ConnectionsStatusCodes.STATUS_ERROR -> {
                    Log.e(TAG, "Error connecting to endpoint: $endpointId")
                }
                else -> {
                    Log.w(TAG, "Unknown connection status ${result.status.statusCode} for endpoint: $endpointId")
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            Log.d(TAG, "Disconnected from endpoint: $endpointId")
            _connectedEndpoints.value = _connectedEndpoints.value - endpointId
        }
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            Log.d(TAG, "Endpoint found: $endpointId (${info.endpointName})")
            // Request connection when a new endpoint is found
            connectionsClient.requestConnection(
                context.packageName, // local endpoint name
                endpointId,
                connectionLifecycleCallback
            ).addOnFailureListener { e ->
                Log.e(TAG, "Failed to request connection to $endpointId", e)
            }
        }

        override fun onEndpointLost(endpointId: String) {
            Log.d(TAG, "Endpoint lost: $endpointId")
        }
    }

    fun startAdvertising(localEndpointName: String) {
        val options = AdvertisingOptions.Builder().setStrategy(strategy).build()
        connectionsClient.startAdvertising(
            localEndpointName,
            serviceId,
            connectionLifecycleCallback,
            options
        ).addOnSuccessListener {
            Log.d(TAG, "Started advertising as $localEndpointName")
        }.addOnFailureListener { e ->
            Log.e(TAG, "Failed to start advertising", e)
        }
    }

    fun stopAdvertising() {
        connectionsClient.stopAdvertising()
        Log.d(TAG, "Stopped advertising")
    }

    fun startDiscovery() {
        val options = DiscoveryOptions.Builder().setStrategy(strategy).build()
        connectionsClient.startDiscovery(
            serviceId,
            endpointDiscoveryCallback,
            options
        ).addOnSuccessListener {
            Log.d(TAG, "Started discovery")
        }.addOnFailureListener { e ->
            Log.e(TAG, "Failed to start discovery", e)
        }
    }

    fun stopDiscovery() {
        connectionsClient.stopDiscovery()
        Log.d(TAG, "Stopped discovery")
    }
    
    fun disconnect(endpointId: String) {
        connectionsClient.disconnectFromEndpoint(endpointId)
        _connectedEndpoints.value = _connectedEndpoints.value - endpointId
        Log.d(TAG, "Disconnected manually from endpoint: $endpointId")
    }
    
    fun stopAllEndpoints() {
        connectionsClient.stopAllEndpoints()
        _connectedEndpoints.value = emptySet()
        Log.d(TAG, "Stopped all endpoints")
    }
}
