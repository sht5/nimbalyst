package com.tvremote.samsung

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.tvremote.samsung.ui.theme.SamsungTvRemoteTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SamsungTvRemoteTheme {
                // enableEdgeToEdge() draws content behind the status/nav bars — pad the root
                // once here so every screen clears them, instead of each screen guessing.
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing),
                ) {
                    RemoteApp()
                }
            }
        }
    }
}
