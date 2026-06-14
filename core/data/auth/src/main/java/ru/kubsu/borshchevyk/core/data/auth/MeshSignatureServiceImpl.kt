package ru.kubsu.borshchevyk.core.data.auth

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import ru.kubsu.borshchevyk.core.database.dao.ChatDao
import ru.kubsu.borshchevyk.core.database.dao.PublicKeyDao
import ru.kubsu.borshchevyk.core.network.mesh.E2EEPayload
import ru.kubsu.borshchevyk.core.network.mesh.MeshSignatureService
import ru.kubsu.borshchevyk.core.security.KeyManager
import java.security.KeyFactory
import java.security.spec.PKCS8EncodedKeySpec
import javax.crypto.Cipher
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeshSignatureServiceImpl @Inject constructor(
    private val authPreferences: AuthPreferences,
    private val keyManager: KeyManager,
    private val publicKeyDao: PublicKeyDao,
    private val chatDao: ChatDao
) : MeshSignatureService {

    override suspend fun signData(dataToSign: ByteArray): String? {
        val userId = authPreferences.userId.firstOrNull() ?: return null
        val wrappedKeyBase64 = authPreferences.localWrappedPrivateKey.firstOrNull()

        return try {
            if (wrappedKeyBase64 != null) {
                // Online user: use software-wrapped key
                val wrappedKeyBytes = Base64.decode(wrappedKeyBase64, Base64.NO_WRAP)
                val rawPrivKey = keyManager.unwrapKeyWithLocalKeystore("local_aes_key_$userId", wrappedKeyBytes)
                val signatureBytes = keyManager.signDataWithRawKey(rawPrivKey, dataToSign)
                Base64.encodeToString(signatureBytes, Base64.NO_WRAP)
            } else {
                // Offline user: use Android Hardware Keystore directly
                val tag = authPreferences.tag.firstOrNull() ?: return null
                val signatureBytes = keyManager.signData("mesh_key_$tag", dataToSign)
                Base64.encodeToString(signatureBytes, Base64.NO_WRAP)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun verifySignature(userId: String, signature: String, data: ByteArray): Boolean {
        return try {
            val pubKeyBase64 = publicKeyDao.getPublicKey(userId) ?: return false
            val pubKeyBytes = Base64.decode(pubKeyBase64, Base64.NO_WRAP)
            val signatureBytes = Base64.decode(signature, Base64.NO_WRAP)

            keyManager.verifyDataWithRawPublicKey(pubKeyBytes, data, signatureBytes)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun verifySignatureWithKey(publicKeyBase64: String, signature: String, data: ByteArray): Boolean {
        return try {
            val pubKeyBytes = Base64.decode(publicKeyBase64, Base64.NO_WRAP)
            val signatureBytes = Base64.decode(signature, Base64.NO_WRAP)

            keyManager.verifyDataWithRawPublicKey(pubKeyBytes, data, signatureBytes)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun hasPublicKey(userId: String): Boolean {
        return publicKeyDao.getPublicKey(userId) != null
    }

    override suspend fun getUserId(): String? {
        return authPreferences.userId.firstOrNull()
    }

    override suspend fun getLocalPublicKey(): String? {
        return authPreferences.localPublicKey.firstOrNull()
    }

    override suspend fun getPartnerPublicKeyForChat(chatId: String): String? {
        return withContext(Dispatchers.IO) {
            val chat = chatDao.getChat(chatId) ?: return@withContext null
            val partnerId = chat.partnerId ?: return@withContext null
            publicKeyDao.getPublicKey(partnerId)
        }
    }

    override suspend fun encryptE2EE(data: ByteArray, publicKeyBase64: String): E2EEPayload? {
        return try {
            val sessionKey = keyManager.generateAesSessionKey()
            val encryptedData = keyManager.encryptWithAes(data, sessionKey)
            val encryptedSessionKey = keyManager.encryptWithRsaPublicKey(sessionKey, publicKeyBase64)
            
            E2EEPayload(
                encryptedSessionKey = Base64.encodeToString(encryptedSessionKey, Base64.NO_WRAP),
                encryptedData = Base64.encodeToString(encryptedData, Base64.NO_WRAP)
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun decryptE2EE(payload: E2EEPayload): ByteArray? {
        return try {
            val encryptedSessionKeyBytes = Base64.decode(payload.encryptedSessionKey, Base64.NO_WRAP)
            val encryptedDataBytes = Base64.decode(payload.encryptedData, Base64.NO_WRAP)

            val sessionKey = decryptRsaWithLocalPrivateKey(encryptedSessionKeyBytes) ?: return null
            keyManager.decryptWithAes(encryptedDataBytes, sessionKey)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private suspend fun decryptRsaWithLocalPrivateKey(encryptedData: ByteArray): ByteArray? {
        val userId = authPreferences.userId.firstOrNull() ?: return null
        val wrappedKeyBase64 = authPreferences.localWrappedPrivateKey.firstOrNull()

        return try {
            if (wrappedKeyBase64 != null) {
                // Online user: use software-wrapped key
                val wrappedKeyBytes = Base64.decode(wrappedKeyBase64, Base64.NO_WRAP)
                val rawPrivKey = keyManager.unwrapKeyWithLocalKeystore("local_aes_key_$userId", wrappedKeyBytes)
                
                val kf = KeyFactory.getInstance("RSA")
                val privateKey = kf.generatePrivate(PKCS8EncodedKeySpec(rawPrivKey))
                val cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
                cipher.init(Cipher.DECRYPT_MODE, privateKey)
                cipher.doFinal(encryptedData)
            } else {
                // Offline user: use Android Hardware Keystore directly
                val tag = authPreferences.tag.firstOrNull() ?: return null
                keyManager.decryptWithRsaPrivateKey("mesh_key_$tag", encryptedData)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
