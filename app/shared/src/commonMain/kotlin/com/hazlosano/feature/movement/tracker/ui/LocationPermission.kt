package com.hazlosano.feature.movement.tracker.ui

import androidx.compose.runtime.Composable

/**
 * Ensures the location permission on the current platform, invoking [onGranted] once it is
 * available. Android prompts the user; platforms without a location integration grant immediately.
 */
@Composable
expect fun LocationPermissionEffect(onGranted: () -> Unit)
