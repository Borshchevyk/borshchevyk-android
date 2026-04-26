package ru.kubsu.borshchevyk.feature.search.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.kubsu.borshchevyk.core.model.domain.Chat
import ru.kubsu.borshchevyk.core.model.domain.User
import ru.kubsu.borshchevyk.core.ui.theme.BorshchevykTheme
import ru.kubsu.borshchevyk.feature.search.ui.components.UserSearchItem

@Composable
internal fun SearchScreen(
    isLoading: Boolean,
    userResults: List<User>,
    chatResults: List<Chat>,
    onUserClick: (String) -> Unit,
    onChatClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (isLoading && userResults.isEmpty() && chatResults.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = BorshchevykTheme.colors.primary)
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            if (chatResults.isNotEmpty()) {
                item {
                    Text(
                        text = "Global Chats",
                        style = BorshchevykTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = BorshchevykTheme.colors.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                items(chatResults, key = { it.id }) { chat ->
                    // Reuse UserSearchItem or create a simple ChatSearchItem. Let's map it to UserSearchItem style for now, 
                    // or better, create a simple row. Actually, we can use UserSearchItem by mapping chat to user-like.
                    UserSearchItem(
                        user = User(userId = chat.id, tag = "Public Group", firstName = chat.title, lastName = null),
                        onClick = { onChatClick(chat.id) }
                    )
                }
            }

            if (userResults.isNotEmpty()) {
                item {
                    Text(
                        text = "Users",
                        style = BorshchevykTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = BorshchevykTheme.colors.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                items(userResults, key = { "u_" + it.userId }) { user ->
                    UserSearchItem(
                        user = user,
                        onClick = { onUserClick(user.userId) }
                    )
                }
            }
            
            if (!isLoading && userResults.isEmpty() && chatResults.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No results found.", color = BorshchevykTheme.colors.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
