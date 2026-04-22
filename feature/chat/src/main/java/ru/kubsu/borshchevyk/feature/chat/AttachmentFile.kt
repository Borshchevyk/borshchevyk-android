package ru.kubsu.borshchevyk.feature.chat

import android.net.Uri

data class AttachmentFile(
    val uri: Uri,
    val bytes: ByteArray,
    val originalFilename: String,
    val contentType: String,
    val extension: String,
    val width: Int? = null,
    val height: Int? = null,
    val duration: Double? = null
)
