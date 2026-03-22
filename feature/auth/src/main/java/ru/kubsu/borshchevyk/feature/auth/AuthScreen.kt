package ru.kubsu.borshchevyk.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.kubsu.borshchevyk.core.model.domain.AuthMode

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
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "Borshchevyk Messenger", style = MaterialTheme.typography.headlineLarge)
            Spacer(modifier = Modifier.height(32.dp))

            // Mode Selector
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = mode == AuthMode.ONLINE, onClick = { mode = AuthMode.ONLINE })
                Text("Online")
                Spacer(modifier = Modifier.width(16.dp))
                RadioButton(selected = mode == AuthMode.OFFLINE, onClick = { mode = AuthMode.OFFLINE })
                Text("Offline (Mesh)")
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (mode == AuthMode.ONLINE) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                if (!isLogin) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = tag,
                        onValueChange = { tag = it },
                        label = { Text("Tag") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                if (uiState.isLoading) {
                    CircularProgressIndicator()
                } else {
                    Button(
                        onClick = {
                            if (isLogin) onLoginOnline(email, password)
                            else onRegisterOnline(email, password, tag)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isLogin) "Login" else "Register")
                    }
                    TextButton(onClick = { isLogin = !isLogin }) {
                        Text(if (isLogin) "Need an account? Register" else "Have an account? Login")
                    }
                }
            } else {
                OutlinedTextField(
                    value = tag,
                    onValueChange = { tag = it },
                    label = { Text("Unique Tag") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (uiState.isLoading) {
                    CircularProgressIndicator()
                } else {
                    Button(
                        onClick = { onRegisterOffline(tag) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Enter Mesh Network")
                    }
                }
            }
        }
    }
}
