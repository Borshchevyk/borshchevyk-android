package ru.kubsu.borshchevyk.feature.chat.ui.chat.components

import android.media.MediaMetadataRetriever
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.kubsu.borshchevyk.core.model.domain.ForwardPayload
import ru.kubsu.borshchevyk.core.model.domain.Message
import ru.kubsu.borshchevyk.core.ui.theme.BorshchevykTheme
import ru.kubsu.borshchevyk.feature.chat.AttachmentFile

/**
 * Input component for typing and sending messages, including text, voice, and circle videos.
 *
 * @param editingMessage The message currently being edited, if any.
 * @param isSending Whether a message is currently being sent.
 * @param onSendMessage Callback invoked when a text message with optional attachments is sent.
 * @param onSendVoice Callback invoked when a voice message is sent.
 * @param onSendCircle Callback invoked when a circle video message is sent.
 * @param onEditMessage Callback invoked when an existing message is edited.
 * @param onCancelEdit Callback invoked when message editing is canceled.
 * @param onTyping Callback invoked when the user is typing.
 * @param forwardPayload The payload of the message being forwarded, if any.
 */
@Composable
internal fun MessageInput(
    editingMessage: Message?,
    isSending: Boolean,
    onSendMessage: (String, List<AttachmentFile>) -> Unit,
    onSendVoice: (ByteArray, Double) -> Unit,
    onSendCircle: (ByteArray, Double) -> Unit,
    onEditMessage: (String, String) -> Unit,
    onCancelEdit: () -> Unit,
    onTyping: () -> Unit,
    forwardPayload: ForwardPayload? = null
) {
    var text by remember { mutableStateOf("") }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val selectedAttachments = remember { mutableStateListOf<AttachmentFile>() }

    var isRecordingVoice by remember { mutableStateOf(false) }
    var mediaRecorder by remember { mutableStateOf<android.media.MediaRecorder?>(null) }
    var voiceFile by remember { mutableStateOf<java.io.File?>(null) }

    /**
     * Extracts the duration in seconds and the raw byte array from a given media URI.
     *
     * @param uri The URI of the media file to process.
     * @return A pair containing the byte array of the file and its duration in seconds.
     */
    fun extractDurationAndBytes(uri: android.net.Uri): Pair<ByteArray?, Double> {
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
            e.printStackTrace()
        }
        return Pair(bytes, duration)
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            try {
                val file = java.io.File(context.cacheDir, "voice_${System.currentTimeMillis()}.m4a")
                voiceFile = file
                val recorder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    android.media.MediaRecorder(context)
                } else {
                    @Suppress("DEPRECATION")
                    android.media.MediaRecorder()
                }
                recorder.apply {
                    setAudioSource(android.media.MediaRecorder.AudioSource.MIC)
                    setOutputFormat(android.media.MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(android.media.MediaRecorder.AudioEncoder.AAC)
                    setOutputFile(file.absolutePath)
                    prepare()
                    start()
                }
                mediaRecorder = recorder
                isRecordingVoice = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val circleUri = remember { mutableStateOf<android.net.Uri?>(null) }
    val circleCaptureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo()
    ) { success ->
        if (success) {
            circleUri.value?.let { uri ->
                scope.launch(Dispatchers.IO) {
                    val (bytes, duration) = extractDurationAndBytes(uri)
                    if (bytes != null) {
                        withContext(Dispatchers.Main) {
                            onSendCircle(bytes, duration)
                        }
                    }
                }
            }
        }
    }

    val circlePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[android.Manifest.permission.CAMERA] ?: false
        val audioGranted = permissions[android.Manifest.permission.RECORD_AUDIO] ?: false
        if (cameraGranted && audioGranted) {
            val values = android.content.ContentValues().apply {
                put(android.provider.MediaStore.Video.Media.DISPLAY_NAME, "circle_${System.currentTimeMillis()}.mp4")
                put(android.provider.MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            }
            val uri = context.contentResolver.insert(android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            circleUri.value = uri
            if (uri != null) {
                circleCaptureLauncher.launch(uri)
            }
        } else {
            android.widget.Toast.makeText(context, "Permissions required for video recording", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        scope.launch(Dispatchers.IO) {
            val newAttachments = uris.mapNotNull { uri ->
                var fileName = "unknown"
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && nameIndex != -1) {
                        fileName = cursor.getString(nameIndex)
                    }
                }
                val contentType = context.contentResolver.getType(uri) ?: "application/octet-stream"
                val extension = fileName.substringAfterLast('.', "")
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes != null) {
                    AttachmentFile(uri = uri, bytes = bytes, originalFilename = fileName, contentType = contentType, extension = extension)
                } else null
            }
            withContext(Dispatchers.Main) {
                selectedAttachments.addAll(newAttachments)
            }
        }
    }

    LaunchedEffect(editingMessage) {
        text = editingMessage?.text ?: ""
    }

    Surface(
        color = BorshchevykTheme.colors.surface,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            if (forwardPayload != null) {
                ForwardPayloadBanner(forwardPayload)
            }

            if (editingMessage != null) {
                EditingMessageBanner(onCancelEdit)
            }

            AttachmentPreviewRow(
                attachments = selectedAttachments.toList(),
                onRemoveAttachment = { selectedAttachments.remove(it) }
            )

            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.Bottom) {
                if (isRecordingVoice) {
                    Row(
                        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(12.dp).clip(CircleShape).background(BorshchevykTheme.colors.error)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Recording Voice...", color = BorshchevykTheme.colors.onSurface, style = BorshchevykTheme.typography.bodyMedium)
                    }
                } else {
                    IconButton(onClick = { filePickerLauncher.launch("*/*") }, modifier = Modifier.padding(bottom = 4.dp, end = 4.dp)) {
                        Icon(Icons.Default.AttachFile, contentDescription = "Attach file", tint = BorshchevykTheme.colors.primary)
                    }
                    IconButton(onClick = { 
                        circlePermissionLauncher.launch(
                            arrayOf(
                                android.Manifest.permission.CAMERA,
                                android.Manifest.permission.RECORD_AUDIO
                            )
                        )
                    }, modifier = Modifier.padding(bottom = 4.dp, end = 4.dp)) {
                        Icon(Icons.Default.Videocam, contentDescription = "Record Circle", tint = BorshchevykTheme.colors.primary)
                    }
                    OutlinedTextField(
                        value = text,
                        onValueChange = { 
                            text = it
                            onTyping()
                        },
                        placeholder = { Text("Type a message...", color = BorshchevykTheme.colors.onSurfaceVariant) },
                        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BorshchevykTheme.colors.primary,
                            unfocusedBorderColor = BorshchevykTheme.colors.outline,
                            focusedContainerColor = BorshchevykTheme.colors.background,
                            unfocusedContainerColor = BorshchevykTheme.colors.background,
                            focusedTextColor = BorshchevykTheme.colors.onSurface,
                            unfocusedTextColor = BorshchevykTheme.colors.onSurface,
                            cursorColor = BorshchevykTheme.colors.primary
                        ),
                        maxLines = 4
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                if (text.isBlank() && selectedAttachments.isEmpty() && forwardPayload == null) {
                    // Show Mic / Stop button
                    Box(
                        modifier = Modifier.size(48.dp).clip(CircleShape).background(if (isRecordingVoice) BorshchevykTheme.colors.error else BorshchevykTheme.colors.primary)
                            .clickable {
                                if (isRecordingVoice) {
                                    try {
                                        mediaRecorder?.stop()
                                        mediaRecorder?.release()
                                    } catch (e: Exception) {}
                                    mediaRecorder = null
                                    isRecordingVoice = false

                                    voiceFile?.let { file ->
                                        scope.launch(Dispatchers.IO) {
                                            val bytes = file.readBytes()
                                            val uri = android.net.Uri.fromFile(file)
                                            val (_, duration) = extractDurationAndBytes(uri)
                                            withContext(Dispatchers.Main) {
                                                onSendVoice(bytes, duration)
                                            }
                                        }
                                    }
                                } else {
                                    audioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isRecordingVoice) Icons.Default.Stop else Icons.Default.Mic, 
                            contentDescription = if (isRecordingVoice) "Stop Recording" else "Record Voice", 
                            tint = if (isRecordingVoice) BorshchevykTheme.colors.onError else BorshchevykTheme.colors.onPrimary, 
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else {
                    // Show Send Button
                    Box(
                        modifier = Modifier.size(48.dp).clip(CircleShape).background(if (isSending) BorshchevykTheme.colors.surfaceVariant else BorshchevykTheme.colors.primary)
                            .clickable(enabled = !isSending) {
                                if (editingMessage != null) {
                                    onEditMessage(editingMessage.id, text)
                                } else {
                                    onSendMessage(text, selectedAttachments.toList())
                                    selectedAttachments.clear()
                                }
                                text = ""
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(color = BorshchevykTheme.colors.primary, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(if (editingMessage != null) Icons.Default.Check else Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = BorshchevykTheme.colors.onPrimary, modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
        }
    }
}
