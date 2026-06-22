package ru.kubsu.borshchevyk.feature.chat.chatlist.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.kubsu.borshchevyk.core.network.client.NetworkMode
import ru.kubsu.borshchevyk.core.network.mesh.MeshPeer
import ru.kubsu.borshchevyk.core.ui.theme.BorshchevykTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChatListTopAppBar(
    networkMode: NetworkMode,
    connectedPeersCount: Int,
    connectedPeers: List<MeshPeer>,
    onToggleNetworkMode: () -> Unit,
    onPeerSelected: (String) -> Unit,
    onSearchClick: () -> Unit,
    onJoinClick: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = "Chats",
                style = BorshchevykTheme.typography.titleLarge,
                color = BorshchevykTheme.colors.onSurface
            )
        },
        actions = {
            if (networkMode == NetworkMode.MESH) {
                var peersExpanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.align(Alignment.CenterVertically)) {
                    Text(
                        text = "$connectedPeersCount peers",
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
                        if (connectedPeers.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No peers connected") },
                                onClick = { peersExpanded = false }
                            )
                        } else {
                            connectedPeers.forEach { peer ->
                                DropdownMenuItem(
                                    text = { Text(peer.name) },
                                    onClick = {
                                        peersExpanded = false
                                        onPeerSelected(peer.name)
                                    }
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            Switch(
                checked = networkMode == NetworkMode.MESH,
                onCheckedChange = { onToggleNetworkMode() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = BorshchevykTheme.colors.primary,
                    checkedTrackColor = BorshchevykTheme.colors.primaryContainer,
                    uncheckedThumbColor = BorshchevykTheme.colors.onSurfaceVariant,
                    uncheckedTrackColor = BorshchevykTheme.colors.surfaceVariant
                ),
                thumbContent = {
                    Icon(
                        imageVector = if (networkMode == NetworkMode.GLOBAL) Icons.Default.Wifi else Icons.Default.WifiOff,
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
            IconButton(onClick = onJoinClick) {
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
