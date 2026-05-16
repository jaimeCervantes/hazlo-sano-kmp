package com.hazlosano.kmp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.hazlosano.kmp.core.ui.image.HazloImageLoader
import com.hazlosano.kmp.core.ui.theme.HazloSanoTheme
import com.hazlosano.kmp.domain.usecase.GetHomeContentUseCase
import com.hazlosano.kmp.domain.usecase.GetSleepContentUseCase
import com.hazlosano.kmp.feature.main.ui.MainScreen
import com.hazlosano.kmp.feature.home.presentation.HomeViewModel
import com.hazlosano.kmp.feature.sleep.presentation.SleepViewModel
import com.hazlosano.kmp.data.repository.MockHomeRepository
import com.hazlosano.kmp.data.repository.MockSleepRepository
import com.hazlosano.kmp.data.product.SeedProducts
import com.hazlosano.kmp.data.product.createProductDataSource
import com.hazlosano.kmp.data.repository.ProductRepositoryImpl
import com.hazlosano.kmp.data.sleep.SleepSessionRepositoryImpl
import com.hazlosano.kmp.data.sleep.createSleepDataSource
import com.hazlosano.kmp.domain.repository.ProductRepository
import com.hazlosano.kmp.domain.usecase.GetSleepAnalysisUseCase
import com.hazlosano.kmp.domain.usecase.SearchProductsUseCase
import com.hazlosano.kmp.domain.repository.SleepSessionRepository
import com.hazlosano.kmp.feature.nutrition.presentation.NutritionViewModel
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
            GetSleepContentUseCase(MockSleepRepository()),
            getSleepAnalysisUseCase,
            sleepSessionRepository,
        )
    }
    val sleepAnalysis by sleepViewModel.sleepAnalysis.collectAsState()

    val productRepository: ProductRepository = remember {
        ProductRepositoryImpl(createProductDataSource())
    }
    val searchProductsUseCase = remember {
        SearchProductsUseCase(productRepository)
    }
    val nutritionViewModel = remember {
        NutritionViewModel(searchProductsUseCase)
    }

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        try {
            SeedProducts.seedIfEmpty()
        } catch (e: Exception) {
            // non-fatal: products already seeded or DB temporarily unavailable
        }
        nutritionViewModel.refresh()
        delay(3000L)
        sleepViewModel.refresh()
        delay(5000L)
        sleepViewModel.refresh()
    }

    val onRefreshSleep: () -> Unit = {
        scope.launch { sleepViewModel.refresh() }
    }

    HazloSanoTheme {
        HazloImageLoader {
            MainScreen(
                homeViewModel = viewModel,
                sleepViewModel = sleepViewModel,
                nutritionViewModel = nutritionViewModel,
                sleepAnalysis = sleepAnalysis,
                onRefreshSleep = onRefreshSleep,
                sleepSessionRepository = sleepSessionRepository,
            )
        }
    }
}
