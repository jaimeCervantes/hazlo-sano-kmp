package com.hazlosano.feature.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import com.hazlosano.core.ui.components.atomic.HazloTopAppBar
import com.hazlosano.core.ui.components.atomic.SectionHeader
import com.hazlosano.core.ui.theme.HazloShapes
import com.hazlosano.core.ui.theme.HazloSpaces
import com.hazlosano.domain.settings.ThemePreference
import com.hazlosano.feature.settings.presentation.SettingsViewModel
import com.hazlosano.feature.settings.presentation.rememberSettingsViewModel
import hazlosano.app.shared.generated.resources.Res
import hazlosano.app.shared.generated.resources.settings_appearance_title
import hazlosano.app.shared.generated.resources.settings_theme_dark
import hazlosano.app.shared.generated.resources.settings_theme_light
import hazlosano.app.shared.generated.resources.settings_theme_system
import hazlosano.app.shared.generated.resources.settings_theme_system_description
import hazlosano.app.shared.generated.resources.settings_title
import hazlosano.app.shared.generated.resources.top_app_bar_back
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** Etiquetas de prueba: la pantalla se afirma por aquí y no por su redacción. */
object SettingsTags {
    const val THEME_GROUP: String = "settings_theme_group"

    fun themeOption(preference: ThemePreference): String = "settings_theme_${preference.name}"
}

/** Los ajustes, cableados a su ViewModel. */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = rememberSettingsViewModel(),
) {
    val theme by viewModel.themePreference.collectAsState()

    SettingsContent(
        theme = theme,
        onChooseTheme = viewModel::chooseTheme,
        onBack = onBack,
        modifier = modifier,
    )
}

/** Sin ViewModel, para poder componerla en un test con cualquier preferencia. */
@Composable
fun SettingsContent(
    theme: ThemePreference,
    onChooseTheme: (ThemePreference) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        HazloTopAppBar(
            title = stringResource(Res.string.settings_title),
            showBackButton = true,
            onBackClick = onBack,
            backContentDescription = stringResource(Res.string.top_app_bar_back),
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = HazloSpaces.md,
                bottom = HazloSpaces.xl,
            ),
            verticalArrangement = Arrangement.spacedBy(HazloSpaces.sm),
        ) {
            item {
                SectionHeader(
                    title = stringResource(Res.string.settings_appearance_title),
                    modifier = Modifier.padding(horizontal = HazloSpaces.gutter),
                )
            }

            item {
                Surface(
                    shape = RoundedCornerShape(HazloShapes.card),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = HazloSpaces.gutter)
                        // Un solo grupo de selección: quien navega con lector de pantalla oye
                        // "1 de 3" y no tres interruptores sueltos que resultan excluirse.
                        .selectableGroup()
                        .testTag(SettingsTags.THEME_GROUP),
                ) {
                    Column {
                        ThemeOption(
                            preference = ThemePreference.SYSTEM,
                            labelRes = Res.string.settings_theme_system,
                            descriptionRes = Res.string.settings_theme_system_description,
                            selected = theme == ThemePreference.SYSTEM,
                            onSelect = onChooseTheme,
                        )
                        ThemeOption(
                            preference = ThemePreference.LIGHT,
                            labelRes = Res.string.settings_theme_light,
                            selected = theme == ThemePreference.LIGHT,
                            onSelect = onChooseTheme,
                        )
                        ThemeOption(
                            preference = ThemePreference.DARK,
                            labelRes = Res.string.settings_theme_dark,
                            selected = theme == ThemePreference.DARK,
                            onSelect = onChooseTheme,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeOption(
    preference: ThemePreference,
    labelRes: StringResource,
    selected: Boolean,
    onSelect: (ThemePreference) -> Unit,
    descriptionRes: StringResource? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // Toda la fila selecciona, no sólo el círculo: un objetivo de 24 dp es incómodo en un
            // teléfono y no hay nada más en la fila que pueda querer el toque.
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = { onSelect(preference) },
            )
            .padding(horizontal = HazloSpaces.md, vertical = HazloSpaces.sm)
            .testTag(SettingsTags.themeOption(preference)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Column(modifier = Modifier.padding(start = HazloSpaces.sm)) {
            Text(
                text = stringResource(labelRes),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
            )
            descriptionRes?.let {
                Text(
                    text = stringResource(it),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
