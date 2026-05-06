package ru.kubsu.borshchevyk.feature.chat.ui.chatlist

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.kubsu.borshchevyk.core.network.client.NetworkMode
import ru.kubsu.borshchevyk.core.ui.theme.BorshchevykTheme
import ru.kubsu.borshchevyk.feature.chat.ChatListViewModel
import ru.kubsu.borshchevyk.feature.chat.ui.chatlist.components.CreateGroupChatDialog
import ru.kubsu.borshchevyk.feature.chat.ui.chatlist.components.JoinChatDialog

/**
 * Route for the Chat List screen. Handles ViewModel interaction and navigation events.
 *
 * @param forwardPayloadJson JSON representation of the payload to forward, if applicable.
 * @param onCancelForward Callback invoked when forwarding is canceled.
 * @param onChatClick Callback invoked when a chat is selected.
 * @param onSearchClick Callback invoked when the search action is triggered.
 * @param modifier The modifier to be applied to the layout.
 * @param viewModel The view model managing the state for the chat list screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListRoute(
    forwardPayloadJson: String? = null,
    onCancelForward: () -> Unit = {},
    onChatClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChatListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showCreateGroupDialog by rememberSaveable { mutableStateOf(false) }
    var showJoinDialog by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    val meshPermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            viewModel.toggleNetworkMode()
        } else {
            Toast.makeText(context, "Mesh permissions denied", Toast.LENGTH_SHORT).show()
        }
    }

    val requiredMeshPermissions = mutableListOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_WIFI_STATE,
        Manifest.permission.CHANGE_WIFI_STATE
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(Manifest.permission.BLUETOOTH_ADVERTISE)
            add(Manifest.permission.BLUETOOTH_CONNECT)
            add(Manifest.permission.BLUETOOTH_SCAN)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }
    }.toTypedArray()

    Scaffold(
        topBar = {
            Column {
                if (forwardPayloadJson != null) {
                    androidx.compose.material3.Surface(
                        color = BorshchevykTheme.colors.primaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Select chat to forward message",
                                style = BorshchevykTheme.typography.bodyMedium,
                                color = BorshchevykTheme.colors.onPrimaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = onCancelForward) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel forwarding", tint = BorshchevykTheme.colors.onPrimaryContainer)
                            }
                        }
                    }
                }
                TopAppBar(
                    title = { 
                        Text(
                            text = "Chats", 
                            style = BorshchevykTheme.typography.titleLarge,
                            color = BorshchevykTheme.colors.onSurface
                        ) 
                    },
                    actions = {
                        if (uiState.networkMode == NetworkMode.MESH) {
                            var peersExpanded by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.align(Alignment.CenterVertically)) {
                                Text(
                                    text = "${uiState.connectedPeersCount} peers",
                                    style = BorshchevykTheme.typography.bodyMedium,
                                    color = BorshchevykTheme.colors.primary,
                                    modifier = Modifier
                                        .clickable { peersExpanded = true }
                                        .padding(horizontal = 4.dp, vertical = 8.dp)
                                )
                                DropdownMenu(
                                    expanded = peersExpanded,
                                    onDismissRequest = { peersExpanded = false }
                                ) {
                                    if (uiState.connectedPeers.isEmpty()) {
                                        DropdownMenuItem(
                                            text = { Text("No peers connected") },
                                            onClick = { peersExpanded = false }
                                        )
                                    } else {
                                        uiState.connectedPeers.forEach { peer ->
                                            DropdownMenuItem(
                                                text = { Text(peer.name) },
                                                onClick = { 
                                                    peersExpanded = false
                                                    viewModel.onCreatePrivateChatByTag(peer.name) { chatId ->
                                                        onChatClick(chatId)
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Switch(
                            checked = uiState.networkMode == NetworkMode.MESH,
                            onCheckedChange = { 
                                if (uiState.networkMode == NetworkMode.GLOBAL) {
                                    // Switching to MESH, request permissions first
                                    meshPermissionsLauncher.launch(requiredMeshPermissions)
                                } else {
                                    // Switching to GLOBAL, just toggle
                                    viewModel.toggleNetworkMode()
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = BorshchevykTheme.colors.primary,
                                checkedTrackColor = BorshchevykTheme.colors.primaryContainer,
                                uncheckedThumbColor = BorshchevykTheme.colors.onSurfaceVariant,
                                uncheckedTrackColor = BorshchevykTheme.colors.surfaceVariant
                            ),
                            thumbContent = {
                                Icon(
                                    imageVector = if (uiState.networkMode == NetworkMode.GLOBAL) Icons.Default.Wifi else Icons.Default.WifiOff,
                                    contentDescription = "Network Mode",
                                    modifier = Modifier.padding(2.dp)
                                )
                            },
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        IconButton(onClick = onSearchClick) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Search",
                                tint = BorshchevykTheme.colors.onSurface
                            )
                        }
                        IconButton(onClick = { showJoinDialog = true }) {
                            Icon(
                                Icons.Default.Link,
                                contentDescription = "Join by Link",
                                tint = BorshchevykTheme.colors.onSurface
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = BorshchevykTheme.colors.background,
                        titleContentColor = BorshchevykTheme.colors.onSurface
                    )
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateGroupDialog = true },
                containerColor = BorshchevykTheme.colors.primary,
                contentColor = BorshchevykTheme.colors.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.GroupAdd, contentDescription = "New Group")
            }
        },
        containerColor = BorshchevykTheme.colors.background,
        modifier = modifier
    ) { padding ->
        ChatListScreen(
            uiState = uiState,
            onChatClick = onChatClick,
            onPinChat = viewModel::onPinChat,
            onUnpinChat = viewModel::onUnpinChat,
            modifier = Modifier.padding(padding)
        )

        if (showCreateGroupDialog) {
            CreateGroupChatDialog(
                onDismiss = { showCreateGroupDialog = false },
                onCreate = { title, desc ->
                    showCreateGroupDialog = false
                    viewModel.onCreateGroupChat(title, desc) { newChatId ->
                        onChatClick(newChatId)
                    }
                }
            )
        }

        if (showJoinDialog) {
            JoinChatDialog(
                onDismiss = { showJoinDialog = false },
                onJoin = { link ->
                    showJoinDialog = false
                    viewModel.onJoinChat(link) { newChatId ->
                        onChatClick(newChatId)
                    }
                }
            )
        }
    }
}
