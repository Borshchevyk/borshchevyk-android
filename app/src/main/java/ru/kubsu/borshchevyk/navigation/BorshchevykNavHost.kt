package ru.kubsu.borshchevyk.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import ru.kubsu.borshchevyk.feature.auth.ui.AuthRoute
import ru.kubsu.borshchevyk.feature.call.ui.incoming.IncomingCallBanner
import ru.kubsu.borshchevyk.feature.chat.conversation.ui.ChatRoute
import ru.kubsu.borshchevyk.feature.call.ui.call.CallScreen as CallScreenRoute
import ru.kubsu.borshchevyk.feature.chat.settings.ui.ChatSettingsRoute as ChatSettingsScreenRoute
import ru.kubsu.borshchevyk.feature.profile.ui.editprivacy.EditPrivacyRoute as EditPrivacyScreenRoute
import ru.kubsu.borshchevyk.feature.profile.ui.editprofile.EditProfileRoute as EditProfileScreenRoute
import ru.kubsu.borshchevyk.feature.search.ui.SearchRoute as SearchScreenRoute

@Serializable
object AuthRoute

@Serializable
object HomeRoute

@Serializable
data class SearchRoute(val isInviteMode: Boolean = false)

@Serializable
object EditProfileRoute

@Serializable
object EditPrivacyRoute

@Serializable
data class ChatRoute(val chatId: String, val forwardPayloadJson: String? = null)

@Serializable
data class ChatSettingsRoute(val chatId: String)

@Serializable
data class CallRoute(val callId: String, val isInitiator: Boolean)

@Composable
fun BorshchevykNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = AuthRoute,
            modifier = modifier
        ) {
            composable<AuthRoute> {
                AuthRoute(
                    onAuthSuccess = {
                        navController.navigate(HomeRoute) {
                            popUpTo<AuthRoute> { inclusive = true }
                        }
                    }
                )
            }

            composable<HomeRoute> { backStackEntry ->
                HomeRoute(
                    navController = navController,
                    backStackEntry = backStackEntry,
                    onLogoutSuccess = {
                        navController.navigate(AuthRoute) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            composable<SearchRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<SearchRoute>()
                SearchScreenRoute(
                    onBackClick = { navController.popBackStack() },
                    onChatCreated = { chatId ->
                        navController.navigate(ChatRoute(chatId = chatId)) {
                            popUpTo<SearchRoute> { inclusive = true }
                        }
                    },
                    onUserSelected = if (route.isInviteMode) {
                        { userId ->
                            navController.previousBackStackEntry?.savedStateHandle?.set("selectedUserIdToInvite", userId)
                            navController.popBackStack()
                        }
                    } else null
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
                ChatRoute(
                    onBackClick = {
                        navController.popBackStack()
                    },
                    onSettingsClick = { chatId ->
                        navController.navigate(ChatSettingsRoute(chatId = chatId))
                    },
                    onNavigateToForwardSelection = { payloadJson ->
                        navController.previousBackStackEntry?.savedStateHandle?.set("forwardPayload", payloadJson)
                        navController.popBackStack()
                    },
                    onNavigateToCall = { callId ->
                        navController.navigate(CallRoute(callId = callId, isInitiator = true))
                    }
                )
            }

            composable<ChatSettingsRoute> {
                val currentBackStackEntry = navController.currentBackStackEntry
                val savedStateHandle = currentBackStackEntry?.savedStateHandle
                val selectedUserIdToInvite = savedStateHandle?.get<String>("selectedUserIdToInvite")

                ChatSettingsScreenRoute(
                    onBackClick = {
                        navController.popBackStack()
                    },
                    onChatDeletedLocally = {
                        navController.popBackStack(HomeRoute, inclusive = false)
                    },
                    onNavigateToInviteSearch = {
                        navController.navigate(SearchRoute(isInviteMode = true))
                    },
                    selectedUserIdToInvite = selectedUserIdToInvite,
                    onInviteConsumed = {
                        savedStateHandle?.remove<String>("selectedUserIdToInvite")
                    }
                )
            }

            composable<CallRoute> {
                CallScreenRoute(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }

        IncomingCallBanner(
            onNavigateToCall = { callId ->
                navController.navigate(CallRoute(callId = callId, isInitiator = false))
            }
        )
    }
}
