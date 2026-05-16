package com.hazlosano.kmp

import androidx.compose.ui.window.ComposeUIViewController
import com.hazlosano.kmp.data.db.DatabaseProvider
import com.hazlosano.kmp.data.db.createSqlDriver

fun MainViewController() = ComposeUIViewController {
    if (!DatabaseProvider.isInitialized) {
        DatabaseProvider.initialize(createSqlDriver())
    }
    App()
}
