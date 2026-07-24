package com.hazlosano.feature.movement.tracker.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

@Composable
actual fun LocationPermissionEffect(onGranted: () -> Unit) {
    LaunchedEffect(Unit) { onGranted() }
}
