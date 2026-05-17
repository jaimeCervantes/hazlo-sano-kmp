package com.hazlosano.data.product

import com.hazlosano.data.db.DatabaseProvider

actual fun createProductDataSource(): ProductDataSource =
    SqlDelightProductDataSource(DatabaseProvider.get())
