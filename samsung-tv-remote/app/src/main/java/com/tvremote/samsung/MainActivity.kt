package com.tvremote.samsung

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.tvremote.samsung.ui.theme.SamsungTvRemoteTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SamsungTvRemoteTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    RemoteApp()
                }
            }
        }
    }
}
