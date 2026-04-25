package ru.kubsu.borshchevyk.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import ru.kubsu.borshchevyk.feature.auth.ui.AuthRoute
import ru.kubsu.borshchevyk.feature.chat.ui.chatlist.ChatListRoute
import ru.kubsu.borshchevyk.feature.profile.ui.ProfileRoute
import ru.kubsu.borshchevyk.feature.chat.ui.chat.ChatRoute as ChatScreenRoute
import ru.kubsu.borshchevyk.feature.chat.ui.chat.ChatSettingsRoute as ChatSettingsScreenRoute
import ru.kubsu.borshchevyk.feature.profile.ui.editprivacy.EditPrivacyRoute as EditPrivacyScreenRoute
import ru.kubsu.borshchevyk.feature.profile.ui.editprofile.EditProfileRoute as EditProfileScreenRoute
import ru.kubsu.borshchevyk.feature.search.ui.SearchRoute as SearchScreenRoute

@Serializable
object AuthRoute

@Serializable
object ChatListRoute

@Serializable
object SearchRoute

@Serializable
object ProfileRoute

@Serializable
object EditProfileRoute

@Serializable
object EditPrivacyRoute

@Serializable
data class ChatRoute(val chatId: String, val forwardPayloadJson: String? = null)

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
            val currentBackStackEntry = navController.currentBackStackEntry
            val savedStateHandle = currentBackStackEntry?.savedStateHandle
            val forwardPayloadJson = savedStateHandle?.get<String>("forwardPayload")

            ChatListRoute(
                forwardPayloadJson = forwardPayloadJson,
                onCancelForward = {
                    savedStateHandle?.remove<String>("forwardPayload")
                },
                onChatClick = { chatId ->
                    if (forwardPayloadJson != null) {
                        savedStateHandle?.remove<String>("forwardPayload")
                        navController.navigate(ChatRoute(chatId = chatId, forwardPayloadJson = forwardPayloadJson))
                    } else {
                        navController.navigate(ChatRoute(chatId = chatId))
                    }
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
            SearchScreenRoute(
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
                onNavigateToEditProfile = { navController.navigate(EditProfileRoute) },
                onNavigateToEditPrivacy = { navController.navigate(EditPrivacyRoute) },
                onLogoutSuccess = {
                    navController.navigate(AuthRoute) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable<EditProfileRoute> {
            EditProfileScreenRoute(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable<EditPrivacyRoute> {
            EditPrivacyScreenRoute(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable<ChatRoute> {
            ChatScreenRoute(
                onBackClick = {
                    navController.popBackStack()
                },
                onSettingsClick = { chatId ->
                    navController.navigate(ChatSettingsRoute(chatId = chatId))
                },
                onNavigateToForwardSelection = { payloadJson ->
                    navController.previousBackStackEntry?.savedStateHandle?.set("forwardPayload", payloadJson)
                    navController.popBackStack()
                }
            )
        }

        composable<ChatSettingsRoute> {
            ChatSettingsScreenRoute(
                onBackClick = {
                    navController.popBackStack()
                },
                onChatDeletedLocally = {
                    navController.popBackStack(ChatListRoute, inclusive = false)
                }
            )
        }
    }
}
