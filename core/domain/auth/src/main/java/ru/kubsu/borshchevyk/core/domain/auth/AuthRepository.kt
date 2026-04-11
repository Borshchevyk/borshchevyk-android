package ru.kubsu.borshchevyk.core.domain.auth

import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val accessToken: Flow<String?>
    val userId: Flow<String?>
    val tag: Flow<String?>

    suspend fun registerOffline(tag: String): String
    suspend fun registerOnline(email: String, password: String, tag: String): String
    suspend fun loginOnline(email: String, password: String): String
    suspend fun isLoggedIn(): Boolean
    
    suspend fun logout()
    suspend fun getUserId(): String?
    suspend fun getTag(): String?
}