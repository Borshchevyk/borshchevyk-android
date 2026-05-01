package ru.kubsu.borshchevyk.feature.profile.ui.editprivacy

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.kubsu.borshchevyk.core.ui.theme.BorshchevykTheme
import ru.kubsu.borshchevyk.feature.profile.ProfileIntent
import ru.kubsu.borshchevyk.feature.profile.ProfileUiState
import ru.kubsu.borshchevyk.feature.profile.ui.components.PrivacyOption

/**
 * Displays the edit privacy screen, allowing the user to configure privacy settings
 * such as email visibility, searchability, profile photo visibility, and chat invitations.
 *
 * @param uiState The current state of the profile UI, containing privacy settings and loading status.
 * @param onIntent Callback for user actions to update specific privacy settings.
 * @param modifier The modifier to be applied to the layout.
 */
@Composable
internal fun EditPrivacyScreen(
    uiState: ProfileUiState,
    onIntent: (ProfileIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    if (uiState.isLoading || uiState.privacySettings == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = BorshchevykTheme.colors.primary)
        }
        return
    }

    val settings = uiState.privacySettings

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        PrivacyOption(
            title = "Who can see my email",
            currentValue = settings.emailVisibility,
            onValueChange = { onIntent(ProfileIntent.UpdatePrivacy(emailVisibility = it)) }
        )
        Spacer(modifier = Modifier.height(24.dp))
        PrivacyOption(
            title = "Who can search me by email",
            currentValue = settings.searchByEmailVisibility,
            onValueChange = { onIntent(ProfileIntent.UpdatePrivacy(searchByEmailVisibility = it)) }
        )
        Spacer(modifier = Modifier.height(24.dp))
        PrivacyOption(
            title = "Who can see my profile photo",
            currentValue = settings.profilePhotoVisibility,
            onValueChange = { onIntent(ProfileIntent.UpdatePrivacy(profilePhotoVisibility = it)) }
        )
        Spacer(modifier = Modifier.height(24.dp))
        PrivacyOption(
            title = "Who can invite me to chats",
            currentValue = settings.inviteToChatVisibility,
            onValueChange = { onIntent(ProfileIntent.UpdatePrivacy(inviteToChatVisibility = it)) }
        )
    }
}
