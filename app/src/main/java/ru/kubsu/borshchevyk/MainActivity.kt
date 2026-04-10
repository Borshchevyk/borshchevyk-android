package ru.kubsu.borshchevyk

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import ru.kubsu.borshchevyk.feature.auth.AuthRoute
import ru.kubsu.borshchevyk.ui.theme.BorshchevykandroidTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BorshchevykandroidTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AuthRoute(
                        onAuthSuccess = {
                            // Proceed to main content
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}