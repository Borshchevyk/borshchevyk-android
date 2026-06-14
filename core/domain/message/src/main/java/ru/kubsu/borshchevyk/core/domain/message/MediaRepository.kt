package ru.kubsu.borshchevyk.core.domain.message

import ru.kubsu.borshchevyk.core.model.domain.DomainAttachmentResponse
import ru.kubsu.borshchevyk.core.model.domain.DomainAttachmentType

/**
 * Repository interface for managing media and attachments.
 *
 * This interface defines the contract for operations related to uploading,
 * retrieving, and deleting media files (such as images, videos, voice messages,
 * and avatars) used within the messaging system. It abstracts the underlying
 * storage mechanisms (e.g., S3, local cache).
 */
interface MediaRepository {
    /**
     * Requests a pre-signed upload URL for securely uploading a file to storage.
     *
     * @param originalFilename The original name of the file being uploaded.
     * @param contentType The MIME type of the file (e.g., "image/jpeg").
     * @param extension The file extension (e.g., ".jpg").
     * @param type The domain-specific classification of the attachment.
     * @param sizeBytes The size of the file in bytes.
     * @param width The width of the media if applicable (e.g., for images or videos).
     * @param height The height of the media if applicable.
     * @param duration The duration of the media in seconds if applicable (e.g., for audio or video).
     * @return A [Pair] containing the generated attachment ID and the pre-signed upload URL.
     */
    suspend fun requestUploadUrl(
        originalFilename: String,
        contentType: String,
        extension: String,
        type: DomainAttachmentType,
        sizeBytes: Long,
        width: Int? = null,
        height: Int? = null,
        duration: Double? = null
    ): Pair<String, String>

    /**
     * Uploads the file data directly to the storage service using a provided URL.
     *
     * @param url The pre-signed upload URL obtained from [requestUploadUrl].
     * @param inputStreamProvider A function that provides an InputStream of the file to be uploaded.
     * @param sizeBytes The total size of the file in bytes.
     * @param contentType The MIME type of the file data.
     */
    suspend fun uploadToS3(url: String, inputStreamProvider: () -> java.io.InputStream?, sizeBytes: Long, contentType: String)

    /**
     * Confirms the successful upload of an attachment with the backend.
     *
     * @param attachmentId The unique identifier of the attachment that was uploaded.
     * @return A [DomainAttachmentResponse] representing the finalized attachment metadata.
     */
    suspend fun completeUpload(attachmentId: String): DomainAttachmentResponse

    /**
     * Uploads a user's avatar image.
     *
     * @param fileBytes The raw byte array of the avatar image.
     * @param filename The intended filename for the avatar.
     * @param contentType The MIME type of the image (e.g., "image/png").
     * @return The URL or identifier of the uploaded avatar.
     */
    suspend fun uploadAvatar(fileBytes: ByteArray, filename: String, contentType: String): String

    /**
     * Uploads a voice message attachment.
     *
     * @param inputStreamProvider A function providing the input stream of the audio.
     * @param sizeBytes The size of the audio file in bytes.
     * @param duration The duration of the voice message in seconds.
     * @return A [DomainAttachmentResponse] representing the uploaded voice message metadata.
     */
    suspend fun uploadVoice(inputStreamProvider: () -> java.io.InputStream?, sizeBytes: Long, duration: Double): DomainAttachmentResponse

    /**
     * Uploads a circle video message attachment.
     *
     * @param inputStreamProvider A function providing the input stream of the video.
     * @param sizeBytes The size of the video file in bytes.
     * @param duration The duration of the video message in seconds.
     * @return A [DomainAttachmentResponse] representing the uploaded circle video metadata.
     */
    suspend fun uploadCircle(inputStreamProvider: () -> java.io.InputStream?, sizeBytes: Long, duration: Double): DomainAttachmentResponse

    /**
     * Retrieves the direct download or display URL for a given attachment.
     *
     * @param attachmentId The unique identifier of the attachment.
     * @return The fully qualified URL string for accessing the attachment.
     */
    suspend fun getAttachmentUrl(attachmentId: String): String

    /**
     * Retrieves the direct download or display URL for a given attachment's thumbnail.
     *
     * @param attachmentId The unique identifier of the attachment.
     * @return The fully qualified URL string for accessing the attachment's thumbnail.
     */
    suspend fun getAttachmentThumbnailUrl(attachmentId: String): String

    /**
     * Deletes an attachment permanently from the storage service.
     *
     * @param attachmentId The unique identifier of the attachment to be deleted.
     */
    suspend fun deleteAttachment(attachmentId: String)

    /**
     * Validates whether a list of attachments exists and are accessible.
     *
     * @param attachmentIds A list of attachment unique identifiers to validate.
     * @return True if all attachments are valid, false otherwise.
     */
    suspend fun validateAttachments(attachmentIds: List<String>): Boolean

    /**
     * Exports a locally cached or remote attachment to the public Downloads directory.
     *
     * @param attachmentId The ID of the attachment to export.
     * @return A URI string indicating the public file location, or an error message.
     */
    suspend fun exportAttachment(attachmentId: String): Result<String>
}
