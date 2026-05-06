package ru.kubsu.borshchevyk.core.network.media

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.kubsu.borshchevyk.core.network.client.NetworkResult
import ru.kubsu.borshchevyk.core.network.dto.AttachmentResponse
import ru.kubsu.borshchevyk.core.network.dto.AttachmentStatus
import ru.kubsu.borshchevyk.core.network.dto.AttachmentType
import ru.kubsu.borshchevyk.core.network.dto.AttachmentUrlResult
import ru.kubsu.borshchevyk.core.network.dto.RequestUploadUrlRequest
import ru.kubsu.borshchevyk.core.network.dto.UploadUrlResult
import ru.kubsu.borshchevyk.core.network.dto.ValidateAttachmentsRequest
import ru.kubsu.borshchevyk.core.network.dto.ValidateAttachmentsResponse
import ru.kubsu.borshchevyk.core.network.mesh.MeshMediaTransferManager
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeshMediaNetworkDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val transferManager: MeshMediaTransferManager
) : MediaNetworkDataSource {

    private val attachmentCache = ConcurrentHashMap<String, AttachmentResponse>()

    fun getCachedAttachment(id: String): AttachmentResponse? {
        return attachmentCache[id]
    }

    override suspend fun requestUploadUrl(request: RequestUploadUrlRequest): NetworkResult<UploadUrlResult> {
        val fakeId = UUID.randomUUID().toString()
        // Determine type based on request, here we just use FILE as default for mesh cache simulation
        val response = AttachmentResponse(
            id = fakeId,
            uploaderId = "self",
            type = request.type,
            s3Key = "mesh-key-$fakeId",
            originalFilename = request.originalFilename,
            extension = "bin",
            contentType = request.contentType,
            sizeBytes = request.sizeBytes,
            status = AttachmentStatus.READY,
            createdAt = java.time.Instant.now().toString()
        )
        attachmentCache[fakeId] = response
        
        return NetworkResult.Success(UploadUrlResult(fakeId, "mesh://upload/$fakeId", "mesh-key-$fakeId"))
    }

    override suspend fun uploadToS3(url: String, fileBytes: ByteArray, contentType: String): NetworkResult<Unit> {
        val attachmentId = url.substringAfterLast("/")
        val file = File(context.cacheDir, "mesh_$attachmentId")
        withContext(Dispatchers.IO) {
            file.writeBytes(fileBytes)
        }
        val cached = attachmentCache[attachmentId]
        val filename = cached?.originalFilename ?: "file"
        transferManager.shareLocalFile(attachmentId, file, contentType, filename)
        return NetworkResult.Success(Unit)
    }

    override suspend fun completeUpload(attachmentId: String): NetworkResult<AttachmentResponse> {
        val response = attachmentCache[attachmentId] ?: AttachmentResponse(
            id = attachmentId,
            uploaderId = "self",
            type = AttachmentType.FILE,
            s3Key = "mesh-key-$attachmentId",
            originalFilename = "mesh_file",
            extension = "bin",
            contentType = "application/octet-stream",
            sizeBytes = 0L,
            status = AttachmentStatus.READY,
            createdAt = java.time.Instant.now().toString()
        )
        attachmentCache[attachmentId] = response
        return NetworkResult.Success(response)
    }

    override suspend fun uploadAvatar(fileBytes: ByteArray, filename: String, contentType: String): NetworkResult<AttachmentUrlResult> {
        val fakeId = UUID.randomUUID().toString()
        val file = File(context.cacheDir, "mesh_$fakeId")
        withContext(Dispatchers.IO) {
            file.writeBytes(fileBytes)
        }
        transferManager.shareLocalFile(fakeId, file, contentType, filename)
        return NetworkResult.Success(AttachmentUrlResult("mesh://avatar/$fakeId"))
    }

    override suspend fun uploadVoice(fileBytes: ByteArray, duration: Double): NetworkResult<AttachmentResponse> {
        val fakeId = UUID.randomUUID().toString()
        val file = File(context.cacheDir, "mesh_$fakeId")
        withContext(Dispatchers.IO) {
            file.writeBytes(fileBytes)
        }
        transferManager.shareLocalFile(fakeId, file, "audio/ogg", "voice.ogg")
        val response = AttachmentResponse(
            id = fakeId,
            uploaderId = "self",
            type = AttachmentType.VOICE,
            s3Key = "mesh-key-$fakeId",
            originalFilename = "voice.ogg",
            extension = "ogg",
            contentType = "audio/ogg",
            sizeBytes = fileBytes.size.toLong(),
            status = AttachmentStatus.READY,
            createdAt = java.time.Instant.now().toString()
        )
        attachmentCache[fakeId] = response
        return NetworkResult.Success(response)
    }

    override suspend fun uploadCircle(fileBytes: ByteArray, duration: Double): NetworkResult<AttachmentResponse> {
        val fakeId = UUID.randomUUID().toString()
        val file = File(context.cacheDir, "mesh_$fakeId")
        withContext(Dispatchers.IO) {
            file.writeBytes(fileBytes)
        }
        transferManager.shareLocalFile(fakeId, file, "video/mp4", "circle.mp4")
        val response = AttachmentResponse(
            id = fakeId,
            uploaderId = "self",
            type = AttachmentType.CIRCLE,
            s3Key = "mesh-key-$fakeId",
            originalFilename = "circle.mp4",
            extension = "mp4",
            contentType = "video/mp4",
            sizeBytes = fileBytes.size.toLong(),
            status = AttachmentStatus.READY,
            createdAt = java.time.Instant.now().toString()
        )
        attachmentCache[fakeId] = response
        return NetworkResult.Success(response)
    }


    override suspend fun getAttachmentUrl(attachmentId: String): NetworkResult<AttachmentUrlResult> {
        // Trigger a pull request just in case we don't have it locally
        transferManager.pullFile(attachmentId)
        return NetworkResult.Success(AttachmentUrlResult("mesh://download/$attachmentId"))
    }

    override suspend fun getAttachmentThumbnailUrl(attachmentId: String): NetworkResult<AttachmentUrlResult> {
        return NetworkResult.Success(AttachmentUrlResult("mesh://thumbnail/$attachmentId"))
    }

    override suspend fun deleteAttachment(attachmentId: String): NetworkResult<Unit> {
        return NetworkResult.Success(Unit)
    }

    override suspend fun validateAttachments(request: ValidateAttachmentsRequest): NetworkResult<ValidateAttachmentsResponse> {
        return NetworkResult.Success(ValidateAttachmentsResponse(valid = true, attachments = emptyList()))
    }
}