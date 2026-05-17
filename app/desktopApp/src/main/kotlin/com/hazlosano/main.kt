package com.hazlosano

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.hazlosano.data.db.DatabaseProvider
import com.hazlosano.data.db.createSqlDriver

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
