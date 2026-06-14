package ru.kubsu.borshchevyk.feature.chat.common.util

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri

object MediaUtil {
    fun extractDurationAndBytes(context: Context, uri: Uri): Pair<ByteArray?, Double> {
        var duration = 0.0
        var bytes: ByteArray? = null
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            duration = time?.toLongOrNull()?.let { it / 1000.0 } ?: 0.0
            retriever.release()
            bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } catch (e: Exception) {
            // Error extracting metadata
        }
        return Pair(bytes, duration)
    }
}
