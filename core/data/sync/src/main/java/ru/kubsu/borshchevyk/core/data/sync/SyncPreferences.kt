package ru.kubsu.borshchevyk.core.data.sync

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.kubsu.borshchevyk.core.model.domain.VectorClock
import javax.inject.Inject
import javax.inject.Singleton

val Context.syncDataStore: DataStore<Preferences> by preferencesDataStore(name = "sync_prefs")

@Singleton
class SyncPreferences @Inject constructor(@ApplicationContext private val context: Context) {
    private val VECTOR_CLOCK = stringPreferencesKey("vector_clock")
    private val NODE_ID = stringPreferencesKey("node_id")
    
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getNodeId(): String {
        var id = context.syncDataStore.data.map { it[NODE_ID] }.first()
        if (id == null) {
            id = java.util.UUID.randomUUID().toString()
            context.syncDataStore.edit { prefs ->
                prefs[NODE_ID] = id!!
            }
        }
        return id
    }

    suspend fun getVectorClock(): VectorClock {
        val clockStr = context.syncDataStore.data.map { it[VECTOR_CLOCK] }.first()
        return if (clockStr != null) {
            json.decodeFromString(clockStr)
        } else {
            VectorClock()
        }
    }

    suspend fun saveVectorClock(clock: VectorClock) {
        val clockStr = json.encodeToString(clock)
        context.syncDataStore.edit { prefs ->
            prefs[VECTOR_CLOCK] = clockStr
        }
    }
}
