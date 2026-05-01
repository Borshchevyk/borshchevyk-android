package ru.kubsu.borshchevyk.core.data.message

import ru.kubsu.borshchevyk.core.domain.message.MediaRepository
import ru.kubsu.borshchevyk.core.model.domain.DomainAttachmentResponse
import ru.kubsu.borshchevyk.core.model.domain.DomainAttachmentType
import ru.kubsu.borshchevyk.core.network.client.getOrThrow
import ru.kubsu.borshchevyk.core.network.dto.AttachmentResponse
import ru.kubsu.borshchevyk.core.network.dto.AttachmentType
import ru.kubsu.borshchevyk.core.network.dto.RequestUploadUrlRequest
import ru.kubsu.borshchevyk.core.network.dto.ValidateAttachmentsRequest
import ru.kubsu.borshchevyk.core.network.client.NetworkConstants
import ru.kubsu.borshchevyk.core.network.media.MediaNetworkDataSource
import javax.inject.Inject

/**
 * Implementation of [MediaRepository] responsible for handling media-related network operations.
 *
 * This repository manages the uploading of various types of media attachments (such as photos, videos,
 * voice messages, and avatars) to a remote server, often involving S3 storage. It facilitates
 * operations like requesting pre-signed URLs, performing direct uploads, and validating attachments.
 *
 * @property networkDataSource The data source handling remote network API calls for media operations.
 */
class MediaRepositoryImpl @Inject constructor(
    private val networkDataSource: MediaNetworkDataSource
) : MediaRepository {

    /**
     * Requests a pre-signed URL from the backend to directly upload a media file to S3 storage.
     *
     * This is typically the first step in a multi-part upload process, allowing the client to bypass
     * the main application server and upload directly to object storage for efficiency.
     *
     * @param originalFilename The original name of the file being uploaded.
     * @param contentType The MIME type of the file.
     * @param extension The file extension (e.g., "jpg", "mp4").
     * @param type The domain-specific classification of the attachment.
     * @param sizeBytes The size of the file in bytes.
     * @param width The width of the media in pixels (applicable for images/videos), null if not applicable.
     * @param height The height of the media in pixels (applicable for images/videos), null if not applicable.
     * @param duration The duration of the media in seconds (applicable for audio/video), null if not applicable.
     * @return A [Pair] containing the generated `attachmentId` and the `uploadUrl` to be used for the actual upload.
     */
    override suspend fun requestUploadUrl(
        originalFilename: String,
        contentType: String,
        extension: String,
        type: DomainAttachmentType,
        sizeBytes: Long,
        width: Int?,
        height: Int?,
        duration: Double?
    ): Pair<String, String> {
        val result = networkDataSource.requestUploadUrl(
            RequestUploadUrlRequest(
                type = type.toDto(),
                contentType = contentType,
                originalFilename = originalFilename,
                extension = extension,
                sizeBytes = sizeBytes,
                width = width,
                height = height,
                duration = duration
            )
        ).getOrThrow()
        return Pair(result.attachmentId, result.uploadUrl)
    }

    /**
     * Uploads the raw bytes of a file directly to the provided S3 pre-signed URL.
     *
     * This method performs a PUT request to the storage provider using the URL obtained via [requestUploadUrl].
     *
     * @param url The pre-signed upload URL.
     * @param fileBytes The byte array containing the file's content.
     * @param contentType The MIME type of the file, must match the one provided during URL request.
     */
    override suspend fun uploadToS3(url: String, fileBytes: ByteArray, contentType: String) {
        networkDataSource.uploadToS3(url, fileBytes, contentType).getOrThrow()
    }

    /**
     * Notifies the backend that an S3 upload has been completed successfully.
     *
     * The backend will verify the upload and finalize the attachment record, making it available
     * for use in messages or profiles.
     *
     * @param attachmentId The unique identifier of the attachment, obtained from [requestUploadUrl].
     * @return A [DomainAttachmentResponse] representing the finalized attachment details.
     */
    override suspend fun completeUpload(attachmentId: String): DomainAttachmentResponse {
        return networkDataSource.completeUpload(attachmentId).getOrThrow().toDomain()
    }

    /**
     * Uploads a user avatar directly to the server.
     *
     * Unlike generic attachments, avatars might use a different upload flow or endpoint.
     * This method handles the direct upload and constructs a fully qualified URL for the avatar.
     *
     * @param fileBytes The byte array of the avatar image.
     * @param filename The name of the avatar file.
     * @param contentType The MIME type of the avatar image.
     * @return The fully qualified URL pointing to the uploaded avatar.
     */
    override suspend fun uploadAvatar(fileBytes: ByteArray, filename: String, contentType: String): String {
        val url = networkDataSource.uploadAvatar(fileBytes, filename, contentType).getOrThrow().url
        return if (url.startsWith("http")) {
            url
        } else {
            "${NetworkConstants.BASE_URL}${if (url.startsWith("/")) "" else "/"}$url"
        }
    }

    /**
     * Uploads a voice message directly to the server.
     *
     * @param fileBytes The byte array of the audio recording.
     * @param duration The length of the voice message in seconds.
     * @return A [DomainAttachmentResponse] containing the uploaded voice message details.
     */
    override suspend fun uploadVoice(fileBytes: ByteArray, duration: Double): DomainAttachmentResponse {
        return networkDataSource.uploadVoice(fileBytes, duration).getOrThrow().toDomain()
    }

    /**
     * Uploads a video circle (video message) directly to the server.
     *
     * @param fileBytes The byte array of the video recording.
     * @param duration The length of the video circle in seconds.
     * @return A [DomainAttachmentResponse] containing the uploaded video circle details.
     */
    override suspend fun uploadCircle(fileBytes: ByteArray, duration: Double): DomainAttachmentResponse {
        return networkDataSource.uploadCircle(fileBytes, duration).getOrThrow().toDomain()
    }

    /**
     * Retrieves the direct download or streaming URL for a specific attachment.
     *
     * If the backend returns a relative path, this method resolves it against the base URL.
     *
     * @param attachmentId The unique identifier of the attachment.
     * @return The fully qualified URL to access the attachment content.
     */
    override suspend fun getAttachmentUrl(attachmentId: String): String {
        val url = networkDataSource.getAttachmentUrl(attachmentId).getOrThrow().url
        return if (url.startsWith("http")) {
            url
        } else {
            "${NetworkConstants.BASE_URL}${if (url.startsWith("/")) "" else "/"}$url"
        }
    }

    /**
     * Deletes an attachment from the server and storage.
     *
     * @param attachmentId The unique identifier of the attachment to be deleted.
     */
    override suspend fun deleteAttachment(attachmentId: String) {
        networkDataSource.deleteAttachment(attachmentId).getOrThrow()
    }

    /**
     * Validates a list of attachments by their IDs.
     *
     * This can be used to ensure that all required attachments for a message are fully processed
     * and available before attempting to send the message.
     *
     * @param attachmentIds A list of attachment unique identifiers.
     * @return `true` if all attachments are valid and accessible, `false` otherwise.
     */
    override suspend fun validateAttachments(attachmentIds: List<String>): Boolean {
        return networkDataSource.validateAttachments(ValidateAttachmentsRequest(attachmentIds)).getOrThrow().valid
    }

    private fun DomainAttachmentType.toDto(): AttachmentType = when (this) {
        DomainAttachmentType.PHOTO -> AttachmentType.PHOTO
        DomainAttachmentType.VIDEO -> AttachmentType.VIDEO
        DomainAttachmentType.VOICE -> AttachmentType.VOICE
        DomainAttachmentType.CIRCLE -> AttachmentType.CIRCLE
        DomainAttachmentType.FILE -> AttachmentType.FILE
        DomainAttachmentType.STICKER -> AttachmentType.STICKER
        DomainAttachmentType.AVATAR -> AttachmentType.AVATAR
    }

    private fun AttachmentType.toDomain(): DomainAttachmentType = when (this) {
        AttachmentType.PHOTO -> DomainAttachmentType.PHOTO
        AttachmentType.VIDEO -> DomainAttachmentType.VIDEO
        AttachmentType.VOICE -> DomainAttachmentType.VOICE
        AttachmentType.CIRCLE -> DomainAttachmentType.CIRCLE
        AttachmentType.FILE -> DomainAttachmentType.FILE
        AttachmentType.STICKER -> DomainAttachmentType.STICKER
        AttachmentType.AVATAR -> DomainAttachmentType.AVATAR
    }

    private fun AttachmentResponse.toDomain(): DomainAttachmentResponse = DomainAttachmentResponse(
        id = id,
        type = type?.toDomain(),
        originalFilename = originalFilename,
        extension = extension,
        sizeBytes = sizeBytes,
        thumbnailKey = thumbnailKey,
        updatedAt = updatedAt,
        width = width,
        height = height,
        duration = duration
    )
}
