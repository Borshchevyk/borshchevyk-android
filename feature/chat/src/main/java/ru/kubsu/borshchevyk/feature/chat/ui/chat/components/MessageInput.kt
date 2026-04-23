package ru.kubsu.borshchevyk.feature.chat.ui.chat.components

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
import androidx.compose.foundation.layout.imePadding
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
import ru.kubsu.borshchevyk.core.model.domain.Message
import ru.kubsu.borshchevyk.core.ui.theme.BorshchevykTheme
import ru.kubsu.borshchevyk.feature.chat.AttachmentFile

@Composable
internal fun MessageInput(
    editingMessage: Message?,
    isSending: Boolean,
    onSendMessage: (String, List<AttachmentFile>) -> Unit,
    onEditMessage: (String, String) -> Unit,
    onCancelEdit: () -> Unit,
    onTyping: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val selectedAttachments = remember { mutableStateListOf<AttachmentFile>() }

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
            if (editingMessage != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().background(BorshchevykTheme.colors.surfaceVariant).padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Editing message", style = BorshchevykTheme.typography.bodyMedium, color = BorshchevykTheme.colors.primary)
                    IconButton(onClick = onCancelEdit, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel edit", tint = BorshchevykTheme.colors.onSurfaceVariant)
                    }
                }
            }

            if (selectedAttachments.isNotEmpty()) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    selectedAttachments.forEach { attachment ->
                        Box(modifier = Modifier.size(60.dp).padding(end = 8.dp).clip(RoundedCornerShape(8.dp)).background(BorshchevykTheme.colors.surfaceVariant)) {
                            if (attachment.contentType.startsWith("image/")) {
                                AsyncImage(model = attachment.uri, contentDescription = "Preview", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            } else {
                                Icon(Icons.Default.AttachFile, contentDescription = "File", tint = BorshchevykTheme.colors.onSurfaceVariant, modifier = Modifier.align(Alignment.Center))
                            }
                            IconButton(
                                onClick = { selectedAttachments.remove(attachment) },
                                modifier = Modifier.size(20.dp).align(Alignment.TopEnd).background(BorshchevykTheme.colors.error, CircleShape)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remove", tint = BorshchevykTheme.colors.onError, modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }
            }

            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp).imePadding(), verticalAlignment = Alignment.Bottom) {
                IconButton(onClick = { filePickerLauncher.launch("*/*") }, modifier = Modifier.padding(bottom = 4.dp, end = 8.dp)) {
                    Icon(Icons.Default.AttachFile, contentDescription = "Attach file", tint = BorshchevykTheme.colors.primary)
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
                Spacer(modifier = Modifier.width(12.dp))
                Box(
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(if (isSending) BorshchevykTheme.colors.surfaceVariant else if (text.isNotBlank() || selectedAttachments.isNotEmpty()) BorshchevykTheme.colors.primary else BorshchevykTheme.colors.surfaceVariant)
                        .clickable(enabled = !isSending && (text.isNotBlank() || selectedAttachments.isNotEmpty())) {
                            if (text.isNotBlank() || selectedAttachments.isNotEmpty()) {
                                if (editingMessage != null) {
                                    onEditMessage(editingMessage.id, text)
                                } else {
                                    onSendMessage(text, selectedAttachments.toList())
                                    selectedAttachments.clear()
                                }
                                text = ""
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSending) {
                        CircularProgressIndicator(color = BorshchevykTheme.colors.primary, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(if (editingMessage != null) Icons.Default.Check else Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = if (text.isNotBlank() || selectedAttachments.isNotEmpty()) BorshchevykTheme.colors.onPrimary else BorshchevykTheme.colors.onSurfaceVariant, modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
    }
}
