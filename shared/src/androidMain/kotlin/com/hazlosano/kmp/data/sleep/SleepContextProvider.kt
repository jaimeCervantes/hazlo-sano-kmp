package com.hazlosano.kmp.data.sleep

import android.content.Context

object SleepContextProvider {
    lateinit var applicationContext: Context
        private set

    fun initialize(context: Context) {
        applicationContext = context.applicationContext
    }
}
