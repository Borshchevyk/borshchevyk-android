package ru.kubsu.borshchevyk.core.network.mesh

interface MeshSignatureService {
    suspend fun signData(dataToSign: ByteArray): String?
    suspend fun verifySignature(userId: String, signature: String, data: ByteArray): Boolean
    suspend fun getUserId(): String?
    suspend fun getLocalPublicKey(): String?
    
    /** Returns the partner's public key for a given chat ID (if it's a private chat). */
    suspend fun getPartnerPublicKeyForChat(chatId: String): String?
    
    /** Encrypts data using hybrid E2EE (AES-256 + RSA). */
    suspend fun encryptE2EE(data: ByteArray, publicKeyBase64: String): E2EEPayload?
    
    /** Decrypts an E2EE payload using the local private key. */
    suspend fun decryptE2EE(payload: E2EEPayload): ByteArray?
}
