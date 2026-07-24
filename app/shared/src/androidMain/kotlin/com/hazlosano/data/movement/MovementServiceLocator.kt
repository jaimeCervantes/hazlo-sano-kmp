package com.hazlosano.data.movement

import android.content.Context

/**
 * Holds the application context for movement data sources. Initialized once from the Android entry
 * point (MainActivity), mirroring [com.hazlosano.data.sleep.SleepServiceLocator].
 */
object MovementServiceLocator {

    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    fun requireContext(): Context =
        appContext ?: throw IllegalStateException(
            "MovementServiceLocator not initialized. Call initialize(context) first.",
        )
}
