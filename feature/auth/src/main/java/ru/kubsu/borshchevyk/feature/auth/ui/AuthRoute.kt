package ru.kubsu.borshchevyk.feature.auth.ui

import android.widget.Toast
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.kubsu.borshchevyk.core.ui.theme.BorshchevykTheme
import ru.kubsu.borshchevyk.feature.auth.AuthEffect
import ru.kubsu.borshchevyk.feature.auth.AuthIntent
import ru.kubsu.borshchevyk.feature.auth.AuthViewModel

@Composable
fun AuthRoute(
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.handleIntent(AuthIntent.CheckAuth)
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is AuthEffect.AuthSuccess -> {
                    onAuthSuccess()
                }
                is AuthEffect.ShowError -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        containerColor = BorshchevykTheme.colors.background,
        modifier = modifier
    ) { padding ->
        AuthScreen(
            uiState = uiState,
            onIntent = { intent -> viewModel.handleIntent(intent) },
            modifier = Modifier.padding(padding)
        )
    }
}
