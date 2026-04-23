package ru.kubsu.borshchevyk.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import ru.kubsu.borshchevyk.feature.auth.ui.AuthRoute
import ru.kubsu.borshchevyk.feature.chat.ui.chatlist.ChatListRoute
import ru.kubsu.borshchevyk.feature.profile.ProfileRoute
import ru.kubsu.borshchevyk.feature.search.ui.SearchRoute
import ru.kubsu.borshchevyk.feature.chat.ui.chat.ChatRoute as ChatScreenRoute

import ru.kubsu.borshchevyk.feature.chat.ui.chat.ChatSettingsRoute as ChatSettingsScreenRoute

@Serializable
object AuthRoute

@Serializable
object ChatListRoute

@Serializable
object SearchRoute

@Serializable
object ProfileRoute

@Serializable
data class ChatRoute(val chatId: String)

@Serializable
data class ChatSettingsRoute(val chatId: String)

@Composable
fun BorshchevykNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = AuthRoute,
        modifier = modifier
    ) {
        composable<AuthRoute> {
            AuthRoute(
                onAuthSuccess = {
                    navController.navigate(ChatListRoute) {
                        popUpTo<AuthRoute> { inclusive = true }
                    }
                }
            )
        }

        composable<ChatListRoute> {
            ChatListRoute(
                onChatClick = { chatId ->
                    navController.navigate(ChatRoute(chatId = chatId))
                },
                onProfileClick = {
                    navController.navigate(ProfileRoute)
                },
                onSearchClick = {
                    navController.navigate(SearchRoute)
                }
            )
        }

        composable<SearchRoute> {
            SearchRoute(
                onBackClick = { navController.popBackStack() },
                onChatCreated = { chatId ->
                    navController.navigate(ChatRoute(chatId = chatId)) {
                        popUpTo<SearchRoute> { inclusive = true }
                    }
                }
            )
        }

        composable<ProfileRoute> {
            ProfileRoute(
                onBackClick = { navController.popBackStack() },
                onLogoutSuccess = {
                    navController.navigate(AuthRoute) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable<ChatRoute> {
            ChatScreenRoute(
                onBackClick = {
                    navController.popBackStack()
                },
                onSettingsClick = { chatId ->
                    navController.navigate(ChatSettingsRoute(chatId = chatId))
                }
            )
        }

        composable<ChatSettingsRoute> {
            ChatSettingsScreenRoute(
                onBackClick = {
                    navController.popBackStack()
                },
                onChatDeletedLocally = {
                    // Navigate back to ChatListRoute
                    navController.popBackStack(ChatListRoute, inclusive = false)
                }
            )
        }
    }
}
