package ru.kubsu.borshchevyk.core.network

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import ru.kubsu.borshchevyk.core.model.dto.AttachmentResponse
import ru.kubsu.borshchevyk.core.model.dto.AttachmentType
import ru.kubsu.borshchevyk.core.model.dto.AttachmentUrlResult
import ru.kubsu.borshchevyk.core.model.dto.ValidateAttachmentsRequest
import ru.kubsu.borshchevyk.core.model.dto.ValidateAttachmentsResponse
import javax.inject.Inject

class KtorMediaNetworkDataSource @Inject constructor(
    private val httpClient: HttpClient
) : MediaNetworkDataSource {

    private val TAG = "MediaNetworkDataSource"

    override suspend fun uploadFile(
        fileBytes: ByteArray,
        fileName: String,
        contentType: String,
        type: AttachmentType,
        width: Int?,
        height: Int?,
        duration: Double?
    ): AttachmentResponse {
        Log.d(TAG, "Uploading file: $fileName, type: $type, size: ${fileBytes.size}")
        return httpClient.post("api/v1/media/upload") {
            setBody(MultiPartFormDataContent(
                formData {
                    append("file", fileBytes, Headers.build {
                        append(HttpHeaders.ContentType, contentType)
                        append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                    })
                    append("type", type.name)
                    width?.let { append("width", it.toString()) }
                    height?.let { append("height", it.toString()) }
                    duration?.let { append("duration", it.toString()) }
                }
            ))
        }.body()
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
}
