package ru.kubsu.borshchevyk.feature.chat.conversation.interactor

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.kubsu.borshchevyk.feature.chat.common.model.AttachmentFile
import ru.kubsu.borshchevyk.feature.chat.common.util.MediaUtil
import java.io.InputStream
import javax.inject.Inject

class LocalMediaInteractor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun resolveAttachment(uri: Uri): AttachmentFile? = withContext(Dispatchers.IO) {
        try {
            var fileName = uri.lastPathSegment ?: "unknown"
            var size = 0L
            if (uri.scheme == "file" && uri.path != null) {
                size = java.io.File(uri.path!!).length()
            } else {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIndex != -1) fileName = cursor.getString(nameIndex)
                        if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
                    }
                }
            }
            val contentType = context.contentResolver.getType(uri) ?: "application/octet-stream"
            val extension = fileName.substringAfterLast('.', "")
            
            // If it's a video, attempt to extract duration
            var duration: Int? = null
            if (contentType.startsWith("video/")) {
                duration = MediaUtil.extractDuration(context, uri).toInt()
            }

            AttachmentFile(
                uri = uri,
                sizeBytes = size,
                originalFilename = fileName,
                contentType = contentType,
                extension = extension,
                duration = duration
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    suspend fun resolveAudio(uri: Uri): AttachmentFile? = withContext(Dispatchers.IO) {
         try {
             var size = 0L
             if (uri.scheme == "file" && uri.path != null) {
                 size = java.io.File(uri.path!!).length()
             } else {
                 context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                     val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                     if (cursor.moveToFirst() && sizeIndex != -1) {
                         size = cursor.getLong(sizeIndex)
                     }
                 }
             }
             val duration = MediaUtil.extractDuration(context, uri)
             
             AttachmentFile(
                 uri = uri,
                 sizeBytes = size,
                 originalFilename = "voice_${System.currentTimeMillis()}.m4a",
                 contentType = "audio/mp4",
                 extension = "m4a",
                 duration = duration?.toInt()
             )
         } catch (e: Exception) {
             e.printStackTrace()
             null
         }
    }

    fun getInputStreamProvider(uri: Uri): () -> InputStream? = {
        try {
            context.contentResolver.openInputStream(uri)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
