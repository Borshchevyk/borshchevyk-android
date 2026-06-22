package ru.kubsu.borshchevyk.core.domain.message

import ru.kubsu.borshchevyk.core.model.domain.DomainAttachmentType
import java.io.InputStream
import javax.inject.Inject

/**
 * Use case for securely uploading an attachment to the remote server.
 *
 * This use case orchestrates a multi-step upload process:
 * 1. Requests a pre-signed URL from the backend to ensure secure, direct-to-storage uploads.
 * 2. Uploads the raw file bytes directly to the storage service (e.g., S3) using the URL.
 * 3. Notifies the backend that the upload is complete to finalize the attachment record.
 *
 * @property mediaRepository The repository managing media and storage operations.
 */
class UploadAttachmentUseCase @Inject constructor(
    private val mediaRepository: MediaRepository
) {
    /**
     * Executes the attachment upload process.
     *
     * @param inputStreamProvider A function that provides the file stream to upload.
     * @param sizeBytes The size of the file in bytes.
     * @param originalFilename The original name of the file.
     * @param contentType The MIME type of the file.
     * @param extension The file extension including the dot (e.g., ".png").
     * @param type The domain-specific classification of the attachment.
     * @param width The optional width of the media (for images/video).
     * @param height The optional height of the media.
     * @param duration The optional duration of the media in seconds (for audio/video).
     * @return The unique string identifier of the successfully uploaded attachment.
     */
    suspend operator fun invoke(
        inputStreamProvider: () -> InputStream?,
        sizeBytes: Long,
        originalFilename: String,
        contentType: String,
        extension: String,
        type: DomainAttachmentType,
        width: Int? = null,
        height: Int? = null,
        duration: Double? = null
    ): String {
        // Step 1: Request pre-signed URL from backend
        val (attachmentId, uploadUrl) = mediaRepository.requestUploadUrl(
            originalFilename = originalFilename,
            contentType = contentType,
            extension = extension,
            type = type,
            sizeBytes = sizeBytes,
            width = width,
            height = height,
            duration = duration
        )

        // Step 2: Upload directly to storage via stream
        mediaRepository.uploadToS3(uploadUrl, inputStreamProvider, sizeBytes, contentType)

        // Step 3: Confirm upload with backend
        mediaRepository.completeUpload(attachmentId)

        return attachmentId
    }
}
