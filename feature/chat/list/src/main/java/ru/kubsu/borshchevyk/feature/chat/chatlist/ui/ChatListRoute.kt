package ru.kubsu.borshchevyk.feature.chat.chatlist.ui

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.kubsu.borshchevyk.core.network.client.NetworkMode
import ru.kubsu.borshchevyk.core.ui.theme.BorshchevykTheme
import ru.kubsu.borshchevyk.feature.chat.chatlist.ChatListViewModel
import ru.kubsu.borshchevyk.feature.chat.chatlist.mvi.ChatListEffect
import ru.kubsu.borshchevyk.feature.chat.chatlist.mvi.ChatListIntent
import ru.kubsu.borshchevyk.feature.chat.chatlist.ui.components.ChatListTopAppBar
import ru.kubsu.borshchevyk.feature.chat.chatlist.ui.components.CreateGroupChatDialog
import ru.kubsu.borshchevyk.feature.chat.chatlist.ui.components.ForwardBanner
import ru.kubsu.borshchevyk.feature.chat.chatlist.ui.components.JoinChatDialog

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

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ChatListEffect.ShowError -> Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                is ChatListEffect.NavigateToChat -> onChatClick(effect.chatId)
            }
        }
    }

    val meshPermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            viewModel.handleIntent(ChatListIntent.ToggleNetworkMode)
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
                    ForwardBanner(onCancelForward = onCancelForward)
                }
                ChatListTopAppBar(
                    networkMode = uiState.networkMode,
                    connectedPeersCount = uiState.connectedPeersCount,
                    connectedPeers = uiState.connectedPeers,
                    onToggleNetworkMode = {
                        if (uiState.networkMode == NetworkMode.GLOBAL) {
                            meshPermissionsLauncher.launch(requiredMeshPermissions)
                        } else {
                            viewModel.handleIntent(ChatListIntent.ToggleNetworkMode)
                        }
                    },
                    onPeerSelected = { peerName ->
                        viewModel.handleIntent(ChatListIntent.CreatePrivateChatByTag(peerName))
                    },
                    onSearchClick = onSearchClick,
                    onJoinClick = { showJoinDialog = true }
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
            onIntent = viewModel::handleIntent,
            modifier = Modifier.padding(padding)
        )

        if (showCreateGroupDialog) {
            CreateGroupChatDialog(
                onDismiss = { showCreateGroupDialog = false },
                onCreate = { title, desc ->
                    showCreateGroupDialog = false
                    viewModel.handleIntent(ChatListIntent.CreateGroupChat(title, desc))
                }
            )
        }

        if (showJoinDialog) {
            JoinChatDialog(
                onDismiss = { showJoinDialog = false },
                onJoin = { link ->
                    showJoinDialog = false
                    viewModel.handleIntent(ChatListIntent.JoinChat(link))
                }
            )
        }
    }
}
