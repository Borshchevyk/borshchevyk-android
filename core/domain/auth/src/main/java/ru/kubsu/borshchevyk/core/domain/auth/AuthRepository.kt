package ru.kubsu.borshchevyk.core.domain.auth

import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing authentication and user identity.
 * 
 * This repository handles both offline (P2P Mesh) and online (Global Server) authentication
 * flows, managing the current user's session, tokens, and basic identity information.
 */
interface AuthRepository {
    /**
     * A flow emitting the current user's access token.
     * Emits `null` if the user is not authenticated online.
     */
    val accessToken: Flow<String?>

    /**
     * A flow emitting the current user's unique identifier (ID).
     * Emits `null` if no user identity is established.
     */
    val userId: Flow<String?>

    /**
     * A flow emitting the current user's unique tag (username).
     * Emits `null` if no user identity is established.
     */
    val tag: Flow<String?>

    /**
     * Registers a new user for offline (P2P Mesh) mode.
     * 
     * Creates a local identity that can be used to communicate on the mesh network
     * without requiring a centralized server.
     *
     * @param tag The unique username or tag for the user.
     * @param firstName The user's first name.
     * @param lastName The user's optional last name.
     * @return The unique user ID generated for this local identity.
     */
    suspend fun registerOffline(tag: String, firstName: String, lastName: String?): String

    /**
     * Registers a new user on the global centralized server.
     *
     * @param email The user's email address.
     * @param password The user's password.
     * @param tag The unique username or tag for the user.
     * @param firstName The user's first name.
     * @param lastName The user's last name, optional.
     * @return The unique user ID returned by the server upon successful registration.
     */
    suspend fun registerOnline(email: String, password: String, tag: String, firstName: String, lastName: String?): String

    /**
     * Authenticates an existing user on the global centralized server.
     *
     * @param email The user's email address.
     * @param password The user's password.
     * @return The unique user ID upon successful login.
     */
    suspend fun loginOnline(email: String, password: String): String

    /**
     * Checks whether there is currently an active user identity or session.
     *
     * @return `true` if a user is logged in (either online or offline), `false` otherwise.
     */
    suspend fun isLoggedIn(): Boolean
    
    /**
     * Logs out the current user, clearing their session, tokens, and active identity.
     */
    suspend fun logout()

    /**
     * Retrieves the current user's ID directly, bypassing the flow.
     *
     * @return The current user's ID, or `null` if not authenticated.
     */
    suspend fun getUserId(): String?

    /**
     * Retrieves the current user's tag (username) directly, bypassing the flow.
     *
     * @return The current user's tag, or `null` if not authenticated.
     */
    suspend fun getTag(): String?
}