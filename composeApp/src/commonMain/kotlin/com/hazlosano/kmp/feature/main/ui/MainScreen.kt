package com.hazlosano.kmp.feature.main.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hazlosano.kmp.core.ui.components.atomic.HazloTopAppBar
import com.hazlosano.kmp.core.ui.theme.HazloSanoGreen
import com.hazlosano.kmp.core.ui.theme.PillarMind
import com.hazlosano.kmp.core.ui.theme.PillarMovement
import com.hazlosano.kmp.core.ui.theme.PillarNutrition
import com.hazlosano.kmp.core.ui.theme.PillarSleep
import com.hazlosano.kmp.feature.home.presentation.HomeViewModel
import com.hazlosano.kmp.feature.sleep.presentation.SleepViewModel
import com.hazlosano.kmp.feature.home.ui.HomeScreen
import com.hazlosano.kmp.feature.sleep.ui.SleepScreen

enum class BottomTab(val label: String, val icon: ImageVector, val color: Color) {
    Inicio("Inicio", Icons.Filled.Home, HazloSanoGreen),
    Sueno("Sueño", Icons.Filled.Bedtime, PillarSleep),
    Nutricion("Nutrición", Icons.Filled.Restaurant, PillarNutrition),
    Movimiento("Movimiento", Icons.AutoMirrored.Filled.DirectionsRun, PillarMovement),
    Mente("Mente", Icons.Filled.SelfImprovement, PillarMind),
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainScreen(homeViewModel: HomeViewModel, sleepViewModel: SleepViewModel) {
    var selectedTab by remember { mutableStateOf(BottomTab.Inicio) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        HazloTopAppBar(scrollBehavior = scrollBehavior)
        Scaffold(
            modifier = Modifier
                .weight(1f)
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ) {
                    BottomTab.entries.forEach { tab ->
                        val selected = selectedTab == tab
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.label,
                                )
                            },
                            label = { Text(tab.label) },
                            selected = selected,
                            onClick = { selectedTab = tab },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = tab.color,
                                indicatorColor = tab.color,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )
                    }
                }
            },
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                when (selectedTab) {
                    BottomTab.Inicio -> HomeScreen(viewModel = homeViewModel)
                    BottomTab.Sueno -> SleepScreen(viewModel = sleepViewModel)
                    else -> PlaceholderScreen(tab = selectedTab)
                }
            }
        }
    }
}

@Composable
private fun PlaceholderScreen(tab: BottomTab) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = tab.icon,
                contentDescription = null,
                tint = tab.color,
                modifier = Modifier.size(64.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = tab.label,
                style = MaterialTheme.typography.headlineMedium,
                color = tab.color,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
