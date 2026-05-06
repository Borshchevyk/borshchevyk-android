package ru.kubsu.borshchevyk.core.data.auth

import android.util.Base64
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import ru.kubsu.borshchevyk.core.domain.auth.AuthRepository
import ru.kubsu.borshchevyk.core.network.auth.AuthNetworkDataSource
import ru.kubsu.borshchevyk.core.network.client.getOrThrow
import ru.kubsu.borshchevyk.core.network.dto.ChallengeRequest
import ru.kubsu.borshchevyk.core.network.dto.LoginRequest
import ru.kubsu.borshchevyk.core.network.dto.RegisterRequest
import ru.kubsu.borshchevyk.core.network.dto.VerifyRequest
import ru.kubsu.borshchevyk.core.network.websocket.WebSocketConnectionManager
import ru.kubsu.borshchevyk.core.security.KeyManager
import java.security.MessageDigest
import javax.inject.Inject

/**
 * Implementation of [AuthRepository] managing user authentication, registration, and session state.
 *
 * Handles both online (global server) and offline (mesh network) authentication flows. It incorporates
 * cryptographic key generation and signing for secure challenge-response authentication.
 *
 * @property networkDataSource Source for authentication REST API operations.
 * @property keyManager Utility for generating, wrapping, and securely managing RSA keys.
 * @property authPreferences Local storage for tokens and user identity.
 * @property webSocketConnectionManager Manager for maintaining the global WebSocket connection.
 */
class AuthRepositoryImpl @Inject constructor(
    private val networkDataSource: AuthNetworkDataSource,
    private val keyManager: KeyManager,
    private val authPreferences: AuthPreferences,
    private val webSocketConnectionManager: WebSocketConnectionManager
) : AuthRepository {

    /** Flow emitting the current JWT access token. */
    override val accessToken: Flow<String?> = authPreferences.accessToken
    
    /** Flow emitting the current user's unique identifier. */
    override val userId: Flow<String?> = authPreferences.userId
    
    /** Flow emitting the current user's tag. */
    override val tag: Flow<String?> = authPreferences.tag

    /**
     * Logs out the current user by clearing local tokens, identity, and disconnecting WebSockets.
     */
    override suspend fun logout() {
        authPreferences.clearTokens()
        authPreferences.clearIdentity()
        webSocketConnectionManager.disconnect()
    }

    /**
     * Retrieves the current user's ID from local storage.
     *
     * @return The user ID string, or null if not authenticated.
     */
    override suspend fun getUserId(): String? {
        return authPreferences.userId.firstOrNull()
    }

    /**
     * Retrieves the current user's tag from local storage.
     *
     * @return The user tag string, or null if not authenticated.
     */
    override suspend fun getTag(): String? {
        return authPreferences.tag.firstOrNull()
    }

    /**
     * Registers a user offline for mesh network usage by generating an RSA key pair in the Android Keystore.
     *
     * @param tag The user's chosen tag/username.
     * @return A string indicating successful offline registration.
     */
    override suspend fun registerOffline(tag: String): String {
        keyManager.generateKeystoreRsaKeyPair("mesh_key_$tag")
        authPreferences.saveTag(tag)
        val pubKey = keyManager.getPublicKey("mesh_key_$tag")
        if (pubKey != null) {
            authPreferences.saveLocalPublicKey(Base64.encodeToString(pubKey.encoded, Base64.NO_WRAP))
        }
        return "offline_user_$tag"
    }

    /**
     * Registers a user online with the global server, including RSA key pair generation and cryptographic wrapping.
     *
     * Generates an in-memory RSA key pair, encrypts the private key with the user's password, and sends the
     * public key and wrapped private key to the server. The private key is also locally wrapped and stored.
     *
     * @param email The user's email address.
     * @param password The user's password.
     * @param tag The user's chosen tag/username.
     * @param firstName The user's first name.
     * @param lastName The user's last name (optional).
     * @return The newly registered user's unique identifier.
     */
    override suspend fun registerOnline(email: String, password: String, tag: String, firstName: String, lastName: String?): String {
        val passwordHash = hashString(password)

        val keyPair = keyManager.generateInMemoryRsaKeyPair()
        val rawPrivateKey = keyPair.private.encoded
        val encryptedPrivKey = keyManager.encryptWithPassword(rawPrivateKey, password)
        val publicKeyEncoded = keyPair.public.encoded
        val pubKeyBase64 = android.util.Base64.encodeToString(publicKeyEncoded, android.util.Base64.NO_WRAP)

        val request = RegisterRequest(
            email = email,
            tag = tag,
            firstName = firstName,
            lastName = lastName,
            passwordHash = passwordHash,
            publicKey = pubKeyBase64,
            encryptedPrivateKey = android.util.Base64.encodeToString(encryptedPrivKey, android.util.Base64.NO_WRAP)
        )
        
        val response = networkDataSource.register(request).getOrThrow()

        val localAesAlias = "local_aes_key_${response.userId}"
        val locallyWrappedKey = keyManager.wrapKeyWithLocalKeystore(localAesAlias, rawPrivateKey)
        authPreferences.saveLocalWrappedPrivateKey(android.util.Base64.encodeToString(locallyWrappedKey, android.util.Base64.NO_WRAP))
        authPreferences.saveLocalPublicKey(pubKeyBase64)

        authPreferences.saveUserId(response.userId)

        return response.userId
    }

    /**
     * Authenticates a user online using a challenge-response mechanism.
     *
     * Retrieves the encrypted private key from the server, decrypts it using the provided password,
     * signs a cryptographic challenge to prove identity, and securely stores the resulting session tokens.
     *
     * @param email The user's email address.
     * @param password The user's password.
     * @return The authenticated user's unique identifier.
     */
    override suspend fun loginOnline(email: String, password: String): String {
        val passwordHash = hashString(password)

        val loginResponse = networkDataSource.login(LoginRequest(email, passwordHash)).getOrThrow()

        val encryptedPrivKeyBytes = android.util.Base64.decode(loginResponse.encryptedPrivateKey, android.util.Base64.NO_WRAP)
        val privateKeyBytes = keyManager.decryptWithPassword(encryptedPrivKeyBytes, password)

        val localAesAlias = "local_aes_key_${loginResponse.userId}"
        val locallyWrappedKey = keyManager.wrapKeyWithLocalKeystore(localAesAlias, privateKeyBytes)
        authPreferences.saveLocalWrappedPrivateKey(android.util.Base64.encodeToString(locallyWrappedKey, android.util.Base64.NO_WRAP))
        
        // Extract public key from private key bytes and save it
        try {
            val keyFactory = java.security.KeyFactory.getInstance("RSA")
            val privateKeySpec = java.security.spec.PKCS8EncodedKeySpec(privateKeyBytes)
            val privateKey = keyFactory.generatePrivate(privateKeySpec) as java.security.interfaces.RSAPrivateCrtKey
            val publicKeySpec = java.security.spec.RSAPublicKeySpec(privateKey.modulus, privateKey.publicExponent)
            val publicKey = keyFactory.generatePublic(publicKeySpec)
            authPreferences.saveLocalPublicKey(android.util.Base64.encodeToString(publicKey.encoded, android.util.Base64.NO_WRAP))
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val challengeResponse = networkDataSource.challenge(ChallengeRequest(loginResponse.userId)).getOrThrow()

        val signatureBytes = keyManager.signDataWithRawKey(privateKeyBytes, challengeResponse.challenge.toByteArray())

        val verifyResponse = networkDataSource.verify(VerifyRequest(
            userId = loginResponse.userId,
            signature = Base64.encodeToString(signatureBytes, Base64.NO_WRAP)
        )).getOrThrow()
        
        authPreferences.saveTokens(verifyResponse.accessToken, verifyResponse.refreshToken)
        authPreferences.saveUserId(loginResponse.userId)
        
        return loginResponse.userId
    }

    /**
     * Checks if the user is currently authenticated by verifying the presence of local tokens or identity tags.
     *
     * @return True if logged in, false otherwise.
     */
    override suspend fun isLoggedIn(): Boolean {
        val token = authPreferences.accessToken.firstOrNull()
        val tag = authPreferences.tag.firstOrNull()
        return !token.isNullOrBlank() || !tag.isNullOrBlank()
    }

    /**
     * Generates a SHA-256 hash of the given string.
     *
     * @param input The plain text string.
     * @return The lowercase hexadecimal representation of the hash.
     */
    private fun hashString(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
