package ru.kubsu.borshchevyk.core.domain.auth

/**
 * Repository interface defining the authentication and registration flows.
 *
 * This component acts as the Single Source of Truth for identity management,
 * handling both the Offline (Mesh) and Online (Server) scenarios.
 */
interface AuthRepository {
    /**
     * Registers an identity locally for use within a Mesh (P2P) network.
     *
     * Generates a local hardware-backed RSA key pair and stores the tag.
     * No server communication is involved.
     *
     * @param tag the unique display name or identifier for the user
     * @return the local internal user identifier generated
     */
    suspend fun registerOffline(tag: String): String

    /**
     * Registers a new identity with the global backend server.
     *
     * Generates an in-memory RSA key pair, encrypts the private key using the
     * provided password, and sends it along with the public key to the server.
     * Locally, the raw private key is securely wrapped using the Android Keystore
     * and persisted.
     *
     * @param email the user's email address
     * @param password the user's plaintext password
     * @param tag the user's display name or identifier
     * @return the server-assigned user UUID
     */
    suspend fun registerOnline(email: String, password: String, tag: String): String

    /**
     * Authenticates an existing user with the global backend server.
     *
     * This orchestrates a two-phase login process:
     * 1. Fetches the encrypted private key from the server using credentials.
     * 2. Decrypts the key, signs a server challenge, and exchanges it for JWT tokens.
     * The raw private key is wrapped and stored securely on the device.
     *
     * @param email the user's email address
     * @param password the user's plaintext password
     * @return the server-assigned user UUID
     * @throws Exception if network fails, decryption fails, or credentials are invalid
     */
    suspend fun loginOnline(email: String, password: String): String

    /**
     * Checks whether the user currently has an active session or a configured local identity.
     *
     * @return `true` if JWT tokens or an offline tag are present, `false` otherwise
     */
    suspend fun isLoggedIn(): Boolean
}
