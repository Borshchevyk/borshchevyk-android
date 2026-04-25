package ru.kubsu.borshchevyk.feature.contacts.ui

import android.widget.Toast
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.kubsu.borshchevyk.core.ui.theme.BorshchevykTheme
import ru.kubsu.borshchevyk.feature.contacts.ContactsEffect
import ru.kubsu.borshchevyk.feature.contacts.ContactsIntent
import ru.kubsu.borshchevyk.feature.contacts.ContactsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsRoute(
    onNavigateToChat: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ContactsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ContactsEffect.ShowError -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is ContactsEffect.NavigateToChat -> {
                    onNavigateToChat(effect.chatId)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Contacts", 
                        style = BorshchevykTheme.typography.titleLarge,
                        color = BorshchevykTheme.colors.onSurface
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BorshchevykTheme.colors.background,
                    titleContentColor = BorshchevykTheme.colors.onSurface
                )
            )
        },
        containerColor = BorshchevykTheme.colors.background,
        modifier = modifier
    ) { padding ->
        ContactsScreen(
            uiState = uiState,
            onContactClick = { userId -> viewModel.handleIntent(ContactsIntent.ContactClicked(userId)) },
            onDeleteContact = { userId -> viewModel.handleIntent(ContactsIntent.RemoveContact(userId)) },
            modifier = Modifier.padding(padding)
        )
    }
}