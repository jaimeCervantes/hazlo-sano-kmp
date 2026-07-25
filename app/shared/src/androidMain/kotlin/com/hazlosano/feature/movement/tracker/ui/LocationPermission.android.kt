package com.hazlosano.feature.movement.tracker.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun LocationPermissionEffect(onGranted: () -> Unit) {
    val context = LocalContext.current
    val alreadyGranted = remember {
        context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
    }
    // Asked together so a background recording can show its ongoing notification. Denying it does
    // not stop the recording: the foreground service runs either way, the notification just stays
    // hidden, so location is the only permission gating [onGranted].
    val permissions = remember {
        buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        if (results[Manifest.permission.ACCESS_FINE_LOCATION] == true) onGranted()
    }

    LaunchedEffect(Unit) {
        if (alreadyGranted) {
            onGranted()
        } else {
            launcher.launch(permissions)
        }
    }
}
