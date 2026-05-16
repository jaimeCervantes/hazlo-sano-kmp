package com.hazlosano.kmp.data.product

import com.hazlosano.kmp.data.db.DatabaseProvider

actual fun createProductDataSource(): ProductDataSource =
    SqlDelightProductDataSource(DatabaseProvider.get())
