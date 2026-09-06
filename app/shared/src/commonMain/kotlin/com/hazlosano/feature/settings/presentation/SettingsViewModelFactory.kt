package com.hazlosano.feature.settings.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.hazlosano.data.settings.appSettingsRepository

/** Ensambla un [SettingsViewModel] con sus dependencias de plataforma (DI manual). */
fun createSettingsViewModel(): SettingsViewModel =
    SettingsViewModel(repository = appSettingsRepository())

/**
 * El mismo ViewModel, atado a la composición.
 *
 * El tema lo observan dos sitios —la raíz del app, que lo aplica, y la pantalla de ajustes, que lo
 * enseña—, así que ambos piden el suyo y los dos leen del mismo almacén.
 */
@Composable
fun rememberSettingsViewModel(): SettingsViewModel = remember { createSettingsViewModel() }
