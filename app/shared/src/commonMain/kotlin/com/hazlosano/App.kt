package com.hazlosano

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.hazlosano.core.ui.HazloLanguage
import com.hazlosano.core.ui.image.HazloImageLoader
import com.hazlosano.core.ui.theme.HazloSanoTheme
import com.hazlosano.domain.usecase.GetHomeContentUseCase
import com.hazlosano.feature.main.ui.MainScreen
import com.hazlosano.feature.home.presentation.HomeViewModel
import com.hazlosano.feature.sleep.presentation.SleepViewModel
import com.hazlosano.data.repository.MockHomeRepository
import com.hazlosano.data.sleep.SleepSessionRepositoryImpl
import com.hazlosano.data.sleep.createSleepDataSource
import com.hazlosano.domain.usecase.GetSleepAnalysisUseCase
import com.hazlosano.domain.repository.SleepSessionRepository
import com.hazlosano.domain.settings.resolvesToDark
import com.hazlosano.feature.settings.presentation.rememberSettingsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun App() {
    val viewModel = remember {
        HomeViewModel(GetHomeContentUseCase(MockHomeRepository()))
    }
    val sleepSessionRepository: SleepSessionRepository = remember {
        SleepSessionRepositoryImpl(createSleepDataSource())
    }
    val getSleepAnalysisUseCase = remember {
        GetSleepAnalysisUseCase(sleepSessionRepository)
    }
    val sleepViewModel = remember {
        SleepViewModel(
            getSleepAnalysisUseCase,
            sleepSessionRepository,
        )
    }
    val sleepAnalysis by sleepViewModel.sleepAnalysis.collectAsState()

    val scope = rememberCoroutineScope()

    // El catálogo ya no se siembra aquí: cada pestaña de pilar lo pide al sitio cuando se abre, y
    // guarda lo leído para poder abrirse sin red la próxima vez.
    LaunchedEffect(Unit) {
        delay(3000L)
        sleepViewModel.refresh()
        delay(5000L)
        sleepViewModel.refresh()
    }

    val onRefreshSleep: () -> Unit = {
        scope.launch { sleepViewModel.refresh() }
    }

    // Los dos ajustes se leen aquí arriba, que es donde el app entero puede repintarse al cambiarlos.
    val settingsViewModel = rememberSettingsViewModel()
    val themePreference by settingsViewModel.themePreference.collectAsState()
    val languagePreference by settingsViewModel.languagePreference.collectAsState()

    HazloSanoTheme(darkTheme = themePreference.resolvesToDark(isSystemInDarkTheme())) {
        HazloLanguage(languagePreference) {
            HazloImageLoader {
                MainScreen(
                    homeViewModel = viewModel,
                    sleepViewModel = sleepViewModel,
                    sleepAnalysis = sleepAnalysis,
                    onRefreshSleep = onRefreshSleep,
                    sleepSessionRepository = sleepSessionRepository,
                )
            }
        }
    }
}
