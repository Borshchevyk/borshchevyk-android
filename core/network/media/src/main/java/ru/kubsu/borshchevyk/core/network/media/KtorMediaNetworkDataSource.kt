package ru.kubsu.borshchevyk.core.network.media

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import ru.kubsu.borshchevyk.core.model.dto.AttachmentResponse
import ru.kubsu.borshchevyk.core.model.dto.AttachmentUrlResult
import ru.kubsu.borshchevyk.core.model.dto.RequestUploadUrlRequest
import ru.kubsu.borshchevyk.core.model.dto.UploadUrlResult
import ru.kubsu.borshchevyk.core.model.dto.ValidateAttachmentsRequest
import ru.kubsu.borshchevyk.core.model.dto.ValidateAttachmentsResponse
import ru.kubsu.borshchevyk.core.network.di.IoDispatcher
import javax.inject.Inject

class KtorMediaNetworkDataSource @Inject constructor(
    private val httpClient: HttpClient,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : MediaNetworkDataSource {

    private val TAG = "MediaNetworkDataSource"

    override suspend fun requestUploadUrl(request: RequestUploadUrlRequest): UploadUrlResult {
        Log.d(TAG, "Requesting upload URL for: ${request.originalFilename}, type: ${request.type}")
        return withContext(ioDispatcher) {
            httpClient.post("api/v1/media/upload-url") {
                setBody(request)
            }.body()
        }
    }

    override suspend fun uploadToS3(url: String, fileBytes: ByteArray, contentType: String) {
        Log.d(TAG, "Uploading file to S3: size ${fileBytes.size}, content-type: $contentType")
        return withContext(ioDispatcher) {
            httpClient.put(url) {
                contentType(ContentType.parse(contentType))
                setBody(fileBytes)
            }
        }
    }

    override suspend fun completeUpload(attachmentId: String): AttachmentResponse {
        Log.d(TAG, "Completing upload for attachment: $attachmentId")
        return withContext(ioDispatcher) {
            httpClient.put("api/v1/media/$attachmentId/complete").body()
        }
    }

    override suspend fun getAttachmentUrl(attachmentId: String): AttachmentUrlResult {
        return withContext(ioDispatcher) {
            httpClient.get("api/v1/media/$attachmentId/url").body()
        }
    }

    override suspend fun deleteAttachment(attachmentId: String) {
        return withContext(ioDispatcher) {
            httpClient.delete("api/v1/media/$attachmentId")
        }
    }

    override suspend fun validateAttachments(request: ValidateAttachmentsRequest): ValidateAttachmentsResponse {
        return withContext(ioDispatcher) {
            httpClient.post("api/v1/media/validate") {
                setBody(request)
            }.body()
        }
    }
}
