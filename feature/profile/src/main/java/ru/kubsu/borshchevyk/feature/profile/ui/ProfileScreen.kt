package ru.kubsu.borshchevyk.feature.profile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.kubsu.borshchevyk.core.ui.theme.BorshchevykTheme
import ru.kubsu.borshchevyk.feature.profile.ProfileIntent
import ru.kubsu.borshchevyk.feature.profile.ProfileUiState
import ru.kubsu.borshchevyk.feature.profile.ui.components.SettingsItem

@Composable
internal fun ProfileScreen(
    uiState: ProfileUiState,
    onIntent: (ProfileIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    if (uiState.isLoading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = BorshchevykTheme.colors.primary)
        }
        return
    }

    val user = uiState.user
    val displayName = user?.firstName?.let { "$it ${user.lastName ?: ""}".trim() } 
        ?: user?.tag 
        ?: "Unknown User"
        
    val initial = displayName.firstOrNull()?.uppercase() ?: "?"

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(BorshchevykTheme.colors.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                style = BorshchevykTheme.typography.titleLarge.copy(fontSize = 40.sp),
                color = BorshchevykTheme.colors.primary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = displayName,
            style = BorshchevykTheme.typography.titleLarge,
            color = BorshchevykTheme.colors.onSurface
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = "@${user?.tag ?: ""}",
            style = BorshchevykTheme.typography.bodyLarge,
            color = BorshchevykTheme.colors.primary
        )

        user?.bio?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = it,
                style = BorshchevykTheme.typography.bodyMedium,
                color = BorshchevykTheme.colors.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = BorshchevykTheme.colors.surface,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                SettingsItem(
                    icon = Icons.Default.Person,
                    title = "Account Details",
                    onClick = { /* TODO */ }
                )
                HorizontalDivider(color = BorshchevykTheme.colors.outline.copy(alpha = 0.5f))
                SettingsItem(
                    icon = Icons.Default.Lock,
                    title = "Privacy & Security",
                    onClick = { onIntent(ProfileIntent.OpenEditPrivacy) }
                )
                HorizontalDivider(color = BorshchevykTheme.colors.outline.copy(alpha = 0.5f))
                SettingsItem(
                    icon = Icons.Default.Notifications,
                    title = "Notifications",
                    onClick = { /* TODO */ }
                )
                HorizontalDivider(color = BorshchevykTheme.colors.outline.copy(alpha = 0.5f))
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "About Borshchevyk",
                    onClick = { /* TODO */ }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = BorshchevykTheme.colors.surface,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            SettingsItem(
                icon = Icons.AutoMirrored.Filled.ExitToApp,
                title = "Log Out",
                onClick = { onIntent(ProfileIntent.Logout) },
                isDestructive = true
            )
        }
    }
}
