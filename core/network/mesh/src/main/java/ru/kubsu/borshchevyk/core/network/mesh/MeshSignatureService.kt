package ru.kubsu.borshchevyk.core.network.mesh

interface MeshSignatureService {
    suspend fun signData(dataToSign: ByteArray): String?
    suspend fun getUserId(): String?
}
