package ru.kubsu.borshchevyk.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.kubsu.borshchevyk.core.model.domain.Visibility
import ru.kubsu.borshchevyk.core.model.dto.UpdatePrivacySettingsRequest
import ru.kubsu.borshchevyk.core.ui.theme.BorshchevykTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileRoute(
    onBackClick: () -> Unit,
    onLogoutSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var isEditingProfile by remember { mutableStateOf(false) }
    var isEditingPrivacy by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        if (isEditingProfile) "Edit Profile" else if (isEditingPrivacy) "Privacy" else "Profile",
                        style = BorshchevykTheme.typography.titleMedium,
                        color = BorshchevykTheme.colors.onSurface
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isEditingProfile) isEditingProfile = false
                        else if (isEditingPrivacy) isEditingPrivacy = false
                        else onBackClick()
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "Back",
                            tint = BorshchevykTheme.colors.onSurface
                        )
                    }
                },
                actions = {
                    if (!isEditingProfile && !isEditingPrivacy) {
                        IconButton(onClick = { isEditingProfile = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = BorshchevykTheme.colors.primary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BorshchevykTheme.colors.background
                )
            )
        },
        containerColor = BorshchevykTheme.colors.background,
        modifier = modifier
    ) { padding ->
        val screenModifier = Modifier.padding(padding)
        when {
            isEditingProfile -> EditProfileScreen(
                uiState = uiState,
                onSave = { f, l, b -> 
                    viewModel.onUpdateProfile(f, l, b)
                    isEditingProfile = false
                },
                modifier = screenModifier
            )
            isEditingPrivacy -> EditPrivacyScreen(
                uiState = uiState,
                onUpdate = viewModel::onUpdatePrivacy,
                modifier = screenModifier
            )
            else -> ProfileScreen(
                uiState = uiState,
                onLogout = { viewModel.onLogout(onLogoutSuccess) },
                onPrivacyClick = { isEditingPrivacy = true },
                modifier = screenModifier
            )
        }
    }
}

@Composable
internal fun ProfileScreen(
    uiState: ProfileUiState,
    onLogout: () -> Unit,
    onPrivacyClick: () -> Unit,
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
                    onClick = onPrivacyClick
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
                onClick = onLogout,
                isDestructive = true
            )
        }
    }
}

@Composable
fun EditProfileScreen(
    uiState: ProfileUiState,
    onSave: (String, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var firstName by remember { mutableStateOf(uiState.user?.firstName ?: "") }
    var lastName by remember { mutableStateOf(uiState.user?.lastName ?: "") }
    var bio by remember { mutableStateOf(uiState.user?.bio ?: "") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        OutlinedTextField(
            value = firstName,
            onValueChange = { firstName = it },
            label = { Text("First Name") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = lastName,
            onValueChange = { lastName = it },
            label = { Text("Last Name") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = bio,
            onValueChange = { bio = it },
            label = { Text("Bio") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            maxLines = 3
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = { onSave(firstName, lastName, bio) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BorshchevykTheme.colors.primary),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Check, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Save Changes")
        }
    }
}

@Composable
fun EditPrivacyScreen(
    uiState: ProfileUiState,
    onUpdate: (UpdatePrivacySettingsRequest) -> Unit,
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
            onValueChange = { onUpdate(UpdatePrivacySettingsRequest(emailVisibility = it)) }
        )
        Spacer(modifier = Modifier.height(24.dp))
        PrivacyOption(
            title = "Who can search me by email",
            currentValue = settings.searchByEmailVisibility,
            onValueChange = { onUpdate(UpdatePrivacySettingsRequest(searchByEmailVisibility = it)) }
        )
        Spacer(modifier = Modifier.height(24.dp))
        PrivacyOption(
            title = "Who can see my profile photo",
            currentValue = settings.profilePhotoVisibility,
            onValueChange = { onUpdate(UpdatePrivacySettingsRequest(profilePhotoVisibility = it)) }
        )
        Spacer(modifier = Modifier.height(24.dp))
        PrivacyOption(
            title = "Who can invite me to chats",
            currentValue = settings.inviteToChatVisibility,
            onValueChange = { onUpdate(UpdatePrivacySettingsRequest(inviteToChatVisibility = it)) }
        )
    }
}

@Composable
fun PrivacyOption(
    title: String,
    currentValue: Visibility,
    onValueChange: (Visibility) -> Unit
) {
    Column {
        Text(text = title, style = BorshchevykTheme.typography.titleMedium, color = BorshchevykTheme.colors.onSurface)
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Visibility.entries.forEach { visibility ->
                val isSelected = visibility == currentValue
                FilterChip(
                    selected = isSelected,
                    onClick = { onValueChange(visibility) },
                    label = { Text(visibility.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BorshchevykTheme.colors.primaryContainer,
                        selectedLabelColor = BorshchevykTheme.colors.primary
                    )
                )
            }
        }
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val contentColor = if (isDestructive) BorshchevykTheme.colors.error else BorshchevykTheme.colors.onSurfaceVariant
        val titleColor = if (isDestructive) BorshchevykTheme.colors.error else BorshchevykTheme.colors.onSurface

        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Text(
            text = title,
            style = BorshchevykTheme.typography.bodyLarge,
            color = titleColor
        )
    }
}
