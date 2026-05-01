package ru.kubsu.borshchevyk.core.network.client

/**
 * Interface for providing and managing authentication tokens.
 * Decouples the network layer from the specific storage mechanism (e.g., DataStore, EncryptedSharedPreferences).
 */
interface TokenProvider {
    /**
     * Retrieves the current access token asynchronously.
     * @return The access token, or null if not available.
     */
    suspend fun getAccessToken(): String?

    /**
     * Retrieves the current access token synchronously.
     * Use with caution, primarily for interceptors that cannot suspend.
     * @return The access token, or null if not available.
     */
    fun getAccessTokenSync(): String?

    /**
     * Retrieves the current refresh token asynchronously.
     * @return The refresh token, or null if not available.
     */
    suspend fun getRefreshToken(): String?

    /**
     * Saves new access and refresh tokens after successful login or refresh.
     * @param access The new access token.
     * @param refresh The new refresh token.
     */
    suspend fun saveTokens(access: String, refresh: String)

    /**
     * Clears all stored tokens, typically invoked upon logout or token expiration.
     */
    suspend fun clearTokens()
}
