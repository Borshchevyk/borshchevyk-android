package ru.kubsu.borshchevyk.feature.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.kubsu.borshchevyk.core.model.domain.User
import ru.kubsu.borshchevyk.core.ui.theme.BorshchevykTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchRoute(
    onBackClick: () -> Unit,
    onChatCreated: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = uiState.query,
                        onValueChange = viewModel::onQueryChange,
                        placeholder = { Text("Search users...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 16.dp),
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BorshchevykTheme.colors.primary,
                            unfocusedBorderColor = BorshchevykTheme.colors.outline,
                            focusedContainerColor = BorshchevykTheme.colors.surfaceVariant,
                            unfocusedContainerColor = BorshchevykTheme.colors.surfaceVariant,
                            focusedTextColor = BorshchevykTheme.colors.onSurface,
                            unfocusedTextColor = BorshchevykTheme.colors.onSurface,
                            cursorColor = BorshchevykTheme.colors.primary
                        ),
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BorshchevykTheme.colors.background
                )
            )
        },
        containerColor = BorshchevykTheme.colors.background,
        modifier = modifier
    ) { padding ->
        SearchScreen(
            uiState = uiState,
            onUserClick = { userId ->
                viewModel.onCreateChat(userId, onChatCreated)
            },
            modifier = Modifier.padding(padding)
        )
    }
}

@Composable
internal fun SearchScreen(
    uiState: SearchUiState,
    onUserClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (uiState.isLoading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = BorshchevykTheme.colors.primary)
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(uiState.results, key = { it.userId }) { user ->
                UserSearchItem(
                    user = user,
                    onClick = { onUserClick(user.userId) }
                )
            }
        }
    }
}

@Composable
internal fun UserSearchItem(
    user: User,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val initial = user.tag.firstOrNull()?.uppercase() ?: "?"
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(BorshchevykTheme.colors.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                style = BorshchevykTheme.typography.titleMedium,
                color = BorshchevykTheme.colors.primary
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = user.firstName ?: user.tag,
                style = BorshchevykTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = BorshchevykTheme.colors.onSurface
            )
            Text(
                text = "@${user.tag}",
                style = BorshchevykTheme.typography.bodyMedium,
                color = BorshchevykTheme.colors.onSurfaceVariant
            )
        }
    }
}
