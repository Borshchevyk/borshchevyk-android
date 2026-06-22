package ru.kubsu.borshchevyk.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import ru.kubsu.borshchevyk.core.ui.theme.BorshchevykTheme
import ru.kubsu.borshchevyk.feature.chat.chatlist.ui.ChatListRoute
import ru.kubsu.borshchevyk.feature.contacts.ui.ContactsRoute
import ru.kubsu.borshchevyk.feature.profile.ui.ProfileRoute

@Composable
fun HomeRoute(
    navController: NavHostController,
    backStackEntry: NavBackStackEntry,
    onLogoutSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedItem by rememberSaveable { mutableIntStateOf(0) }
    val items = listOf("Chats", "Contacts", "Profile")
    val selectedIcons = listOf(Icons.Filled.Chat, Icons.Filled.Contacts, Icons.Filled.Person)
    val unselectedIcons = listOf(Icons.Outlined.Chat, Icons.Outlined.Contacts, Icons.Outlined.Person)

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = BorshchevykTheme.colors.surface,
                contentColor = BorshchevykTheme.colors.onSurface
            ) {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                if (selectedItem == index) selectedIcons[index] else unselectedIcons[index],
                                contentDescription = item
                            )
                        },
                        label = { Text(item) },
                        selected = selectedItem == index,
                        onClick = { selectedItem = index },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BorshchevykTheme.colors.onPrimaryContainer,
                            selectedTextColor = BorshchevykTheme.colors.onSurface,
                            indicatorColor = BorshchevykTheme.colors.primaryContainer,
                            unselectedIconColor = BorshchevykTheme.colors.onSurfaceVariant,
                            unselectedTextColor = BorshchevykTheme.colors.onSurfaceVariant
                        )
                    )
                }
            }
        },
        containerColor = BorshchevykTheme.colors.background,
        modifier = modifier
    ) { innerPadding ->
        Box(modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())) {
            when (selectedItem) {
                0 -> {
                    // Chats
                    val savedStateHandle = backStackEntry.savedStateHandle
                    val forwardPayloadJson by savedStateHandle.getStateFlow<String?>("forwardPayload", null).collectAsStateWithLifecycle()

                    ChatListRoute(
                        forwardPayloadJson = forwardPayloadJson,
                        onCancelForward = {
                            savedStateHandle.set("forwardPayload", null)
                        },
                        onChatClick = { chatId ->
                            if (forwardPayloadJson != null) {
                                savedStateHandle?.remove<String>("forwardPayload")
                                navController.navigate(ChatRoute(chatId = chatId, forwardPayloadJson = forwardPayloadJson))
                            } else {
                                navController.navigate(ChatRoute(chatId = chatId))
                            }
                        },
                        onSearchClick = {
                            navController.navigate(SearchRoute())
                        }
                    )
                }
                1 -> {
                    // Contacts
                    ContactsRoute(
                        onNavigateToChat = { chatId ->
                            navController.navigate(ChatRoute(chatId = chatId))
                        }
                    )
                }
                2 -> {
                    // Profile
                    ProfileRoute(
                        onBackClick = { selectedItem = 0 }, // Back takes you to Chats
                        onNavigateToEditProfile = { navController.navigate(EditProfileRoute) },
                        onNavigateToEditPrivacy = { navController.navigate(EditPrivacyRoute) },
                        onLogoutSuccess = onLogoutSuccess
                    )
                }
            }
        }
    }
}