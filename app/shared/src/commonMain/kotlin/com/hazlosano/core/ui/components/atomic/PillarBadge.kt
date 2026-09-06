package com.hazlosano.core.ui.components.atomic

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.hazlosano.core.ui.theme.HazloShapes
import com.hazlosano.core.ui.theme.HazloSpaces
import com.hazlosano.core.ui.theme.PillarPalette

/**
 * El chip que dice de qué pilar habla algo.
 *
 * Toma la pareja `soft`/`ink` en vez de un color suelto. Antes se fabricaba el fondo tenue solo,
 * con `color.copy(alpha = 0.1f)`, y escribía encima con ese mismo color a plena opacidad — que es
 * exactamente lo que la rampa del sitio hace bien: un tinte del 10 % no garantiza contraste con
 * nada, y con el azul de Mente el texto quedaba en 2.14:1.
 *
 * Sigue siendo atómico: recibe la paleta, no el pilar, así que no sabe qué es un pilar.
 */
@Composable
fun PillarBadge(
    label: String,
    palette: PillarPalette,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(HazloShapes.chip),
        color = palette.soft,
        modifier = modifier,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = palette.ink,
            modifier = Modifier.padding(horizontal = HazloSpaces.sm, vertical = HazloSpaces.xs),
        )
    }
}
