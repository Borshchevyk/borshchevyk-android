package ru.kubsu.borshchevyk.core.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import ru.kubsu.borshchevyk.core.model.dto.AttachmentResponse
import ru.kubsu.borshchevyk.core.model.dto.AttachmentUrlResult
import ru.kubsu.borshchevyk.core.model.dto.RequestUploadUrlRequest
import ru.kubsu.borshchevyk.core.model.dto.UploadUrlResult
import ru.kubsu.borshchevyk.core.model.dto.ValidateAttachmentsRequest
import ru.kubsu.borshchevyk.core.model.dto.ValidateAttachmentsResponse
import javax.inject.Inject

class KtorMediaNetworkDataSource @Inject constructor(
    private val httpClient: HttpClient
) : MediaNetworkDataSource {

    override suspend fun requestUploadUrl(request: RequestUploadUrlRequest): UploadUrlResult {
        return httpClient.post("api/v1/media/upload-url") {
            setBody(request)
        }.body()
    }

    override suspend fun completeUpload(attachmentId: String): AttachmentResponse {
        return httpClient.put("api/v1/media/$attachmentId/complete").body()
    }

    override suspend fun getAttachmentUrl(attachmentId: String): AttachmentUrlResult {
        return httpClient.get("api/v1/media/$attachmentId/url").body()
    }

    override suspend fun deleteAttachment(attachmentId: String) {
        httpClient.delete("api/v1/media/$attachmentId")
    }

    override suspend fun validateAttachments(request: ValidateAttachmentsRequest): ValidateAttachmentsResponse {
        return httpClient.post("api/v1/media/validate") {
            setBody(request)
        }.body()
    }

    override suspend fun uploadFileToS3(url: String, fileBytes: ByteArray, contentType: String) {
        val s3Client = HttpClient()
        try {
            s3Client.put(url) {
                this.contentType(ContentType.parse(contentType))
                setBody(fileBytes)
            }
        } finally {
            s3Client.close()
        }
    }
}
