package com.vidcam.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun PermissionScreen(onRequest: () -> Unit, onOpenSettings: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "VidCam",
                style = MaterialTheme.typography.headlineLarge,
            )
            Text(
                text = "Vídeos verticales de hasta 30 segundos",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = "Necesitamos acceso a la cámara, el micrófono y tus archivos " +
                    "multimedia para grabar, importar vídeos y añadir música.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 24.dp, bottom = 24.dp),
            )
            Button(
                onClick = onRequest,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = "Conceder permisos")
            }
            TextButton(
                onClick = onOpenSettings,
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text(text = "Abrir ajustes de la app")
            }
        }
    }
}
