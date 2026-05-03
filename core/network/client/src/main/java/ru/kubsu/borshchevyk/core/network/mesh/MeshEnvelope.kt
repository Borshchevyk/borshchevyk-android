package ru.kubsu.borshchevyk.core.network.mesh

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class MeshEnvelope(
    val envelopeId: String,
    val originEndpointId: String,
    val action: String,
    val payload: String,
    val signature: String? = null
) {
    fun toByteArray(): ByteArray {
        return Json.encodeToString(this).toByteArray(Charsets.UTF_8)
    }

    companion object {
        fun fromByteArray(bytes: ByteArray): MeshEnvelope {
            val jsonString = bytes.toString(Charsets.UTF_8)
            return Json.decodeFromString(jsonString)
        }
    }
}