package com.hazlosano.kmp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import android.app.AlertDialog
import android.content.DialogInterface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import com.hazlosano.kmp.data.sleep.SleepMonitor
import com.hazlosano.kmp.data.sleep.SleepServiceLocator

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            SleepMonitor.start(this)
        } else {
            showDeniedRationale()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        SleepServiceLocator.initialize(this)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            SleepMonitor.start(this)
        } else {
            if (shouldShowRequestPermissionRationale(Manifest.permission.ACTIVITY_RECOGNITION)) {
                showPreRationale()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
            }
        }

        setContent {
            App()
        }
    }

    private fun showPreRationale() {
        AlertDialog.Builder(this)
            .setTitle("Sueño y Descanso")
            .setMessage(
                "El pilar de Sueño y Descanso usa los sensores de tu teléfono para medir " +
                    "tus hábitos de descanso mientras duermes. Esto ayuda a entender tus patrones " +
                    "y mejorar tu salud metabólica y mental.\n\n" +
                    "La app no graba audio ni video. Solo detecta si estás dormido o despierto " +
                    "usando los sensores de movimiento y luz ambiental.",
            )
            .setPositiveButton("Activar") { _: DialogInterface, _: Int ->
                requestPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
            }
            .setNegativeButton("Ahora no", null)
            .show()
    }

    private fun showDeniedRationale() {
        AlertDialog.Builder(this)
            .setTitle("Permiso necesario")
            .setMessage(
                "Sin el permiso de Actividad Física, la app no puede medir tu sueño automáticamente. " +
                    "Puedes activarlo manualmente en Ajustes > Aplicaciones > Hazlo Sano > Permisos.",
            )
            .setPositiveButton("Ir a Ajustes") { _: DialogInterface, _: Int ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
