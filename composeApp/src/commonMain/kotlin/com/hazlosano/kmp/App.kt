package com.hazlosano.kmp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.hazlosano.kmp.core.ui.image.HazloImageLoader
import com.hazlosano.kmp.core.ui.theme.HazloSanoTheme
import com.hazlosano.kmp.domain.usecase.GetHomeContentUseCase
import com.hazlosano.kmp.domain.usecase.GetSleepContentUseCase
import com.hazlosano.kmp.feature.main.ui.MainScreen
import com.hazlosano.kmp.feature.home.presentation.HomeViewModel
import com.hazlosano.kmp.feature.sleep.presentation.SleepViewModel
import com.hazlosano.kmp.data.repository.MockHomeRepository
import com.hazlosano.kmp.data.repository.MockSleepRepository

@Composable
@Preview
fun App() {
    val viewModel = remember {
        HomeViewModel(GetHomeContentUseCase(MockHomeRepository()))
    }
    val sleepViewModel = remember {
        SleepViewModel(GetSleepContentUseCase(MockSleepRepository()))
    }

    HazloSanoTheme {
        HazloImageLoader {
            MainScreen(homeViewModel = viewModel, sleepViewModel = sleepViewModel)
        }
    }
}
