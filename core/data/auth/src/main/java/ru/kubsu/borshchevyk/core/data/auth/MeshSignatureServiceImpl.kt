package ru.kubsu.borshchevyk.core.data.auth

import android.util.Base64
import kotlinx.coroutines.flow.firstOrNull
import ru.kubsu.borshchevyk.core.network.mesh.MeshSignatureService
import ru.kubsu.borshchevyk.core.security.KeyManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeshSignatureServiceImpl @Inject constructor(
    private val authPreferences: AuthPreferences,
    private val keyManager: KeyManager
) : MeshSignatureService {
    override suspend fun signData(dataToSign: ByteArray): String? {
        val userId = authPreferences.userId.firstOrNull() ?: return null
        val wrappedKeyBase64 = authPreferences.localWrappedPrivateKey.firstOrNull() ?: return null
        
        return try {
            val wrappedKeyBytes = Base64.decode(wrappedKeyBase64, Base64.NO_WRAP)
            val rawPrivKey = keyManager.unwrapKeyWithLocalKeystore("local_aes_key_$userId", wrappedKeyBytes)
            val signatureBytes = keyManager.signDataWithRawKey(rawPrivKey, dataToSign)
            Base64.encodeToString(signatureBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
