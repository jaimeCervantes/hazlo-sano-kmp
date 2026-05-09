package com.hazlosano.kmp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.hazlosano.kmp.data.sleep.SleepContextProvider
import com.hazlosano.kmp.data.sleep.SleepMonitor
import com.hazlosano.kmp.data.sleep.SleepReceiver
import com.hazlosano.kmp.data.sleep.createSleepDataSource

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        SleepContextProvider.initialize(this)

        val dataSource = createSleepDataSource()
        SleepReceiver.dataSource = dataSource
        SleepMonitor.start(this)

        setContent {
            App()
        }
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
