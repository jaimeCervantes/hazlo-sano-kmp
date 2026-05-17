package com.hazlosano

import androidx.compose.ui.window.ComposeUIViewController
import com.hazlosano.data.db.DatabaseProvider
import com.hazlosano.data.db.createSqlDriver

fun MainViewController() = ComposeUIViewController {
    if (!DatabaseProvider.isInitialized) {
        DatabaseProvider.initialize(createSqlDriver())
    }
    App()
}
