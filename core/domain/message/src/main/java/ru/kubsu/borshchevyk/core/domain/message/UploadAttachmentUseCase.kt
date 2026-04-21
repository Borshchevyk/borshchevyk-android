package ru.kubsu.borshchevyk.core.domain.message

import android.util.Log
import ru.kubsu.borshchevyk.core.model.dto.AttachmentType
import ru.kubsu.borshchevyk.core.model.dto.RequestUploadUrlRequest
import javax.inject.Inject

class UploadAttachmentUseCase @Inject constructor(
    private val mediaRepository: MediaRepository
) {
    private val TAG = "UploadAttachmentUseCase"

    suspend operator fun invoke(
        fileBytes: ByteArray,
        originalFilename: String,
        contentType: String,
        extension: String,
        type: AttachmentType,
        width: Int? = null,
        height: Int? = null,
        duration: Double? = null
    ): String {
        Log.d(TAG, "Initializing upload for $originalFilename ($contentType)")
        val request = RequestUploadUrlRequest(
            type = type,
            contentType = contentType,
            originalFilename = originalFilename,
            extension = extension,
            sizeBytes = fileBytes.size.toLong(),
            width = width,
            height = height,
            duration = duration
        )
        
        Log.d(TAG, "Step 1: Requesting upload URL from backend...")
        val urlResult = mediaRepository.requestUploadUrl(request)
        Log.i(TAG, "Received attachmentId: ${urlResult.attachmentId}")

        Log.d(TAG, "Step 2: Uploading binary data directly to S3...")
        mediaRepository.uploadFileToS3(urlResult.uploadUrl, fileBytes, contentType)
        
        Log.d(TAG, "Step 3: Notifying backend that upload is complete...")
        mediaRepository.completeUpload(urlResult.attachmentId)
        Log.i(TAG, "Upload flow finished successfully for ${urlResult.attachmentId}")
        
        return urlResult.attachmentId
    }
}
