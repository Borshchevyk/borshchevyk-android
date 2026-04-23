package ru.kubsu.borshchevyk.core.data.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import ru.kubsu.borshchevyk.core.network.auth.TokenProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Extension property to lazily instantiate Jetpack DataStore.
 */
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

/**
 * Provides access to locally persisted authentication and identity data.
 *
 * Utilizes Jetpack DataStore to store non-cryptographic secrets and the encrypted
 * (AES-wrapped) form of the user's RSA private key.
 *
 * @property context the application context required for DataStore initialization
 */
@Singleton
class AuthPreferences @Inject constructor(@ApplicationContext private val context: Context) : TokenProvider {
    private val ACCESS_TOKEN = stringPreferencesKey("access_token")
    private val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
    private val USER_ID = stringPreferencesKey("user_id")
    private val TAG = stringPreferencesKey("tag")
    private val LOCAL_WRAPPED_PRIVATE_KEY = stringPreferencesKey("local_wrapped_private_key")

    /** Flow emitting the current JWT access token, or null if unauthenticated. */
    val accessToken: Flow<String?> = context.dataStore.data.map { it[ACCESS_TOKEN] }
    
    /** Flow emitting the currently logged-in user UUID. */
    val userId: Flow<String?> = context.dataStore.data.map { it[USER_ID] }
    
    /** Flow emitting the current user's tag/username. */
    val tag: Flow<String?> = context.dataStore.data.map { it[TAG] }
    
    /** Flow emitting the Base64-encoded, AES-wrapped RSA private key. */
    val localWrappedPrivateKey: Flow<String?> = context.dataStore.data.map { it[LOCAL_WRAPPED_PRIVATE_KEY] }

    override suspend fun getAccessToken(): String? {
        return context.dataStore.data.map { it[ACCESS_TOKEN] }.first()
    }

    override suspend fun getRefreshToken(): String? {
        return context.dataStore.data.map { it[REFRESH_TOKEN] }.first()
    }

    override suspend fun clearTokens() {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
    }

    suspend fun clearIdentity() {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
    }

    /**
     * Persists the JWT session tokens provided by the backend.
     *
     * @param access the access token string
     * @param refresh the refresh token string
     */
    override suspend fun saveTokens(access: String, refresh: String) {
        context.dataStore.edit { prefs ->
            prefs[ACCESS_TOKEN] = access
            prefs[REFRESH_TOKEN] = refresh
        }
    }

    /**
     * Persists the authenticated user's ID.
     *
     * @param id the user's UUID string
     */
    suspend fun saveUserId(id: String) {
        context.dataStore.edit { prefs ->
            prefs[USER_ID] = id
        }
    }

    /**
     * Persists the authenticated user's tag.
     *
     * @param tagStr the user's tag/display name
     */
    suspend fun saveTag(tagStr: String) {
        context.dataStore.edit { prefs ->
            prefs[TAG] = tagStr
        }
    }

    /**
     * Persists the user's wrapped private key safely.
     *
     * @param key the Base64-encoded byte array representing the wrapped private key
     */
    suspend fun saveLocalWrappedPrivateKey(key: String) {
        context.dataStore.edit { prefs ->
            prefs[LOCAL_WRAPPED_PRIVATE_KEY] = key
        }
    }
}
