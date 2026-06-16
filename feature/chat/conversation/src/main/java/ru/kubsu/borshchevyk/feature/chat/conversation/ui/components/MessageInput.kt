package ru.kubsu.borshchevyk.feature.chat.conversation.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import kotlinx.collections.immutable.persistentListOf
import ru.kubsu.borshchevyk.core.model.domain.ForwardPayload
import ru.kubsu.borshchevyk.core.ui.theme.BorshchevykTheme
import ru.kubsu.borshchevyk.feature.chat.conversation.ui.model.MessageUiModel
import java.io.File

@Composable
internal fun MessageInput(
    editingMessage: MessageUiModel?,
    isSending: Boolean,
    isRecordingVoice: Boolean,
    onSendMessage: (String, kotlinx.collections.immutable.PersistentList<Uri>) -> Unit,
    onSendCircle: (Uri) -> Unit,
    onEditMessage: (String, String) -> Unit,
    onCancelEdit: () -> Unit,
    onTyping: () -> Unit,
    onStartVoiceRecording: () -> Unit,
    onStopVoiceRecording: () -> Unit,
    forwardPayload: ForwardPayload? = null
) {
    var text by remember { mutableStateOf("") }
    var selectedAttachments by remember { mutableStateOf(persistentListOf<Uri>()) }

    val context = LocalContext.current

    val circleUri = remember { mutableStateOf<Uri?>(null) }
    val circleCaptureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo()
    ) { success ->
        if (success) {
            circleUri.value?.let { onSendCircle(it) }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val file = File(context.cacheDir, "circle_${System.currentTimeMillis()}.mp4")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            circleUri.value = uri
            circleCaptureLauncher.launch(uri)
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onStartVoiceRecording()
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        selectedAttachments = selectedAttachments.addAll(uris)
    }

    LaunchedEffect(editingMessage) {
        text = editingMessage?.text ?: ""
    }

    Surface(
        color = BorshchevykTheme.colors.surface,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.navigationBarsPadding()) {
            if (forwardPayload != null) {
                ForwardPayloadBanner(forwardPayload)
            }

            if (editingMessage != null) {
                EditingMessageBanner(onCancelEdit)
            }

            AttachmentPreviewRow(
                attachments = selectedAttachments,
                onRemoveAttachment = { selectedAttachments = selectedAttachments.remove(it) }
            )

            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
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
                        cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA) 
                    }, modifier = Modifier.padding(bottom = 4.dp, end = 4.dp)) {
                        Icon(Icons.Default.Videocam, contentDescription = "Record Circle", tint = BorshchevykTheme.colors.primary)
                    }
                    OutlinedTextField(
                        value = text,
                        onValueChange = { 
                            text = it
                            onTyping()
                        },
                        placeholder = { Text("Message", color = BorshchevykTheme.colors.onSurfaceVariant) },
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
                    Box(
                        modifier = Modifier.size(48.dp).clip(CircleShape).background(if (isRecordingVoice) BorshchevykTheme.colors.error else BorshchevykTheme.colors.primary)
                            .clickable {
                                if (isRecordingVoice) {
                                    onStopVoiceRecording()
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
                    Box(
                        modifier = Modifier.size(48.dp).clip(CircleShape).background(if (isSending) BorshchevykTheme.colors.surfaceVariant else BorshchevykTheme.colors.primary)
                            .clickable(enabled = !isSending) {
                                if (editingMessage != null) {
                                    onEditMessage(editingMessage.id, text)
                                } else {
                                    onSendMessage(text, selectedAttachments)
                                    selectedAttachments = persistentListOf()
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
