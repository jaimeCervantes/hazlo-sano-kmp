package com.hazlosano.core.ui.model

import androidx.compose.ui.graphics.Color
import com.hazlosano.core.ui.theme.PillarMind
import com.hazlosano.core.ui.theme.PillarMovement
import com.hazlosano.core.ui.theme.PillarNutrition
import com.hazlosano.core.ui.theme.PillarSleep
import com.hazlosano.domain.model.PillarType

fun PillarType.toColor(): Color = when (this) {
    PillarType.SLEEP -> PillarSleep
    PillarType.MOVEMENT -> PillarMovement
    PillarType.NUTRITION -> PillarNutrition
    PillarType.MIND -> PillarMind
}
