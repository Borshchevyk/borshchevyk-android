package ru.kubsu.borshchevyk.feature.chat.settings.ui.components

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import ru.kubsu.borshchevyk.core.ui.theme.BorshchevykTheme
import ru.kubsu.borshchevyk.feature.chat.settings.mvi.ChatSettingsIntent
import ru.kubsu.borshchevyk.feature.chat.settings.mvi.ChatSettingsUiState

@Composable
internal fun ChatHeaderSection(uiState: ChatSettingsUiState) {
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
                    if (uiState.isSavedMessages) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text(
                                text = "SM",
                                style = BorshchevykTheme.typography.titleLarge.copy(fontSize = 40.sp),
                                color = BorshchevykTheme.colors.onPrimaryContainer
                            )
                        }
                    } else {
                        Icon(
                            imageVector = if (uiState.isGroupChat) Icons.Default.Group else Icons.Default.Person,
                            contentDescription = "Default Avatar",
                            modifier = Modifier.padding(32.dp),
                            tint = BorshchevykTheme.colors.onPrimaryContainer
                        )
                    }
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

        val statusText = if (uiState.isGroupChat) "${uiState.members.size} members" else uiState.partnerTag ?: uiState.partnerId?.let { "@${it.take(8)}" } ?: "@unknown"
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
internal fun ChatActionsSection(
    uiState: ChatSettingsUiState,
    onIntent: (ChatSettingsIntent) -> Unit,
    onShowInvite: () -> Unit,
    onShowAddContact: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        if (!uiState.isGroupChat && uiState.partnerId != null) {
            SectionHeader("Contact Actions")
            Card(
                colors = CardDefaults.cardColors(containerColor = BorshchevykTheme.colors.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                ActionItem(
                    icon = if (uiState.isContact) Icons.Default.PersonRemove else Icons.Default.PersonAdd,
                    title = if (uiState.isContact) "Remove from Contacts" else "Add to Contacts",
                    color = if (uiState.isContact) BorshchevykTheme.colors.error else BorshchevykTheme.colors.primary,
                    onClick = if (uiState.isContact) {
                        { onIntent(ChatSettingsIntent.RemoveContact) }
                    } else onShowAddContact
                )
            }
        } else {
            Log.d("MINE", "${uiState.partnerId}")
        }

        if (uiState.isGroupChat) {
            SectionHeader("Group Actions")
            Card(
                colors = CardDefaults.cardColors(containerColor = BorshchevykTheme.colors.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column {
                    ActionItem(
                        icon = Icons.Default.PersonAdd,
                        title = "Invite User",
                        onClick = onShowInvite
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = BorshchevykTheme.colors.outline
                    )
                    ActionItem(
                        icon = Icons.Default.Add,
                        title = "Generate Invite Link",
                        onClick = { onIntent(ChatSettingsIntent.GenerateInviteLink) }
                    )

                    if (uiState.inviteLink != null) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = BorshchevykTheme.colors.outline
                        )
                        ListItem(
                            headlineContent = {
                                Text(
                                    "Link: ${uiState.inviteLink}",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = BorshchevykTheme.typography.bodyMedium
                                )
                            },
                            trailingContent = {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = "Copy",
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable {
                                clipboardManager.setText(AnnotatedString(uiState.inviteLink))
                                Toast.makeText(context, "Link copied to clipboard", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun MemberItemCard(
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
        val displayName = if (member.userId == currentUserId) "You" else member.user?.let { "${it.firstName ?: ""} ${it.lastName ?: ""}".trim().ifBlank { it.tag } } ?: "Unknown User"
        
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
internal fun DangerZoneSection(
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
internal fun SectionHeader(title: String, color: Color = BorshchevykTheme.colors.primary) {
    Text(
        text = title,
        style = BorshchevykTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
        color = color,
        modifier = Modifier.padding(start = 8.dp, bottom = 8.dp, top = 16.dp)
    )
}

@Composable
internal fun ActionItem(
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
