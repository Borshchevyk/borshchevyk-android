package ru.kubsu.borshchevyk.feature.chat

import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.kubsu.borshchevyk.core.model.domain.ChatMember
import ru.kubsu.borshchevyk.core.model.domain.ChatMemberRole
import ru.kubsu.borshchevyk.core.model.domain.Message
import ru.kubsu.borshchevyk.core.model.dto.UpdatePermissionsRequest
import ru.kubsu.borshchevyk.core.ui.theme.BorshchevykTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoute(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isChatDeleted) {
        if (uiState.isChatDeleted) {
            onBackClick()
        }
    }

    if (uiState.showSettings) {
        ChatSettingsScreen(
            uiState = uiState,
            onBackClick = { viewModel.toggleSettings() },
            onInvite = { viewModel.onInviteUser(it) },
            onGenerateLink = { viewModel.onGenerateInviteLink() },
            onUpdatePermissions = { targetUserId, request -> viewModel.onUpdatePermissions(targetUserId, request) },
            onClearHistory = { forAll -> viewModel.onClearHistory(forAll) },
            onDeleteChat = { viewModel.onDeleteChat() },
            onKickUser = { targetUserId -> viewModel.onKickUser(targetUserId) },
            onLeaveChat = { viewModel.onLeaveChat() },
            onUpdateChatInfo = { title, desc -> viewModel.onUpdateChatInfo(title, desc) }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Text(
                            text = if (uiState.isGroupChat) "Group Chat" else "Private Chat",
                            style = BorshchevykTheme.typography.titleMedium,
                            color = BorshchevykTheme.colors.onSurface
                        ) 
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack, 
                                contentDescription = "Back",
                                tint = BorshchevykTheme.colors.onSurface
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.toggleSettings() }) {
                            Icon(
                                Icons.Default.Info, 
                                contentDescription = "Chat Info",
                                tint = BorshchevykTheme.colors.onSurface
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = BorshchevykTheme.colors.background
                    )
                )
            },
            bottomBar = {
                MessageInput(
                    editingMessage = uiState.editingMessage,
                    isSending = uiState.isSending,
                    onSendMessage = { text, attachments -> viewModel.onSendMessage(text, attachments) },
                    onEditMessage = viewModel::onEditMessage,
                    onCancelEdit = { viewModel.setEditingMessage(null) },
                    onTyping = viewModel::onTyping
                )
            },
            containerColor = BorshchevykTheme.colors.background,
            modifier = modifier
        ) { padding ->
            ChatScreen(
                uiState = uiState,
                resolveAttachmentUrl = { viewModel.resolveAttachmentUrl(it) },
                onPinToggle = { msg ->
                    if (msg.isPinned) viewModel.onUnpinMessage(msg.id)
                    else viewModel.onPinMessage(msg.id)
                },
                onReactionToggle = { msgId, reaction ->
                    viewModel.onToggleReaction(msgId, reaction)
                },
                onEdit = { msg -> viewModel.setEditingMessage(msg) },
                onDelete = { msgId, forAll ->
                    viewModel.onDeleteMessage(msgId, forAll)
                },
                onMessageVisible = { msgId ->
                    viewModel.onMessageVisible(msgId)
                },
                onLoadReaders = { msgId ->
                    viewModel.onLoadReaders(msgId)
                },
                onLoadComments = { msgId ->
                    viewModel.onLoadComments(msgId)
                },
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatSettingsScreen(
    uiState: ChatUiState,
    onBackClick: () -> Unit,
    onInvite: (String) -> Unit,
    onGenerateLink: () -> Unit,
    onUpdatePermissions: (String, UpdatePermissionsRequest) -> Unit,
    onClearHistory: (Boolean) -> Unit,
    onDeleteChat: () -> Unit,
    onKickUser: (String) -> Unit,
    onLeaveChat: () -> Unit,
    onUpdateChatInfo: (String?, String?) -> Unit
) {
    var showInviteDialog by remember { mutableStateOf(false) }
    var memberForPermissions by remember { mutableStateOf<ChatMember?>(null) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showDeleteChatDialog by remember { mutableStateOf(false) }
    var showUpdateInfoDialog by remember { mutableStateOf(false) }

    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val currentUserMember = uiState.members.find { it.userId == uiState.currentUserId }
    val canManagePermissions = uiState.isGroupChat &&
            (currentUserMember?.role == ChatMemberRole.OWNER || currentUserMember?.role == ChatMemberRole.ADMIN)
    val canChangeInfo = uiState.isGroupChat && (currentUserMember?.canChangeInfo == true || canManagePermissions)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat Settings", style = BorshchevykTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                if (canChangeInfo) {
                    Button(
                        onClick = { showUpdateInfoDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Edit Chat Info")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (uiState.isGroupChat) {
                    Button(
                        onClick = { showInviteDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = BorshchevykTheme.colors.primary)
                    ) {
                        Text("Invite User")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = onGenerateLink,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = BorshchevykTheme.colors.primaryContainer, contentColor = BorshchevykTheme.colors.primary)
                    ) {
                        Text("Generate Invite Link")
                    }

                    if (uiState.inviteLink != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clickable {
                                    clipboardManager.setText(AnnotatedString(uiState.inviteLink))
                                    Toast.makeText(context, "Link copied to clipboard", Toast.LENGTH_SHORT).show()
                                },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Link: ${uiState.inviteLink}",
                                style = BorshchevykTheme.typography.bodyMedium,
                                color = BorshchevykTheme.colors.onSurface,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Link",
                                tint = BorshchevykTheme.colors.primary,
                                modifier = Modifier.size(20.dp).padding(start = 8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
                Text("Members (${uiState.members.size})", style = BorshchevykTheme.typography.titleMedium, color = BorshchevykTheme.colors.onSurface)
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            items(uiState.members) { member ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(enabled = canManagePermissions && member.userId != uiState.currentUserId) {
                            memberForPermissions = member
                        }
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (member.userId == uiState.currentUserId) "You (${member.userId})" else "User: ${member.userId}",
                            style = BorshchevykTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                            color = BorshchevykTheme.colors.onSurface
                        )
                        Text(
                            text = "Role: ${member.role}",
                            style = BorshchevykTheme.typography.bodyMedium,
                            color = BorshchevykTheme.colors.onSurfaceVariant
                        )
                    }
                    if (canManagePermissions && member.userId != uiState.currentUserId) {
                         Icon(Icons.Default.Settings, contentDescription = "Edit Permissions", tint = BorshchevykTheme.colors.primary, modifier = Modifier.size(20.dp))
                    }
                }
                HorizontalDivider(color = BorshchevykTheme.colors.outline.copy(alpha = 0.5f))
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = { showClearHistoryDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = BorshchevykTheme.colors.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Clear History", color = BorshchevykTheme.colors.onError)
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (uiState.isGroupChat) {
                    Button(
                        onClick = onLeaveChat,
                        colors = ButtonDefaults.buttonColors(containerColor = BorshchevykTheme.colors.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Leave Chat", color = BorshchevykTheme.colors.onError)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (!uiState.isGroupChat || currentUserMember?.role == ChatMemberRole.OWNER) {
                    Button(
                        onClick = { showDeleteChatDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = BorshchevykTheme.colors.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Delete Chat", color = BorshchevykTheme.colors.onError)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        if (showClearHistoryDialog) {
            var forAll by remember { mutableStateOf(false) }
            AlertDialog(
                onDismissRequest = { showClearHistoryDialog = false },
                title = { Text("Clear History") },
                text = {
                    Column {
                        Text("Are you sure you want to clear the history of this chat? This action cannot be undone.")
                        if (!uiState.isGroupChat) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 16.dp)) {
                                Checkbox(checked = forAll, onCheckedChange = { forAll = it })
                                Text("Clear for everyone")
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onClearHistory(forAll)
                            showClearHistoryDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BorshchevykTheme.colors.error)
                    ) { Text("Clear", color = BorshchevykTheme.colors.onError) }
                },
                dismissButton = {
                    TextButton(onClick = { showClearHistoryDialog = false }) { Text("Cancel") }
                }
            )
        }

        if (showDeleteChatDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteChatDialog = false },
                title = { Text("Delete Chat") },
                text = { Text("Are you sure you want to delete this chat? This action cannot be undone.") },
                confirmButton = {
                    Button(
                        onClick = {
                            onDeleteChat()
                            showDeleteChatDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BorshchevykTheme.colors.error)
                    ) { Text("Delete", color = BorshchevykTheme.colors.onError) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteChatDialog = false }) { Text("Cancel") }
                }
            )
        }

        if (showInviteDialog) {
            var userIdToInvite by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showInviteDialog = false },
                containerColor = BorshchevykTheme.colors.surface,
                title = { Text("Invite User", color = BorshchevykTheme.colors.onSurface) },
                text = {
                    OutlinedTextField(
                        value = userIdToInvite,
                        onValueChange = { userIdToInvite = it },
                        label = { Text("User ID") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BorshchevykTheme.colors.primary,
                            unfocusedBorderColor = BorshchevykTheme.colors.outline
                        )
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onInvite(userIdToInvite)
                            showInviteDialog = false
                        },
                        enabled = userIdToInvite.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = BorshchevykTheme.colors.primary)
                    ) { Text("Invite") }
                },
                dismissButton = {
                    TextButton(onClick = { showInviteDialog = false }) { Text("Cancel") }
                }
            )
        }

        memberForPermissions?.let { member ->
            var canSend by remember { mutableStateOf(member.canSendMessages) }
            var canDelete by remember { mutableStateOf(member.canDeleteMessages) }
            var canInvite by remember { mutableStateOf(member.canInviteUsers) }
            var canChangeInfo by remember { mutableStateOf(member.canChangeInfo) }

            AlertDialog(
                onDismissRequest = { memberForPermissions = null },
                containerColor = BorshchevykTheme.colors.surface,
                title = { Text("Update Permissions for ${member.userId}", style = BorshchevykTheme.typography.titleMedium, color = BorshchevykTheme.colors.onSurface) },
                text = {
                    Column {
                        PermissionRow("Send Messages", canSend) { canSend = it }
                        PermissionRow("Delete Messages", canDelete) { canDelete = it }
                        PermissionRow("Invite Users", canInvite) { canInvite = it }
                        PermissionRow("Change Chat Info", canChangeInfo) { canChangeInfo = it }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onUpdatePermissions(
                                member.userId,
                                UpdatePermissionsRequest(
                                    canSendMessages = canSend,
                                    canDeleteMessages = canDelete,
                                    canInviteUsers = canInvite,
                                    canChangeInfo = canChangeInfo
                                )
                            )
                            memberForPermissions = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BorshchevykTheme.colors.primary)
                    ) { Text("Save") }
                },
                dismissButton = {
                    Row {
                        if (canManagePermissions && member.userId != uiState.currentUserId) {
                            TextButton(
                                onClick = {
                                    onKickUser(member.userId)
                                    memberForPermissions = null
                                },
                                colors = ButtonDefaults.textButtonColors(contentColor = BorshchevykTheme.colors.error)
                            ) { Text("Kick User") }
                        }
                        TextButton(onClick = { memberForPermissions = null }) { Text("Cancel") }
                    }
                }
            )
        }

        if (showUpdateInfoDialog) {
            var title by remember { mutableStateOf("") }
            var description by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { showUpdateInfoDialog = false },
                title = { Text("Update Chat Info") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Title") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Description") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onUpdateChatInfo(
                                title.ifBlank { null },
                                description.ifBlank { null }
                            )
                            showUpdateInfoDialog = false
                        }
                    ) { Text("Update") }
                },
                dismissButton = {
                    TextButton(onClick = { showUpdateInfoDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
fun PermissionRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = androidx.compose.material3.CheckboxDefaults.colors(checkedColor = BorshchevykTheme.colors.primary)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, style = BorshchevykTheme.typography.bodyLarge, color = BorshchevykTheme.colors.onSurface)
    }
}

@Composable
internal fun ChatScreen(
    uiState: ChatUiState,
    resolveAttachmentUrl: suspend (String) -> String?,
    onPinToggle: (Message) -> Unit,
    onReactionToggle: (String, String) -> Unit,
    onEdit: (Message) -> Unit,
    onDelete: (String, Boolean) -> Unit,
    onMessageVisible: (String) -> Unit,
    onLoadReaders: (String) -> Unit,
    onLoadComments: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var messageIdForReaders by remember { mutableStateOf<String?>(null) }
    var messageIdForComments by remember { mutableStateOf<String?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        if (uiState.pinnedMessages.isNotEmpty()) {
            PinnedMessagesBanner(
                messages = uiState.pinnedMessages,
                onUnpinClick = { msg -> onPinToggle(msg) }
            )
        }
        
        if (uiState.isLoading && uiState.messages.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BorshchevykTheme.colors.primary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                reverseLayout = true
            ) {
                val otherTypingUsers = uiState.typingUsers.filter { it != uiState.currentUserId }
                if (otherTypingUsers.isNotEmpty()) {
                    item {
                        Text(
                            text = if (otherTypingUsers.size == 1) "User is typing..." else "Multiple users are typing...",
                            style = BorshchevykTheme.typography.labelSmall,
                            color = BorshchevykTheme.colors.onSurfaceVariant,
                            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                        )
                    }
                }

                items(uiState.messages, key = { it.id }) { message ->
                    MessageBubble(
                        message = message,
                        isFromMe = message.authorId == uiState.currentUserId,
                        currentUserId = uiState.currentUserId,
                        resolveAttachmentUrl = resolveAttachmentUrl,
                        onPinToggle = { onPinToggle(message) },
                        onReactionToggle = { reaction -> onReactionToggle(message.id, reaction) },
                        onEdit = { onEdit(message) },
                        onDelete = { forAll -> onDelete(message.id, forAll) },
                        onMessageVisible = { onMessageVisible(message.id) },
                        onViewReaders = {
                            onLoadReaders(message.id)
                            messageIdForReaders = message.id
                        },
                        onViewComments = {
                            onLoadComments(message.id)
                            messageIdForComments = message.id
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }

    if (messageIdForReaders != null) {
        val readers = uiState.readersByMessageId[messageIdForReaders]
        AlertDialog(
            onDismissRequest = { messageIdForReaders = null },
            containerColor = BorshchevykTheme.colors.surface,
            title = { Text("Readers", color = BorshchevykTheme.colors.onSurface) },
            text = {
                if (readers == null) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = BorshchevykTheme.colors.primary)
                    }
                } else if (readers.isEmpty()) {
                    Text("No one has read this yet.", color = BorshchevykTheme.colors.onSurfaceVariant)
                } else {
                    LazyColumn {
                        items(readers) { readerId ->
                            Text(text = "User ID: $readerId", style = BorshchevykTheme.typography.bodyMedium, color = BorshchevykTheme.colors.onSurface, modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { messageIdForReaders = null }) { Text("Close") }
            }
        )
    }

    if (messageIdForComments != null) {
        val comments = uiState.commentsByMessageId[messageIdForComments]
        AlertDialog(
            onDismissRequest = { messageIdForComments = null },
            containerColor = BorshchevykTheme.colors.surface,
            title = { Text("Comments", color = BorshchevykTheme.colors.onSurface) },
            text = {
                if (comments == null) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = BorshchevykTheme.colors.primary)
                    }
                } else if (comments.isEmpty()) {
                    Text("No comments yet.", color = BorshchevykTheme.colors.onSurfaceVariant)
                } else {
                    LazyColumn {
                        items(comments) { comment ->
                            Column(Modifier.padding(vertical = 4.dp)) {
                                Text(text = comment.authorId, style = BorshchevykTheme.typography.labelSmall, color = BorshchevykTheme.colors.primary)
                                Text(text = comment.text, style = BorshchevykTheme.typography.bodyMedium, color = BorshchevykTheme.colors.onSurface)
                            }
                            HorizontalDivider(color = BorshchevykTheme.colors.outline.copy(alpha = 0.5f))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { messageIdForComments = null }) { Text("Close") }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun MessageBubble(
    message: Message,
    isFromMe: Boolean,
    currentUserId: String,
    resolveAttachmentUrl: suspend (String) -> String?,
    onPinToggle: () -> Unit,
    onReactionToggle: (String) -> Unit,
    onEdit: () -> Unit,
    onDelete: (Boolean) -> Unit,
    onMessageVisible: () -> Unit,
    onViewReaders: () -> Unit,
    onViewComments: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    LaunchedEffect(message.id) {
        if (!isFromMe) {
            onMessageVisible()
        }
    }
    
    val bubbleShape = if (isFromMe) {
        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 4.dp, bottomEnd = 20.dp)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isFromMe) Alignment.End else Alignment.Start
    ) {
        Box {
            Surface(
                color = if (isFromMe) BorshchevykTheme.colors.primary else BorshchevykTheme.colors.surfaceVariant,
                shape = bubbleShape,
                shadowElevation = 1.dp,
                modifier = Modifier.combinedClickable(
                    onClick = {},
                    onLongClick = { showMenu = true }
                )
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    if (message.attachmentIds.isNotEmpty()) {
                        message.attachmentIds.forEach { attachmentId ->
                            var url by remember { mutableStateOf<String?>(null) }
                            LaunchedEffect(attachmentId) {
                                url = resolveAttachmentUrl(attachmentId)
                            }
                            if (url != null) {
                                AsyncImage(
                                    model = url,
                                    contentDescription = "Attachment",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .padding(bottom = 8.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .padding(bottom = 8.dp)
                                        .background(BorshchevykTheme.colors.surfaceVariant, RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = if (isFromMe) BorshchevykTheme.colors.onPrimary else BorshchevykTheme.colors.primary)
                                }
                            }
                        }
                    }

                    if (message.text.isNotBlank()) {
                        Text(
                            text = message.text,
                            style = BorshchevykTheme.typography.bodyLarge,
                            color = if (isFromMe) BorshchevykTheme.colors.onPrimary else BorshchevykTheme.colors.onSurface
                        )
                    }
                }
            }
            
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                containerColor = BorshchevykTheme.colors.surface
            ) {
                DropdownMenuItem(
                    text = { Text(if (message.isPinned) "Unpin Message" else "Pin Message") },
                    onClick = {
                        showMenu = false
                        onPinToggle()
                    }
                )
                if (isFromMe) {
                    DropdownMenuItem(
                        text = { Text("Edit Message") },
                        onClick = {
                            showMenu = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete for Everyone") },
                        onClick = {
                            showMenu = false
                            onDelete(true)
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Delete for Me") },
                    onClick = {
                        showMenu = false
                        onDelete(false)
                    }
                )
                if (isFromMe) {
                    DropdownMenuItem(
                        text = { Text("View Readers") },
                        onClick = {
                            showMenu = false
                            onViewReaders()
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text("View Comments (${message.commentsCount})") },
                    onClick = {
                        showMenu = false
                        onViewComments()
                    }
                )

                HorizontalDivider(color = BorshchevykTheme.colors.outline.copy(alpha = 0.5f))
                val reactions = listOf("👍", "❤️", "😂", "😢", "🔥")
                Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                    reactions.forEach { emoji ->
                        Text(
                            text = emoji,
                            modifier = Modifier
                                .clickable {
                                    showMenu = false
                                    onReactionToggle(emoji)
                                }
                                .padding(8.dp),
                            fontSize = 20.sp
                        )
                    }
                }
            }
        }

        // Display Reactions
        if (message.reactions.isNotEmpty()) {
            val reactionCounts = message.reactions.groupBy { it.reaction }.mapValues { it.value.size }
            Row(
                modifier = Modifier.padding(top = 4.dp, start = if (isFromMe) 0.dp else 8.dp, end = if (isFromMe) 8.dp else 0.dp),
                horizontalArrangement = if (isFromMe) Arrangement.End else Arrangement.Start
            ) {
                reactionCounts.forEach { (emoji, count) ->
                    val iReacted = message.reactions.any { it.reaction == emoji && it.userId == currentUserId }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (iReacted) BorshchevykTheme.colors.primaryContainer else BorshchevykTheme.colors.surfaceVariant,
                        border = if (iReacted) androidx.compose.foundation.BorderStroke(1.dp, BorshchevykTheme.colors.primary) else null,
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .clickable { onReactionToggle(emoji) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = emoji, fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = count.toString(),
                                style = BorshchevykTheme.typography.labelSmall,
                                color = if (iReacted) BorshchevykTheme.colors.primary else BorshchevykTheme.colors.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun PinnedMessagesBanner(
    messages: List<Message>,
    onUnpinClick: (Message) -> Unit
) {
    val message = messages.lastOrNull() ?: return
    Surface(
        color = BorshchevykTheme.colors.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Star, contentDescription = "Pinned", tint = BorshchevykTheme.colors.primary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Pinned Message", style = BorshchevykTheme.typography.labelSmall, color = BorshchevykTheme.colors.primary)
                Text(text = message.text, style = BorshchevykTheme.typography.bodyMedium, color = BorshchevykTheme.colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

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
