package ru.kubsu.borshchevyk.feature.chat.settings.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import ru.kubsu.borshchevyk.core.model.domain.Message
import ru.kubsu.borshchevyk.core.ui.theme.BorshchevykTheme
import ru.kubsu.borshchevyk.feature.chat.common.model.MediaType
import ru.kubsu.borshchevyk.feature.chat.sharedmedia.mvi.ChatSharedMediaUiState

@Composable
internal fun ChatSharedMediaSection(
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
