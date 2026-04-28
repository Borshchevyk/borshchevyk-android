package ru.kubsu.borshchevyk.core.network.client

interface TokenProvider {
    suspend fun getAccessToken(): String?
    fun getAccessTokenSync(): String?
    suspend fun getRefreshToken(): String?
    suspend fun saveTokens(access: String, refresh: String)
    suspend fun clearTokens()
}
