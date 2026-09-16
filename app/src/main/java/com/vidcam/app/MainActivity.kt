package com.vidcam.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.vidcam.app.ui.VidCamApp
import com.vidcam.app.ui.theme.VidCamTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VidCamTheme {
                VidCamApp()
            }
        }
    }
}
