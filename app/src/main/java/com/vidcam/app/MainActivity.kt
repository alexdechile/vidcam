package com.vidcam.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vidcam.app.share.SharedFile
import com.vidcam.app.share.SharedMedia
import com.vidcam.app.ui.VidCamApp
import com.vidcam.app.ui.theme.VidCamTheme
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    /**
     * Archivos que llegaron compartidos desde otra app y que el editor todavía
     * no consumió. Se guardan aquí —y no se procesan en [onCreate]— porque el
     * `EditorViewModel` aún no existe y porque el usuario puede necesitar
     * conceder los permisos de medios antes de que haya editor.
     */
    private val sharedFiles = MutableStateFlow<List<SharedFile>>(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Al recrearse (rotación, cambio de tema) el intent se vuelve a
        // entregar: reprocesarlo duplicaría los archivos ya recibidos.
        if (savedInstanceState == null) collectSharedFiles(intent)
        setContent {
            VidCamTheme {
                val pending by sharedFiles.collectAsStateWithLifecycle()
                VidCamApp(
                    sharedFiles = pending,
                    onSharedFilesConsumed = { sharedFiles.value = emptyList() },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        collectSharedFiles(intent)
    }

    private fun collectSharedFiles(intent: Intent?) {
        val received = SharedMedia.fromIntent(this, intent)
        if (received.isEmpty()) return
        sharedFiles.value = sharedFiles.value + received
    }
}
