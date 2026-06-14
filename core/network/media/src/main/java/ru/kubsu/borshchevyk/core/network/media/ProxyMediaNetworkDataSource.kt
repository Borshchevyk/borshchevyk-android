package ru.kubsu.borshchevyk.core.network.media

import ru.kubsu.borshchevyk.core.network.client.NetworkMode
import ru.kubsu.borshchevyk.core.network.client.NetworkResult
import ru.kubsu.borshchevyk.core.network.client.TransportModeManager
import ru.kubsu.borshchevyk.core.network.dto.AttachmentResponse
import ru.kubsu.borshchevyk.core.network.dto.AttachmentUrlResult
import ru.kubsu.borshchevyk.core.network.dto.RequestUploadUrlRequest
import ru.kubsu.borshchevyk.core.network.dto.UploadUrlResult
import ru.kubsu.borshchevyk.core.network.dto.ValidateAttachmentsRequest
import ru.kubsu.borshchevyk.core.network.dto.ValidateAttachmentsResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProxyMediaNetworkDataSource @Inject constructor(
    private val ktorDataSource: KtorMediaNetworkDataSource,
    private val meshDataSource: MeshMediaNetworkDataSource,
    private val transportModeManager: TransportModeManager
) : MediaNetworkDataSource {

    private val currentDataSource: MediaNetworkDataSource
        get() = if (transportModeManager.networkMode.value == NetworkMode.MESH) {
            meshDataSource
        } else {
            ktorDataSource
        }

    override suspend fun requestUploadUrl(request: RequestUploadUrlRequest): NetworkResult<UploadUrlResult> =
        currentDataSource.requestUploadUrl(request)

    override suspend fun uploadToS3(url: String, inputStreamProvider: () -> java.io.InputStream?, sizeBytes: Long, contentType: String): NetworkResult<Unit> =
        currentDataSource.uploadToS3(url, inputStreamProvider, sizeBytes, contentType)

    override suspend fun completeUpload(attachmentId: String): NetworkResult<AttachmentResponse> =
        currentDataSource.completeUpload(attachmentId)

    override suspend fun uploadAvatar(fileBytes: ByteArray, filename: String, contentType: String): NetworkResult<AttachmentUrlResult> =
        currentDataSource.uploadAvatar(fileBytes, filename, contentType)

    override suspend fun uploadVoice(inputStreamProvider: () -> java.io.InputStream?, sizeBytes: Long, duration: Double): NetworkResult<AttachmentResponse> =
        currentDataSource.uploadVoice(inputStreamProvider, sizeBytes, duration)

    override suspend fun uploadCircle(inputStreamProvider: () -> java.io.InputStream?, sizeBytes: Long, duration: Double): NetworkResult<AttachmentResponse> =
        currentDataSource.uploadCircle(inputStreamProvider, sizeBytes, duration)

    override suspend fun getAttachmentUrl(attachmentId: String): NetworkResult<AttachmentUrlResult> =
        currentDataSource.getAttachmentUrl(attachmentId)

    override suspend fun getAttachmentThumbnailUrl(attachmentId: String): NetworkResult<AttachmentUrlResult> =
        currentDataSource.getAttachmentThumbnailUrl(attachmentId)

    override suspend fun deleteAttachment(attachmentId: String): NetworkResult<Unit> =
        currentDataSource.deleteAttachment(attachmentId)

    override suspend fun validateAttachments(request: ValidateAttachmentsRequest): NetworkResult<ValidateAttachmentsResponse> =
        currentDataSource.validateAttachments(request)

    override suspend fun exportAttachment(attachmentId: String): NetworkResult<String> =
        currentDataSource.exportAttachment(attachmentId)
}