package ru.kubsu.borshchevyk.feature.auth.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.kubsu.borshchevyk.core.model.domain.AuthMode
import ru.kubsu.borshchevyk.core.ui.theme.BorshchevykTheme
import ru.kubsu.borshchevyk.feature.auth.AuthIntent
import ru.kubsu.borshchevyk.feature.auth.AuthUiState
import ru.kubsu.borshchevyk.feature.auth.ui.components.AuthTextField
import ru.kubsu.borshchevyk.feature.auth.ui.components.TabButton

@Composable
internal fun AuthScreen(
    uiState: AuthUiState,
    onIntent: (AuthIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Borshchevyk",
            style = BorshchevykTheme.typography.titleLarge.copy(fontSize = 32.sp),
            color = BorshchevykTheme.colors.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Modern Messenger",
            style = BorshchevykTheme.typography.bodyLarge,
            color = BorshchevykTheme.colors.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(48.dp))

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = BorshchevykTheme.colors.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(4.dp)) {
                TabButton(
                    text = "Online",
                    isSelected = uiState.mode == AuthMode.ONLINE,
                    onClick = { onIntent(AuthIntent.ToggleAuthMode) },
                    modifier = Modifier.weight(1f)
                )
                TabButton(
                    text = "Mesh",
                    isSelected = uiState.mode == AuthMode.OFFLINE,
                    onClick = { onIntent(AuthIntent.ToggleAuthMode) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        AnimatedContent(
            targetState = uiState.mode,
            transitionSpec = {
                fadeIn(animationSpec = tween(220, delayMillis = 90)) togetherWith
                fadeOut(animationSpec = tween(90))
            },
            label = "AuthModeTransition"
        ) { mode ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (mode == AuthMode.ONLINE) {
                    OnlineAuthFields(uiState, onIntent)
                } else {
                    OfflineAuthFields(uiState, onIntent)
                }
            }
        }
    }
}

@Composable
private fun OnlineAuthFields(
    uiState: AuthUiState,
    onIntent: (AuthIntent) -> Unit
) {
    AuthTextField(
        value = uiState.email,
        onValueChange = { onIntent(AuthIntent.EmailChanged(it)) },
        label = "Email",
        error = uiState.emailError,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(16.dp))
    AuthTextField(
        value = uiState.password,
        onValueChange = { onIntent(AuthIntent.PasswordChanged(it)) },
        label = "Password",
        isPassword = true,
        error = uiState.passwordError,
        modifier = Modifier.fillMaxWidth()
    )

    if (!uiState.isLogin) {
        Spacer(modifier = Modifier.height(16.dp))
        AuthTextField(
            value = uiState.firstName,
            onValueChange = { onIntent(AuthIntent.FirstNameChanged(it)) },
            label = "First Name",
            error = uiState.firstNameError,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        AuthTextField(
            value = uiState.lastName,
            onValueChange = { onIntent(AuthIntent.LastNameChanged(it)) },
            label = "Last Name (Optional)",
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        AuthTextField(
            value = uiState.tag,
            onValueChange = { onIntent(AuthIntent.TagChanged(it)) },
            label = "Tag",
            error = uiState.tagError,
            modifier = Modifier.fillMaxWidth()
        )
    }

    Spacer(modifier = Modifier.height(32.dp))

    if (uiState.isLoading) {
        CircularProgressIndicator(color = BorshchevykTheme.colors.primary)
    } else {
        Button(
            onClick = { onIntent(AuthIntent.Submit) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = BorshchevykTheme.colors.primary,
                contentColor = BorshchevykTheme.colors.onPrimary
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = if (uiState.isLogin) "Login" else "Register",
                style = BorshchevykTheme.typography.titleMedium
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = { onIntent(AuthIntent.ToggleLoginRegister) }) {
            Text(
                text = if (uiState.isLogin) "Need an account? Register" else "Have an account? Login",
                color = BorshchevykTheme.colors.primary
            )
        }
    }
}

@Composable
private fun OfflineAuthFields(
    uiState: AuthUiState,
    onIntent: (AuthIntent) -> Unit
) {
    AuthTextField(
        value = uiState.tag,
        onValueChange = { onIntent(AuthIntent.TagChanged(it)) },
        label = "Unique Tag",
        error = uiState.tagError,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(32.dp))

    if (uiState.isLoading) {
        CircularProgressIndicator(color = BorshchevykTheme.colors.primary)
    } else {
        Button(
            onClick = { onIntent(AuthIntent.Submit) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = BorshchevykTheme.colors.primary,
                contentColor = BorshchevykTheme.colors.onPrimary
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "Enter Mesh Network",
                style = BorshchevykTheme.typography.titleMedium
            )
        }
    }
}
