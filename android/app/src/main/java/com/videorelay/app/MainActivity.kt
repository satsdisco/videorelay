package com.videorelay.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.videorelay.app.navigation.VideoRelayNavHost
import com.videorelay.app.ui.theme.VideoRelayTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VideoRelayTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    VideoRelayNavHost()
                }
            }
        }
    }
}
