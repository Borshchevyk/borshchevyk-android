package ru.kubsu.borshchevyk.feature.chat.common.util

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri

object MediaUtil {
    fun extractDuration(context: Context, uri: Uri): Double {
        var duration = 0.0
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            duration = time?.toLongOrNull()?.let { it / 1000.0 } ?: 0.0
        } catch (e: Exception) {
            // Error extracting metadata
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {}
        }
        return duration
    }
}
