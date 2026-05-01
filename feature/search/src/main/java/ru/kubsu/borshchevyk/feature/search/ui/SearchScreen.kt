package ru.kubsu.borshchevyk.feature.search.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.kubsu.borshchevyk.core.model.domain.Chat
import ru.kubsu.borshchevyk.core.model.domain.User
import ru.kubsu.borshchevyk.core.ui.theme.BorshchevykTheme
import ru.kubsu.borshchevyk.feature.search.ui.components.UserSearchItem

/**
 * Composable that displays the main content of the Search screen.
 * It handles the rendering of search results for both users and public chats,
 * along with loading and error states.
 *
 * @param isLoading Whether a search operation is currently running.
 * @param userResults The list of users to display.
 * @param chatResults The list of public chats to display.
 * @param onUserClick Callback triggered when a user item is clicked. Passes the user ID.
 * @param onChatClick Callback triggered when a chat item is clicked. Passes the chat ID.
 * @param modifier The modifier to be applied to the root layout.
 * @param error An optional error message to display.
 */
@Composable
internal fun SearchScreen(
    isLoading: Boolean,
    userResults: List<User>,
    chatResults: List<Chat>,
    onUserClick: (String) -> Unit,
    onChatClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (userResults.isEmpty() && chatResults.isEmpty()) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = BorshchevykTheme.colors.primary
                    )
                }
                error != null -> {
                    SearchInfoMessage(
                        title = "Search Error",
                        message = error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    SearchInfoMessage(
                        title = "No results",
                        message = "Try searching for users or public chats.",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                if (chatResults.isNotEmpty()) {
                    item(key = "header_chats") {
                        SearchHeader(text = "Global Chats")
                    }
                    items(chatResults, key = { "c_${it.id}" }) { chat ->
                        UserSearchItem(
                            user = User(
                                userId = chat.id,
                                tag = "Public Group",
                                firstName = chat.title,
                                lastName = null,
                                avatarUrl = chat.partnerAvatarUrl // Use partnerAvatarUrl as fallback for now
                            ),
                            onClick = { onChatClick(chat.id) }
                        )
                    }
                }

                if (userResults.isNotEmpty()) {
                    item(key = "header_users") {
                        SearchHeader(text = "Users")
                    }
                    items(userResults, key = { "u_${it.userId}" }) { user ->
                        UserSearchItem(
                            user = user,
                            onClick = { onUserClick(user.userId) }
                        )
                    }
                }
            }
            
            if (isLoading) {
                androidx.compose.material3.LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                    color = BorshchevykTheme.colors.primary
                )
            }
        }
    }
}

/**
 * Displays a section header within the search results list.
 *
 * @param text The title text to display in the header.
 */
@Composable
private fun SearchHeader(text: String) {
    Text(
        text = text,
        style = BorshchevykTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = BorshchevykTheme.colors.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

/**
 * A reusable component to display informational text or errors in the center of the screen.
 *
 * @param title The main headline text.
 * @param message The secondary descriptive text.
 * @param modifier The modifier for layout positioning.
 */
@Composable
private fun SearchInfoMessage(
    title: String,
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = BorshchevykTheme.colors.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = BorshchevykTheme.typography.bodyMedium,
            color = BorshchevykTheme.colors.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
