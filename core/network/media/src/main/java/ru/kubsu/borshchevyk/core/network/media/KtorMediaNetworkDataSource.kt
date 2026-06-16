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
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.utils.io.close
import io.ktor.utils.io.writeFully
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.kubsu.borshchevyk.core.network.client.NetworkResult
import ru.kubsu.borshchevyk.core.network.di.IoDispatcher
import ru.kubsu.borshchevyk.core.network.dto.AttachmentResponse
import ru.kubsu.borshchevyk.core.network.dto.AttachmentUrlResult
import ru.kubsu.borshchevyk.core.network.dto.RequestUploadUrlRequest
import ru.kubsu.borshchevyk.core.network.dto.UploadUrlResult
import ru.kubsu.borshchevyk.core.network.dto.ValidateAttachmentsRequest
import ru.kubsu.borshchevyk.core.network.dto.ValidateAttachmentsResponse
import ru.kubsu.borshchevyk.core.network.ktor.client.safeRequest
import javax.inject.Inject

class KtorMediaNetworkDataSource @Inject constructor(
    private val httpClient: HttpClient,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : MediaNetworkDataSource {

    private val TAG = "MediaNetworkDataSource"

    override fun observeAttachmentProgress(attachmentId: String): Flow<Float> {
        return flowOf(0f)
    }

    override fun observeIncomingFiles(): Flow<String> {
        return emptyFlow()
    }

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

    override suspend fun uploadVoice(inputStreamProvider: () -> java.io.InputStream?, sizeBytes: Long, duration: Double): NetworkResult<AttachmentResponse> {
        return withContext(ioDispatcher) {
            val requestScope = this
            safeRequest {
                httpClient.post("api/v1/media/upload/voice") {
                    setBody(
                        MultiPartFormDataContent(
                            formData {
                                append("duration", duration.toString())
                                append("file", io.ktor.client.request.forms.ChannelProvider(sizeBytes) {
                                    val channel = io.ktor.utils.io.ByteChannel()
                                    requestScope.launch {
                                        try {
                                            val stream = inputStreamProvider() ?: throw IllegalStateException("Could not open input stream")
                                            stream.use { input ->
                                                val buffer = ByteArray(8192)
                                                var bytesRead: Int
                                                while (input.read(buffer).also { bytesRead = it } != -1) {
                                                    channel.writeFully(buffer, 0, bytesRead)
                                                }
                                            }
                                        } catch (e: Exception) {
                                            channel.close(e)
                                        } finally {
                                            channel.close()
                                        }
                                    }
                                    channel
                                }, Headers.build {
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

    override suspend fun uploadCircle(inputStreamProvider: () -> java.io.InputStream?, sizeBytes: Long, duration: Double): NetworkResult<AttachmentResponse> {
        return withContext(ioDispatcher) {
            val requestScope = this
            safeRequest {
                httpClient.post("api/v1/media/upload/circle") {
                    setBody(
                        MultiPartFormDataContent(
                            formData {
                                append("duration", duration.toString())
                                append("file", io.ktor.client.request.forms.ChannelProvider(sizeBytes) {
                                    val channel = io.ktor.utils.io.ByteChannel()
                                    requestScope.launch {
                                        try {
                                            val stream = inputStreamProvider() ?: throw IllegalStateException("Could not open input stream")
                                            stream.use { input ->
                                                val buffer = ByteArray(8192)
                                                var bytesRead: Int
                                                while (input.read(buffer).also { bytesRead = it } != -1) {
                                                    channel.writeFully(buffer, 0, bytesRead)
                                                }
                                            }
                                        } catch (e: Exception) {
                                            channel.close(e)
                                        } finally {
                                            channel.close()
                                        }
                                    }
                                    channel
                                }, Headers.build {
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

    override suspend fun uploadToS3(url: String, inputStreamProvider: () -> java.io.InputStream?, sizeBytes: Long, contentType: String): NetworkResult<Unit> {
        Log.d(TAG, "Uploading file to S3: size $sizeBytes, content-type: $contentType")
        return withContext(ioDispatcher) {
            safeRequest {
                httpClient.put(url) {
                    contentType(io.ktor.http.ContentType.parse(contentType))
                    setBody(object : io.ktor.http.content.OutgoingContent.WriteChannelContent() {
                        override val contentLength: Long = sizeBytes
                        override val contentType: io.ktor.http.ContentType = io.ktor.http.ContentType.parse(contentType)
                        override suspend fun writeTo(channel: io.ktor.utils.io.ByteWriteChannel) {
                            val stream = inputStreamProvider() ?: throw IllegalStateException("Could not open input stream")
                            stream.use { input ->
                                val buffer = ByteArray(8192)
                                var bytesRead: Int
                                while (input.read(buffer).also { bytesRead = it } != -1) {
                                    channel.writeFully(buffer, 0, bytesRead)
                                }
                            }
                        }
                    })
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

    override suspend fun getAttachmentThumbnailUrl(attachmentId: String): NetworkResult<AttachmentUrlResult> {
        return withContext(ioDispatcher) {
            safeRequest {
                httpClient.get("api/v1/media/$attachmentId/thumbnail-url")
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

    override suspend fun exportAttachment(attachmentId: String): NetworkResult<String> {
        // For Ktor (online mode), we can simply trigger the Android DownloadManager
        // using the attachment's direct URL. Since getting context here is tricky if not injected,
        // we can just return the URL, and let the UI layer handle DownloadManager.
        return try {
            val response = getAttachmentUrl(attachmentId)
            if (response is NetworkResult.Success) {
                NetworkResult.Success(response.data.url)
            } else {
                NetworkResult.Error(code = 404, message = "Could not resolve URL for Ktor download.")
            }
        } catch (e: Exception) {
            NetworkResult.Error(code = 500, message = e.message)
        }
    }
}
