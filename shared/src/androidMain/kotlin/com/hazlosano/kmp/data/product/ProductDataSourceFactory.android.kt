package com.hazlosano.kmp.data.product

import com.hazlosano.kmp.data.db.createSqlDriver

actual fun createProductDataSource(): ProductDataSource =
    SqlDelightProductDataSource(createSqlDriver())
