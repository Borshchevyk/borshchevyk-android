package ru.kubsu.borshchevyk.feature.contacts.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.kubsu.borshchevyk.core.ui.theme.BorshchevykTheme
import ru.kubsu.borshchevyk.feature.contacts.ContactsUiState
import ru.kubsu.borshchevyk.feature.contacts.ui.components.ContactItem

/**
 * The main UI screen for displaying the list of contacts.
 *
 * It handles the loading state, empty state, and populated list state. When contacts are present,
 * it displays them in a lazy list using [ContactItem].
 *
 * @param uiState The current state of the contacts UI, including the list of contacts and loading status.
 * @param onContactClick Callback invoked when a contact is selected.
 * @param onDeleteContact Callback invoked when the delete action is triggered for a contact.
 * @param modifier The modifier to be applied to the layout.
 */
@Composable
internal fun ContactsScreen(
    uiState: ContactsUiState,
    onContactClick: (String) -> Unit,
    onDeleteContact: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (uiState.isLoading && uiState.contacts.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = BorshchevykTheme.colors.primary)
        }
    } else if (!uiState.isLoading && uiState.contacts.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No contacts yet.", style = BorshchevykTheme.typography.bodyLarge, color = BorshchevykTheme.colors.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(uiState.contacts, key = { it.id }) { contact ->
                ContactItem(
                    contact = contact,
                    onClick = { onContactClick(contact.contactUserId) },
                    onDeleteClick = { onDeleteContact(contact.contactUserId) }
                )
            }
        }
    }
}