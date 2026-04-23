package ru.kubsu.borshchevyk.feature.search.ui

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.kubsu.borshchevyk.core.ui.theme.BorshchevykTheme
import ru.kubsu.borshchevyk.feature.search.SearchEffect
import ru.kubsu.borshchevyk.feature.search.SearchIntent
import ru.kubsu.borshchevyk.feature.search.SearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchRoute(
    onBackClick: () -> Unit,
    onChatCreated: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is SearchEffect.ShowError -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is SearchEffect.NavigateToChat -> {
                    onChatCreated(effect.chatId)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = uiState.query,
                        onValueChange = { viewModel.handleIntent(SearchIntent.UpdateQuery(it)) },
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
            isLoading = uiState.isLoading,
            results = uiState.results,
            onUserClick = { userId ->
                viewModel.handleIntent(SearchIntent.CreateChat(userId))
            },
            modifier = Modifier.padding(padding)
        )
    }
}
