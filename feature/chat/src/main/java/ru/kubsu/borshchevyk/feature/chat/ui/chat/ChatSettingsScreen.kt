package ru.kubsu.borshchevyk.feature.chat.ui.chat

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import ru.kubsu.borshchevyk.core.model.domain.ChatMember
import ru.kubsu.borshchevyk.core.model.domain.ChatMemberRole
import ru.kubsu.borshchevyk.core.model.domain.Message
import ru.kubsu.borshchevyk.core.ui.theme.BorshchevykTheme
import ru.kubsu.borshchevyk.feature.chat.ChatSettingsUiState
import ru.kubsu.borshchevyk.feature.chat.ChatSharedMediaUiState
import ru.kubsu.borshchevyk.feature.chat.MediaType
import ru.kubsu.borshchevyk.feature.chat.ui.chat.components.AddContactDialog
import ru.kubsu.borshchevyk.feature.chat.ui.chat.components.ClearHistoryDialog
import ru.kubsu.borshchevyk.feature.chat.ui.chat.components.DeleteChatDialog
import ru.kubsu.borshchevyk.feature.chat.ui.chat.components.UpdateChatInfoDialog
import ru.kubsu.borshchevyk.feature.chat.ui.chat.components.UpdatePermissionsDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChatSettingsScreen(
    uiState: ChatSettingsUiState,
    sharedMediaUiState: ChatSharedMediaUiState,
    onTabSelected: (MediaType) -> Unit,
    onLoadNextPage: () -> Unit,
    onMessageClick: (Message) -> Unit,
    onResolveSharedMediaUrl: (String, Boolean) -> Unit,
    onBackClick: () -> Unit,
    onShowInviteSearch: () -> Unit,
    onGenerateLink: () -> Unit,
    onUpdatePermissions: (String, Boolean, Boolean, Boolean, Boolean) -> Unit,
    onClearHistory: (Boolean) -> Unit,
    onDeleteChat: () -> Unit,
    onKickUser: (String) -> Unit,
    onLeaveChat: () -> Unit,
    onUpdateChatInfo: (String?, String?) -> Unit,
    onAddContact: (String, String?) -> Unit,
    onRemoveContact: () -> Unit
) {
    var memberIdForPermissions by rememberSaveable { mutableStateOf<String?>(null) }
    var showClearHistoryDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteChatDialog by rememberSaveable { mutableStateOf(false) }
    var showUpdateInfoDialog by rememberSaveable { mutableStateOf(false) }
    var showAddContactDialog by rememberSaveable { mutableStateOf(false) }

    val currentUserMember = uiState.members.find { it.userId == uiState.currentUserId }
    val canManagePermissions = uiState.isGroupChat &&
            (currentUserMember?.role == ChatMemberRole.OWNER || currentUserMember?.role == ChatMemberRole.ADMIN)
    val canChangeInfo = uiState.isGroupChat && (currentUserMember?.canChangeInfo == true || canManagePermissions)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (canChangeInfo) {
                        IconButton(onClick = { showUpdateInfoDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Info")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = BorshchevykTheme.colors.surface
                )
            )
        },
        containerColor = BorshchevykTheme.colors.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item {
                ChatHeaderSection(uiState)
            }

            item {
                ChatActionsSection(
                    uiState = uiState,
                    onShowInvite = onShowInviteSearch,
                    onGenerateLink = onGenerateLink,
                    onShowAddContact = { showAddContactDialog = true },
                    onRemoveContact = onRemoveContact
                )
            }

            item {
                ChatSharedMediaSection(
                    uiState = sharedMediaUiState,
                    onTabSelected = onTabSelected,
                    onLoadNextPage = onLoadNextPage,
                    onMessageClick = onMessageClick,
                    onResolveUrl = onResolveSharedMediaUrl
                )
            }

            if (uiState.isGroupChat) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Members (${uiState.members.size})",
                            style = BorshchevykTheme.typography.titleMedium,
                            color = BorshchevykTheme.colors.primary
                        )
                        if (canManagePermissions) {
                            IconButton(onClick = onShowInviteSearch, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Add, contentDescription = "Add Member", tint = BorshchevykTheme.colors.primary)
                            }
                        }
                    }
                }

                items(uiState.members) { member ->
                    MemberItemCard(
                        member = member,
                        currentUserId = uiState.currentUserId,
                        canManagePermissions = canManagePermissions,
                        onMemberClick = { memberIdForPermissions = member.userId }
                    )
                }
            }

            item {
                DangerZoneSection(
                    uiState = uiState,
                    currentUserMember = currentUserMember,
                    onShowClearHistory = { showClearHistoryDialog = true },
                    onShowDeleteChat = { showDeleteChatDialog = true },
                    onLeaveChat = onLeaveChat
                )
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        // Dialogs
        if (showClearHistoryDialog) {
            ClearHistoryDialog(
                isGroupChat = uiState.isGroupChat,
                onDismiss = { showClearHistoryDialog = false },
                onConfirm = { forAll -> 
                    onClearHistory(forAll)
                    showClearHistoryDialog = false 
                }
            )
        }

        if (showDeleteChatDialog) {
            DeleteChatDialog(
                onDismiss = { showDeleteChatDialog = false },
                onConfirm = { 
                    onDeleteChat()
                    showDeleteChatDialog = false 
                }
            )
        }

        memberIdForPermissions?.let { memberId ->
            uiState.members.find { it.userId == memberId }?.let { member ->
                UpdatePermissionsDialog(
                    member = member,
                    currentUserId = uiState.currentUserId,
                    canManagePermissions = canManagePermissions,
                    onDismiss = { memberIdForPermissions = null },
                    onConfirm = { userId, canSend, canDelete, canInvite, canChange ->
                        onUpdatePermissions(userId, canSend, canDelete, canInvite, canChange)
                        memberIdForPermissions = null
                    },
                    onKickUser = { userId ->
                        onKickUser(userId)
                        memberIdForPermissions = null
                    }
                )
            } ?: run { memberIdForPermissions = null }
        }

        if (showUpdateInfoDialog) {
            UpdateChatInfoDialog(
                onDismiss = { showUpdateInfoDialog = false },
                onConfirm = { title, desc ->
                    onUpdateChatInfo(title, desc)
                    showUpdateInfoDialog = false
                }
            )
        }
        
        if (showAddContactDialog) {
            AddContactDialog(
                initialFirstName = uiState.partnerFirstName,
                initialLastName = uiState.partnerLastName,
                onDismiss = { showAddContactDialog = false },
                onConfirm = { firstName, lastName ->
                    onAddContact(firstName, lastName)
                    showAddContactDialog = false
                }
            )
        }
    }
}

@Composable
private fun ChatHeaderSection(uiState: ChatSettingsUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (uiState.chatAvatarUrl != null) {
                AsyncImage(
                    model = uiState.chatAvatarUrl,
                    contentDescription = "Chat Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                )
            } else {
                Surface(
                    modifier = Modifier.size(120.dp),
                    shape = CircleShape,
                    color = BorshchevykTheme.colors.primaryContainer
                ) {
                    Icon(
                        imageVector = if (uiState.isGroupChat) Icons.Default.Group else Icons.Default.Person,
                        contentDescription = "Default Avatar",
                        modifier = Modifier.padding(32.dp),
                        tint = BorshchevykTheme.colors.onPrimaryContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = uiState.chatName,
            style = BorshchevykTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = BorshchevykTheme.colors.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        val statusText = if (uiState.isGroupChat) "${uiState.members.size} members" else "@${uiState.partnerTag ?: uiState.partnerId?.take(8) ?: "unknown"}"
        Text(
            text = statusText,
            style = BorshchevykTheme.typography.bodyLarge,
            color = BorshchevykTheme.colors.onSurfaceVariant
        )

        if (!uiState.chatDescription.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = uiState.chatDescription,
                style = BorshchevykTheme.typography.bodyMedium,
                color = BorshchevykTheme.colors.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}

@Composable
private fun ChatActionsSection(
    uiState: ChatSettingsUiState,
    onShowInvite: () -> Unit,
    onGenerateLink: () -> Unit,
    onShowAddContact: () -> Unit,
    onRemoveContact: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        SectionHeader("Actions")
        Card(
            colors = CardDefaults.cardColors(containerColor = BorshchevykTheme.colors.surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column {
                if (!uiState.isGroupChat && uiState.partnerId != null) {
                    ActionItem(
                        icon = if (uiState.isContact) Icons.Default.PersonRemove else Icons.Default.PersonAdd,
                        title = if (uiState.isContact) "Remove from Contacts" else "Add to Contacts",
                        color = if (uiState.isContact) BorshchevykTheme.colors.error else BorshchevykTheme.colors.primary,
                        onClick = if (uiState.isContact) onRemoveContact else onShowAddContact
                    )
                }

                if (uiState.isGroupChat) {
                    ActionItem(
                        icon = Icons.Default.PersonAdd,
                        title = "Invite User",
                        onClick = onShowInvite
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = BorshchevykTheme.colors.outline)
                    ActionItem(
                        icon = Icons.Default.Add,
                        title = "Generate Invite Link",
                        onClick = onGenerateLink
                    )
                    
                    if (uiState.inviteLink != null) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = BorshchevykTheme.colors.outline)
                        ListItem(
                            headlineContent = { 
                                Text(
                                    "Link: \${uiState.inviteLink}", 
                                    maxLines = 1, 
                                    overflow = TextOverflow.Ellipsis,
                                    style = BorshchevykTheme.typography.bodyMedium
                                ) 
                            },
                            trailingContent = {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(20.dp))
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable {
                                clipboardManager.setText(AnnotatedString(uiState.inviteLink))
                                Toast.makeText(context, "Link copied to clipboard", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MemberItemCard(
    member: ChatMember,
    currentUserId: String,
    canManagePermissions: Boolean,
    onMemberClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = BorshchevykTheme.colors.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        val displayName = if (member.userId == currentUserId) "You" else member.user?.let { "\${it.firstName ?: \"\"} \${it.lastName ?: \"\"}".trim().ifBlank { it.tag } } ?: "Unknown User"
        
        ListItem(
            headlineContent = { Text(displayName, fontWeight = FontWeight.Medium) },
            supportingContent = {
                val user = member.user
                val tagDisplay = if (user != null && member.userId != currentUserId) user.tag?.let { "@$it • " } ?: "" else ""
                Text("${tagDisplay}Role: ${member.role}")
            },
            leadingContent = {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(BorshchevykTheme.colors.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    val user = member.user
                    if (user != null && user.avatarUrl != null) {
                        AsyncImage(
                            model = user.avatarUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(Icons.Default.Person, contentDescription = null, tint = BorshchevykTheme.colors.onPrimaryContainer)
                    }
                }
            },
            trailingContent = {
                if (canManagePermissions && member.userId != currentUserId) {
                    Icon(Icons.Default.Settings, contentDescription = "Manage", tint = BorshchevykTheme.colors.primary)
                }
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            modifier = Modifier.clickable(enabled = canManagePermissions && member.userId != currentUserId) {
                onMemberClick()
            }
        )
    }
}

@Composable
private fun DangerZoneSection(
    uiState: ChatSettingsUiState,
    currentUserMember: ChatMember?,
    onShowClearHistory: () -> Unit,
    onShowDeleteChat: () -> Unit,
    onLeaveChat: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp)) {
        SectionHeader("Chat Management", color = BorshchevykTheme.colors.error)
        Card(
            colors = CardDefaults.cardColors(containerColor = BorshchevykTheme.colors.surface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorshchevykTheme.colors.error.copy(alpha = 0.2f))
        ) {
            Column {
                ActionItem(
                    icon = Icons.Default.History,
                    title = "Clear History",
                    color = BorshchevykTheme.colors.error,
                    onClick = onShowClearHistory
                )
                
                if (uiState.isGroupChat) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = BorshchevykTheme.colors.outline)
                    ActionItem(
                        icon = Icons.Default.Logout,
                        title = "Leave Chat",
                        color = BorshchevykTheme.colors.error,
                        onClick = onLeaveChat
                    )
                }

                if (uiState.isDeletable && (!uiState.isGroupChat || currentUserMember?.role == ChatMemberRole.OWNER)) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = BorshchevykTheme.colors.outline)
                    ActionItem(
                        icon = Icons.Default.Delete,
                        title = "Delete Chat",
                        color = BorshchevykTheme.colors.error,
                        onClick = onShowDeleteChat
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, color: Color = BorshchevykTheme.colors.primary) {
    Text(
        text = title,
        style = BorshchevykTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
        color = color,
        modifier = Modifier.padding(start = 8.dp, bottom = 8.dp, top = 16.dp)
    )
}

@Composable
private fun ActionItem(
    icon: ImageVector,
    title: String,
    color: Color = BorshchevykTheme.colors.onSurface,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title, color = color) },
        leadingContent = { Icon(icon, contentDescription = null, tint = color) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable { onClick() }
    )
}


@Composable
fun ChatSharedMediaSection(
    uiState: ChatSharedMediaUiState,
    onTabSelected: (MediaType) -> Unit,
    onLoadNextPage: () -> Unit,
    onMessageClick: (Message) -> Unit,
    onResolveUrl: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp).fillMaxWidth()) {
        SectionHeader("Shared Media")
        Card(
            colors = CardDefaults.cardColors(containerColor = BorshchevykTheme.colors.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                ScrollableTabRow(
                    selectedTabIndex = uiState.selectedTab.ordinal,
                    containerColor = Color.Transparent,
                    contentColor = BorshchevykTheme.colors.onSurface,
                    edgePadding = 16.dp,
                    divider = {},
                    indicator = { tabPositions ->
                        if (uiState.selectedTab.ordinal < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[uiState.selectedTab.ordinal]),
                                color = BorshchevykTheme.colors.primary
                            )
                        }
                    }
                ) {
                    MediaType.entries.forEach { tab ->
                        Tab(
                            selected = uiState.selectedTab == tab,
                            onClick = { onTabSelected(tab) },
                            text = { Text(tab.displayName) },
                            selectedContentColor = BorshchevykTheme.colors.primary,
                            unselectedContentColor = BorshchevykTheme.colors.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (uiState.isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = BorshchevykTheme.colors.primary)
                    }
                } else if (uiState.currentItems.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        Text("No attachments found.", color = BorshchevykTheme.colors.onSurfaceVariant)
                    }
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(uiState.currentItems) { message ->
                            AttachmentItem(
                                message = message,
                                type = uiState.selectedTab,
                                attachmentUrls = uiState.attachmentUrls,
                                onResolveUrl = onResolveUrl,
                                onClick = { onMessageClick(message) }
                            )
                        }

                        item {
                            if (!uiState.isCurrentEndReached) {
                                LaunchedEffect(true) {
                                    onLoadNextPage()
                                }
                                Box(modifier = Modifier.size(100.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = BorshchevykTheme.colors.primary)
                                }
                            }
                        }
                    }
                }

                if (uiState.error != null) {
                    Text(
                        text = uiState.error,
                        color = BorshchevykTheme.colors.error,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AttachmentItem(
    message: Message,
    type: MediaType,
    attachmentUrls: Map<String, String>,
    onResolveUrl: (String, Boolean) -> Unit,
    onClick: () -> Unit
) {
    val attachment = message.attachments.firstOrNull()
    val idToResolve = attachment?.id
    val url = idToResolve?.let { attachmentUrls[it] } ?: ""

    if (idToResolve != null && url.isEmpty()) {
        LaunchedEffect(idToResolve) {
            onResolveUrl(idToResolve, type == MediaType.VIDEO || type == MediaType.CIRCLE)
        }
    }

    Box(
        modifier = Modifier
            .size(100.dp)
            .background(BorshchevykTheme.colors.surfaceVariant, shape = MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        when (type) {
            MediaType.PHOTO -> {
                AsyncImage(
                    model = url,
                    contentDescription = "Photo Attachment",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            MediaType.VIDEO -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    AsyncImage(
                        model = url,
                        contentDescription = "Video Thumbnail",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Play icon overlay
                    Surface(
                        shape = MaterialTheme.shapes.extraLarge,
                        color = BorshchevykTheme.colors.surface.copy(alpha = 0.7f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("▶", color = BorshchevykTheme.colors.onSurface)
                        }
                    }
                }
            }
            MediaType.CIRCLE -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    AsyncImage(
                        model = url,
                        contentDescription = "Circle Thumbnail",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Surface(
                        shape = MaterialTheme.shapes.extraLarge,
                        color = BorshchevykTheme.colors.surface.copy(alpha = 0.7f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("◎", color = BorshchevykTheme.colors.onSurface)
                        }
                    }
                }
            }
            MediaType.FILE -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "File",
                        style = BorshchevykTheme.typography.bodyMedium,
                        color = BorshchevykTheme.colors.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = attachment?.originalFilename ?: "Unknown",
                        style = BorshchevykTheme.typography.labelSmall,
                        color = BorshchevykTheme.colors.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            MediaType.VOICE -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "Voice",
                        style = BorshchevykTheme.typography.bodyMedium,
                        color = BorshchevykTheme.colors.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Voice Message",
                        style = BorshchevykTheme.typography.labelSmall,
                        color = BorshchevykTheme.colors.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}