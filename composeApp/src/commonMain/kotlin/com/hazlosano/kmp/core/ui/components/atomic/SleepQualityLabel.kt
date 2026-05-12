package com.hazlosano.kmp.core.ui.components.atomic

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import kmp.composeapp.generated.resources.Res
import kmp.composeapp.generated.resources.sleep_quality_excellent
import kmp.composeapp.generated.resources.sleep_quality_good
import kmp.composeapp.generated.resources.sleep_quality_interrupted
import kmp.composeapp.generated.resources.sleep_quality_no_data
import kmp.composeapp.generated.resources.sleep_quality_poor
import org.jetbrains.compose.resources.stringResource

@Composable
fun SleepQualityLabel(
    efficiency: Float,
    segments: Int,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = when {
            efficiency >= EXCELLENT_THRESHOLD && segments <= MAX_EXCELLENT_SEGMENTS ->
                stringResource(Res.string.sleep_quality_excellent)
            efficiency >= GOOD_THRESHOLD ->
                stringResource(Res.string.sleep_quality_good)
            efficiency >= INTERRUPTED_THRESHOLD ->
                stringResource(Res.string.sleep_quality_interrupted)
            efficiency > 0f ->
                stringResource(Res.string.sleep_quality_poor)
            else ->
                stringResource(Res.string.sleep_quality_no_data)
        },
        style = MaterialTheme.typography.labelLarge,
        color = accentColor,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier,
    )
}

const val EXCELLENT_THRESHOLD = 0.90f
const val GOOD_THRESHOLD = 0.80f
const val INTERRUPTED_THRESHOLD = 0.65f
const val MAX_EXCELLENT_SEGMENTS = 2
