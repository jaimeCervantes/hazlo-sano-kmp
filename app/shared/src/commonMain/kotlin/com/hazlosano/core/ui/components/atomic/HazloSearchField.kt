package com.hazlosano.core.ui.components.atomic

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * El campo de escribir para acotar una lista.
 *
 * Estaba privado dentro de `HazloExploreItemsSection`, donde sólo lo alcanzaban las rejillas de los
 * pilares. Sale aquí al aparecer el segundo sitio que lo necesita —el feed del Inicio— porque
 * copiarlo habría sido el segundo componente casi idéntico que `AGENTS.md` llama fallo de diseño.
 *
 * Es atómico de verdad: no lee el catálogo de recursos ni sabe qué es una publicación. El texto de
 * ayuda entra por parámetro y quien llama decide qué se está acotando.
 */
@Composable
fun HazloSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholderText: String,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(placeholderText) },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(FULLY_ROUNDED),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedBorderColor = accentColor,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
        ),
    )
}

/** Un radio mayor que la mitad de la altura redondea del todo, sea cual sea esa altura. */
private val FULLY_ROUNDED = 50.dp
