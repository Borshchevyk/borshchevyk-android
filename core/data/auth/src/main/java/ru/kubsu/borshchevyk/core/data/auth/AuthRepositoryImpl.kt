package ru.kubsu.borshchevyk.core.data.auth

/**
 * Implementation of [AuthRepository] managing user identities and network authentication.
 *
 * Orchestrates API calls, secure key wrapping via [KeyManager], and local state persistence
 * via [AuthPreferences].
 *
 * @property networkDataSource the Ktor-based network client for API interaction
 * @property keyManager the security manager for crypto operations
 * @property authPreferences the Jetpack DataStore wrapper for local storage
 */
import android.util.Base64
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import ru.kubsu.borshchevyk.core.domain.auth.AuthRepository
import ru.kubsu.borshchevyk.core.model.dto.ChallengeRequest
import ru.kubsu.borshchevyk.core.model.dto.LoginRequest
import ru.kubsu.borshchevyk.core.model.dto.RegisterRequest
import ru.kubsu.borshchevyk.core.model.dto.VerifyRequest
import ru.kubsu.borshchevyk.core.network.auth.AuthNetworkDataSource
import ru.kubsu.borshchevyk.core.network.websocket.WebSocketDataSource
import ru.kubsu.borshchevyk.core.security.KeyManager
import java.security.MessageDigest
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val networkDataSource: AuthNetworkDataSource,
    private val keyManager: KeyManager,
    private val authPreferences: AuthPreferences,
    private val webSocketDataSource: WebSocketDataSource
) : AuthRepository {

    override val accessToken: Flow<String?> = authPreferences.accessToken
    override val userId: Flow<String?> = authPreferences.userId
    override val tag: Flow<String?> = authPreferences.tag

    override suspend fun logout() {
        authPreferences.clearTokens()
        authPreferences.clearIdentity()
        
        webSocketDataSource.disconnect()
        
    }

    override suspend fun getUserId(): String? {
        return authPreferences.userId.firstOrNull()
    }

    override suspend fun getTag(): String? {
        return authPreferences.tag.firstOrNull()
    }

    override suspend fun registerOffline(tag: String): String {
        keyManager.generateKeystoreRsaKeyPair("mesh_key_$tag")
        authPreferences.saveTag(tag)
        return "offline_user_$tag"
    }

    override suspend fun registerOnline(email: String, password: String, tag: String, firstName: String, lastName: String?): String {
        val passwordHash = hashString(password)

        val keyPair = keyManager.generateInMemoryRsaKeyPair()
        
        val rawPrivateKey = keyPair.private.encoded
        val encryptedPrivKey = keyManager.encryptWithPassword(rawPrivateKey, password)
        val publicKeyEncoded = keyPair.public.encoded

        val request = RegisterRequest(
            email = email,
            tag = tag,
            firstName = firstName,
            lastName = lastName,
            passwordHash = passwordHash,
            publicKey = Base64.encodeToString(publicKeyEncoded, Base64.NO_WRAP),
            encryptedPrivateKey = Base64.encodeToString(encryptedPrivKey, Base64.NO_WRAP)
        )
        
        val response = networkDataSource.register(request)
        
        val localAesAlias = "local_aes_key_${response.userId}"
        val locallyWrappedKey = keyManager.wrapKeyWithLocalKeystore(localAesAlias, rawPrivateKey)
        authPreferences.saveLocalWrappedPrivateKey(Base64.encodeToString(locallyWrappedKey, Base64.NO_WRAP))
        
        authPreferences.saveUserId(response.userId)
        
        return response.userId
    }

    override suspend fun loginOnline(email: String, password: String): String {
        val passwordHash = hashString(password)
        
        val loginResponse = networkDataSource.login(LoginRequest(email, passwordHash))
        
        val encryptedPrivKeyBytes = Base64.decode(loginResponse.encryptedPrivateKey, Base64.NO_WRAP)
        val privateKeyBytes = keyManager.decryptWithPassword(encryptedPrivKeyBytes, password)
        
        val localAesAlias = "local_aes_key_${loginResponse.userId}"
        val locallyWrappedKey = keyManager.wrapKeyWithLocalKeystore(localAesAlias, privateKeyBytes)
        authPreferences.saveLocalWrappedPrivateKey(Base64.encodeToString(locallyWrappedKey, Base64.NO_WRAP))
        
        val challengeResponse = networkDataSource.challenge(ChallengeRequest(loginResponse.userId))
        
        val signatureBytes = keyManager.signDataWithRawKey(privateKeyBytes, challengeResponse.challenge.toByteArray())
        
        val verifyResponse = networkDataSource.verify(VerifyRequest(
            userId = loginResponse.userId,
            signature = Base64.encodeToString(signatureBytes, Base64.NO_WRAP)
        ))
        
        authPreferences.saveTokens(verifyResponse.accessToken, verifyResponse.refreshToken)
        authPreferences.saveUserId(loginResponse.userId)
        
        return loginResponse.userId
    }

    override suspend fun isLoggedIn(): Boolean {
        val token = authPreferences.accessToken.firstOrNull()
        val tag = authPreferences.tag.firstOrNull()
        return !token.isNullOrBlank() || !tag.isNullOrBlank()
    }

    /**
     * Calculates the SHA-256 hash of a given string.
     *
     * @param input the string to hash (e.g., a plaintext password)
     * @return the lowercase hex string representation of the hash
     */
    private fun hashString(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
