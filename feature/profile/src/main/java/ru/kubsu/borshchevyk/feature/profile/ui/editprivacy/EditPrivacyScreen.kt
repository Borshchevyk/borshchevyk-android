package ru.kubsu.borshchevyk.feature.profile.ui.editprivacy

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.kubsu.borshchevyk.core.model.domain.Visibility
import ru.kubsu.borshchevyk.feature.profile.ProfileUiState
import ru.kubsu.borshchevyk.feature.profile.ui.components.PrivacyOption

@Composable
internal fun EditPrivacyScreen(
    uiState: ProfileUiState,
    onUpdate: (Visibility?, Visibility?, Visibility?, Visibility?) -> Unit,
    modifier: Modifier = Modifier
) {
    val settings = uiState.privacySettings ?: return

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        PrivacyOption(
            title = "Who can see my email",
            currentValue = settings.emailVisibility,
            onValueChange = { onUpdate(it, null, null, null) }
        )
        Spacer(modifier = Modifier.height(24.dp))
        PrivacyOption(
            title = "Who can search me by email",
            currentValue = settings.searchByEmailVisibility,
            onValueChange = { onUpdate(null, it, null, null) }
        )
        Spacer(modifier = Modifier.height(24.dp))
        PrivacyOption(
            title = "Who can see my profile photo",
            currentValue = settings.profilePhotoVisibility,
            onValueChange = { onUpdate(null, null, it, null) }
        )
        Spacer(modifier = Modifier.height(24.dp))
        PrivacyOption(
            title = "Who can invite me to chats",
            currentValue = settings.inviteToChatVisibility,
            onValueChange = { onUpdate(null, null, null, it) }
        )
    }
}
