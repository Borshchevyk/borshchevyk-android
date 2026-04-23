package ru.kubsu.borshchevyk.feature.auth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.kubsu.borshchevyk.core.model.domain.AuthMode
import ru.kubsu.borshchevyk.core.ui.theme.BorshchevykTheme
import ru.kubsu.borshchevyk.feature.auth.AuthIntent
import ru.kubsu.borshchevyk.feature.auth.ui.components.AuthTextField
import ru.kubsu.borshchevyk.feature.auth.ui.components.TabButton

@Composable
internal fun AuthScreen(
    isLoading: Boolean,
    onIntent: (AuthIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    var mode by rememberSaveable { mutableStateOf(AuthMode.ONLINE) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var tag by rememberSaveable { mutableStateOf("") }
    var isLogin by rememberSaveable { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxSize()
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
                    isSelected = mode == AuthMode.ONLINE,
                    onClick = { mode = AuthMode.ONLINE },
                    modifier = Modifier.weight(1f)
                )
                TabButton(
                    text = "Mesh",
                    isSelected = mode == AuthMode.OFFLINE,
                    onClick = { mode = AuthMode.OFFLINE },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))

        if (mode == AuthMode.ONLINE) {
            AuthTextField(
                value = email,
                onValueChange = { email = it },
                label = "Email",
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            AuthTextField(
                value = password,
                onValueChange = { password = it },
                label = "Password",
                isPassword = true,
                modifier = Modifier.fillMaxWidth()
            )
            if (!isLogin) {
                Spacer(modifier = Modifier.height(16.dp))
                AuthTextField(
                    value = tag,
                    onValueChange = { tag = it },
                    label = "Tag",
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.height(32.dp))

            if (isLoading) {
                CircularProgressIndicator(color = BorshchevykTheme.colors.primary)
            } else {
                Button(
                    onClick = {
                        if (isLogin) {
                            onIntent(AuthIntent.LoginOnline(email, password))
                        } else {
                            onIntent(AuthIntent.RegisterOnline(email, password, tag))
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BorshchevykTheme.colors.primary,
                        contentColor = BorshchevykTheme.colors.onPrimary
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = if (isLogin) "Login" else "Register",
                        style = BorshchevykTheme.typography.titleMedium
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = { isLogin = !isLogin }) {
                    Text(
                        text = if (isLogin) "Need an account? Register" else "Have an account? Login",
                        color = BorshchevykTheme.colors.primary
                    )
                }
            }
        } else {
            AuthTextField(
                value = tag,
                onValueChange = { tag = it },
                label = "Unique Tag",
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(32.dp))

            if (isLoading) {
                CircularProgressIndicator(color = BorshchevykTheme.colors.primary)
            } else {
                Button(
                    onClick = { onIntent(AuthIntent.RegisterOffline(tag)) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
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
    }
}
