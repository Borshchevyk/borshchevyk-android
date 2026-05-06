package ru.kubsu.borshchevyk.core.network.mesh

interface MeshSignatureService {
    suspend fun signData(dataToSign: ByteArray): String?
    suspend fun verifySignature(userId: String, signature: String, data: ByteArray): Boolean
    suspend fun getUserId(): String?
    suspend fun getLocalPublicKey(): String?
}
