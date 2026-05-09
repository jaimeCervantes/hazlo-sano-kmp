package com.hazlosano.kmp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import com.hazlosano.kmp.data.sleep.SleepContextProvider
import com.hazlosano.kmp.data.sleep.SleepMonitor
import com.hazlosano.kmp.data.sleep.SleepReceiver
import com.hazlosano.kmp.data.sleep.createSleepDataSource

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            startSleepMonitoring()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        SleepContextProvider.initialize(this)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startSleepMonitoring()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
        }

        setContent {
            App()
        }
    }

    private fun startSleepMonitoring() {
        val dataSource = createSleepDataSource()
        SleepReceiver.dataSource = dataSource
        SleepMonitor.start(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            SleepMonitor.stop(this)
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
