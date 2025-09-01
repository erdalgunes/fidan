package com.erdalgunes.fidan

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
import androidx.compose.ui.tooling.preview.Preview
import com.erdalgunes.fidan.core.ui.theme.FidanTheme
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.NavigableCircuitContent
import com.slack.circuit.foundation.rememberCircuitNavigator
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var circuit: Circuit
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            FidanTheme {
                CircuitCompositionLocals(circuit) {
                    val navigator = rememberCircuitNavigator()
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        NavigableCircuitContent(
                            navigator = navigator,
                            startScreen = HomeScreen,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}

// Temporary home screen for initial setup
object HomeScreen : com.slack.circuit.runtime.screen.Screen

@Composable
fun HomeContent() {
    Text("Welcome to Fidan - Your Focus Companion")
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    FidanTheme {
        HomeContent()
    }
}