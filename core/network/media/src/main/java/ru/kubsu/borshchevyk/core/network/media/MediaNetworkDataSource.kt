package ru.kubsu.borshchevyk.core.network.media

import kotlinx.coroutines.flow.Flow
import ru.kubsu.borshchevyk.core.network.client.NetworkResult
import ru.kubsu.borshchevyk.core.network.dto.AttachmentResponse
import ru.kubsu.borshchevyk.core.network.dto.AttachmentUrlResult
import ru.kubsu.borshchevyk.core.network.dto.RequestUploadUrlRequest
import ru.kubsu.borshchevyk.core.network.dto.UploadUrlResult
import ru.kubsu.borshchevyk.core.network.dto.ValidateAttachmentsRequest
import ru.kubsu.borshchevyk.core.network.dto.ValidateAttachmentsResponse

/**
 * Data source interface defining operations for uploading and managing media attachments.
 */
interface MediaNetworkDataSource {
    /**
     * Observes the download progress of a specific attachment.
     *
     * @param attachmentId The ID of the attachment.
     * @return A Flow emitting progress values from 0.0 to 1.0.
     */
    fun observeAttachmentProgress(attachmentId: String): Flow<Float>

    /**
     * Requests a pre-signed URL from the server to upload a file directly to storage (e.g., S3).
     *
     * @param request The metadata of the file to be uploaded.
     * @return A [NetworkResult] containing the [UploadUrlResult].
     */
    suspend fun requestUploadUrl(request: RequestUploadUrlRequest): NetworkResult<UploadUrlResult>

    /**
     * Performs the actual binary upload to the provided pre-signed URL via streaming.
     *
     * @param url The pre-signed upload URL.
     * @param inputStreamProvider A function providing the input stream.
     * @param sizeBytes The size of the file.
     * @param contentType The MIME type of the file.
     * @return A [NetworkResult] indicating upload success or failure.
     */
    suspend fun uploadToS3(url: String, inputStreamProvider: () -> java.io.InputStream?, sizeBytes: Long, contentType: String): NetworkResult<Unit>

    /**
     * Notifies the server that a file upload has been completed.
     *
     * @param attachmentId The ID of the newly uploaded attachment.
     * @return A [NetworkResult] containing the final [AttachmentResponse].
     */
    suspend fun completeUpload(attachmentId: String): NetworkResult<AttachmentResponse>

    /**
     * Convenience method to upload a user avatar directly.
     *
     * @param fileBytes The binary content of the avatar image.
     * @param filename The original filename.
     * @param contentType The MIME type of the image.
     * @return A [NetworkResult] containing the resulting [AttachmentUrlResult].
     */
    suspend fun uploadAvatar(fileBytes: ByteArray, filename: String, contentType: String): NetworkResult<AttachmentUrlResult>

    /**
     * Convenience method to upload a voice message directly.
     *
     * @param inputStreamProvider A function providing the input stream of the audio.
     * @param sizeBytes The size of the file.
     * @param duration The duration of the audio in seconds.
     * @return A [NetworkResult] containing the server's [AttachmentResponse].
     */
    suspend fun uploadVoice(inputStreamProvider: () -> java.io.InputStream?, sizeBytes: Long, duration: Double): NetworkResult<AttachmentResponse>

    /**
     * Convenience method to upload a video circle message directly.
     *
     * @param inputStreamProvider A function providing the input stream of the video.
     * @param sizeBytes The size of the file.
     * @param duration The duration of the video in seconds.
     * @return A [NetworkResult] containing the server's [AttachmentResponse].
     */
    suspend fun uploadCircle(inputStreamProvider: () -> java.io.InputStream?, sizeBytes: Long, duration: Double): NetworkResult<AttachmentResponse>

    /**
     * Requests a direct, pre-signed download URL for a given attachment.
     *
     * @param attachmentId The ID of the attachment.
     * @return A [NetworkResult] containing the [AttachmentUrlResult].
     */
    suspend fun getAttachmentUrl(attachmentId: String): NetworkResult<AttachmentUrlResult>

    /**
     * Requests a direct, pre-signed thumbnail URL for a given attachment.
     *
     * @param attachmentId The ID of the attachment.
     * @return A [NetworkResult] containing the [AttachmentUrlResult].
     */
    suspend fun getAttachmentThumbnailUrl(attachmentId: String): NetworkResult<AttachmentUrlResult>

    /**
     * Deletes an uploaded attachment.
     *
     * @param attachmentId The ID of the attachment to delete.
     * @return A [NetworkResult] indicating success or failure.
     */
    suspend fun deleteAttachment(attachmentId: String): NetworkResult<Unit>

    /**
     * Validates a batch of attachment IDs to ensure they are fully processed and ready for use.
     *
     * @param request The list of attachment IDs to check.
     * @return A [NetworkResult] containing the validation result.
     */
    suspend fun validateAttachments(request: ValidateAttachmentsRequest): NetworkResult<ValidateAttachmentsResponse>

    /**
     * Exports an attachment from the local cache to the device's public Downloads directory.
     *
     * @param attachmentId The ID of the attachment to export.
     * @return A [NetworkResult] containing the URI or path to the exported file, or an error.
     */
    suspend fun exportAttachment(attachmentId: String): NetworkResult<String>
}
