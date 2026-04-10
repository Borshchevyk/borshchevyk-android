package ru.kubsu.borshchevyk.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.kubsu.borshchevyk.core.model.domain.AuthMode
import ru.kubsu.borshchevyk.core.ui.theme.BorshchevykTheme

@Composable
fun AuthRoute(
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.checkAuth()
    }

    LaunchedEffect(uiState.success) {
        if (uiState.success) {
            onAuthSuccess()
        }
    }

    AuthScreen(
        uiState = uiState,
        onRegisterOffline = viewModel::onRegisterOffline,
        onRegisterOnline = viewModel::onRegisterOnline,
        onLoginOnline = viewModel::onLoginOnline,
        onErrorShown = viewModel::errorShown,
        modifier = modifier
    )
}

@Composable
internal fun AuthScreen(
    uiState: AuthUiState,
    onRegisterOffline: (String) -> Unit,
    onRegisterOnline: (String, String, String) -> Unit,
    onLoginOnline: (String, String) -> Unit,
    onErrorShown: () -> Unit,
    modifier: Modifier = Modifier
) {
    var mode by remember { mutableStateOf(AuthMode.ONLINE) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var tag by remember { mutableStateOf("") }
    var isLogin by remember { mutableStateOf(true) }
    
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            onErrorShown()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = BorshchevykTheme.colors.background,
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
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

                if (uiState.isLoading) {
                    CircularProgressIndicator(color = BorshchevykTheme.colors.primary)
                } else {
                    Button(
                        onClick = {
                            if (isLogin) onLoginOnline(email, password)
                            else onRegisterOnline(email, password, tag)
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

                if (uiState.isLoading) {
                    CircularProgressIndicator(color = BorshchevykTheme.colors.primary)
                } else {
                    Button(
                        onClick = { onRegisterOffline(tag) },
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
}

@Composable
fun TabButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) BorshchevykTheme.colors.background else Color.Transparent,
            contentColor = if (isSelected) BorshchevykTheme.colors.primary else BorshchevykTheme.colors.onSurfaceVariant
        ),
        shape = RoundedCornerShape(8.dp),
        elevation = if (isSelected) ButtonDefaults.buttonElevation(defaultElevation = 2.dp) else null,
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        Text(text = text, style = BorshchevykTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
    }
}

@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isPassword: Boolean = false,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = BorshchevykTheme.colors.primary,
            unfocusedBorderColor = BorshchevykTheme.colors.outline,
            focusedTextColor = BorshchevykTheme.colors.onSurface,
            unfocusedTextColor = BorshchevykTheme.colors.onSurface,
            cursorColor = BorshchevykTheme.colors.primary,
            focusedLabelColor = BorshchevykTheme.colors.primary
        ),
        singleLine = true
    )
}
