package com.hazlosano.kmp

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.hazlosano.kmp.data.db.DatabaseProvider
import com.hazlosano.kmp.data.db.createSqlDriver

fun main() = application {
    if (!DatabaseProvider.isInitialized) {
        DatabaseProvider.initialize(createSqlDriver())
    }
    Window(
        onCloseRequest = ::exitApplication,
        title = "Hazlo Sano",
    ) {
        App()
    }
}
