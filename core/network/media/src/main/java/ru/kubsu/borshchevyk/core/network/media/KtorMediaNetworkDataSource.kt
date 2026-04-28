package ru.kubsu.borshchevyk.core.network.media

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import ru.kubsu.borshchevyk.core.model.domain.NetworkResult
import ru.kubsu.borshchevyk.core.model.dto.AttachmentResponse
import ru.kubsu.borshchevyk.core.model.dto.AttachmentUrlResult
import ru.kubsu.borshchevyk.core.model.dto.RequestUploadUrlRequest
import ru.kubsu.borshchevyk.core.model.dto.UploadUrlResult
import ru.kubsu.borshchevyk.core.model.dto.ValidateAttachmentsRequest
import ru.kubsu.borshchevyk.core.model.dto.ValidateAttachmentsResponse
import ru.kubsu.borshchevyk.core.network.client.safeRequest
import ru.kubsu.borshchevyk.core.network.di.IoDispatcher
import javax.inject.Inject

class KtorMediaNetworkDataSource @Inject constructor(
    private val httpClient: HttpClient,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : MediaNetworkDataSource {

    private val TAG = "MediaNetworkDataSource"

    override suspend fun uploadAvatar(fileBytes: ByteArray, filename: String, contentType: String): NetworkResult<AttachmentUrlResult> {
        return withContext(ioDispatcher) {
            safeRequest {
                httpClient.post("api/v1/media/upload/avatar") {
                    setBody(
                        MultiPartFormDataContent(
                            formData {
                                append("file", fileBytes, Headers.build {
                                    append(HttpHeaders.ContentType, contentType)
                                    append(HttpHeaders.ContentDisposition, "filename=\"$filename\"")
                                })
                            }
                        )
                    )
                }
            }
        }
    }

    override suspend fun uploadVoice(fileBytes: ByteArray, duration: Double): NetworkResult<AttachmentResponse> {
        return withContext(ioDispatcher) {
            safeRequest {
                httpClient.post("api/v1/media/upload/voice") {
                    setBody(
                        MultiPartFormDataContent(
                            formData {
                                append("duration", duration.toString())
                                append("file", fileBytes, Headers.build {
                                    append(HttpHeaders.ContentType, "audio/ogg")
                                    append(HttpHeaders.ContentDisposition, "filename=\"voice.ogg\"")
                                })
                            }
                        )
                    )
                }
            }
        }
    }

    override suspend fun uploadCircle(fileBytes: ByteArray, duration: Double): NetworkResult<AttachmentResponse> {
        return withContext(ioDispatcher) {
            safeRequest {
                httpClient.post("api/v1/media/upload/circle") {
                    setBody(
                        MultiPartFormDataContent(
                            formData {
                                append("duration", duration.toString())
                                append("file", fileBytes, Headers.build {
                                    append(HttpHeaders.ContentType, "video/mp4")
                                    append(HttpHeaders.ContentDisposition, "filename=\"circle.mp4\"")
                                })
                            }
                        )
                    )
                }
            }
        }
    }

    override suspend fun requestUploadUrl(request: RequestUploadUrlRequest): NetworkResult<UploadUrlResult> {
        Log.d(TAG, "Requesting upload URL for: ${request.originalFilename}, type: ${request.type}")
        return withContext(ioDispatcher) {
            safeRequest {
                httpClient.post("api/v1/media/upload-url") {
                    setBody(request)
                }
            }
        }
    }

    override suspend fun uploadToS3(url: String, fileBytes: ByteArray, contentType: String): NetworkResult<Unit> {
        Log.d(TAG, "Uploading file to S3: size ${fileBytes.size}, content-type: $contentType")
        return withContext(ioDispatcher) {
            safeRequest {
                httpClient.put(url) {
                    contentType(ContentType.parse(contentType))
                    setBody(fileBytes)
                }
            }
        }
    }

    override suspend fun completeUpload(attachmentId: String): NetworkResult<AttachmentResponse> {
        Log.d(TAG, "Completing upload for attachment: $attachmentId")
        return withContext(ioDispatcher) {
            safeRequest {
                httpClient.put("api/v1/media/$attachmentId/complete")
            }
        }
    }

    override suspend fun getAttachmentUrl(attachmentId: String): NetworkResult<AttachmentUrlResult> {
        return withContext(ioDispatcher) {
            safeRequest {
                httpClient.get("api/v1/media/$attachmentId/url")
            }
        }
    }

    override suspend fun deleteAttachment(attachmentId: String): NetworkResult<Unit> {
        return withContext(ioDispatcher) {
            safeRequest {
                httpClient.delete("api/v1/media/$attachmentId")
            }
        }
    }

    override suspend fun validateAttachments(request: ValidateAttachmentsRequest): NetworkResult<ValidateAttachmentsResponse> {
        return withContext(ioDispatcher) {
            safeRequest {
                httpClient.post("api/v1/media/validate") {
                    setBody(request)
                }
            }
        }
    }
}
