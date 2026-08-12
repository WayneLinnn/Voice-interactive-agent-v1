package com.waynelinnn.voiceagent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.waynelinnn.voiceagent.presentation.navigation.VoiceAgentNavHost
import com.waynelinnn.voiceagent.presentation.theme.VoiceAgentTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VoiceAgentTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    VoiceAgentNavHost()
                }
            }
        }
    }
}
