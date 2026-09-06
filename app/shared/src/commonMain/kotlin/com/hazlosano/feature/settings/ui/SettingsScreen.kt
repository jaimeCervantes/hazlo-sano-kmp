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
import androidx.compose.foundation.lazy.LazyListScope
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
import com.hazlosano.core.ui.platformAppliesLanguage
import com.hazlosano.core.ui.theme.HazloShapes
import com.hazlosano.core.ui.theme.HazloSpaces
import com.hazlosano.domain.settings.LanguagePreference
import com.hazlosano.domain.settings.ThemePreference
import com.hazlosano.feature.settings.presentation.SettingsViewModel
import com.hazlosano.feature.settings.presentation.rememberSettingsViewModel
import hazlosano.app.shared.generated.resources.Res
import hazlosano.app.shared.generated.resources.settings_appearance_title
import hazlosano.app.shared.generated.resources.settings_language_english
import hazlosano.app.shared.generated.resources.settings_language_needs_restart
import hazlosano.app.shared.generated.resources.settings_language_spanish
import hazlosano.app.shared.generated.resources.settings_language_system
import hazlosano.app.shared.generated.resources.settings_language_system_description
import hazlosano.app.shared.generated.resources.settings_language_title
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
    const val LANGUAGE_GROUP: String = "settings_language_group"
    const val LANGUAGE_NOTICE: String = "settings_language_notice"

    fun themeOption(preference: ThemePreference): String = "settings_theme_${preference.name}"

    fun languageOption(preference: LanguagePreference): String =
        "settings_language_${preference.name}"
}

/** Los ajustes, cableados a su ViewModel. */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = rememberSettingsViewModel(),
) {
    val theme by viewModel.themePreference.collectAsState()
    val language by viewModel.languagePreference.collectAsState()

    SettingsContent(
        theme = theme,
        language = language,
        onChooseTheme = viewModel::chooseTheme,
        onChooseLanguage = viewModel::chooseLanguage,
        onBack = onBack,
        modifier = modifier,
    )
}

/** Sin ViewModel, para poder componerla en un test con cualquier preferencia. */
@Composable
fun SettingsContent(
    theme: ThemePreference,
    language: LanguagePreference,
    onChooseTheme: (ThemePreference) -> Unit,
    onChooseLanguage: (LanguagePreference) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    /**
     * Si en esta plataforma elegir idioma cambia algo. Es parámetro y no una lectura directa para
     * poder componer en un test los dos casos, incluido el que la JVM del test nunca produce.
     */
    languageApplies: Boolean = platformAppliesLanguage,
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
            contentPadding = PaddingValues(top = HazloSpaces.md, bottom = HazloSpaces.xl),
            verticalArrangement = Arrangement.spacedBy(HazloSpaces.sm),
        ) {
            settingSection(
                titleRes = Res.string.settings_appearance_title,
                groupTag = SettingsTags.THEME_GROUP,
            ) {
                ThemePreference.entries.forEach { preference ->
                    SettingOption(
                        labelRes = preference.labelRes(),
                        descriptionRes = preference.descriptionRes(),
                        selected = theme == preference,
                        tag = SettingsTags.themeOption(preference),
                        onSelect = { onChooseTheme(preference) },
                    )
                }
            }

            settingSection(
                titleRes = Res.string.settings_language_title,
                groupTag = SettingsTags.LANGUAGE_GROUP,
            ) {
                LanguagePreference.entries.forEach { preference ->
                    SettingOption(
                        labelRes = preference.labelRes(),
                        descriptionRes = preference.descriptionRes(),
                        selected = language == preference,
                        tag = SettingsTags.languageOption(preference),
                        onSelect = { onChooseLanguage(preference) },
                    )
                }
            }

            // Se dice en la pantalla en vez de dejar tres opciones que se marcan y no hacen nada.
            if (!languageApplies) {
                item {
                    Text(
                        text = stringResource(Res.string.settings_language_needs_restart),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = HazloSpaces.gutter)
                            .testTag(SettingsTags.LANGUAGE_NOTICE),
                    )
                }
            }
        }
    }
}

/**
 * Un bloque de ajuste: su encabezado y su grupo de opciones excluyentes.
 *
 * Existe para que el idioma no fuera un segundo copiar-pegar del tema. El `selectableGroup` es lo
 * que hace que un lector de pantalla anuncie «1 de 3» en vez de tres interruptores sueltos.
 */
private fun LazyListScope.settingSection(
    titleRes: StringResource,
    groupTag: String,
    options: @Composable () -> Unit,
) {
    item {
        SectionHeader(
            title = stringResource(titleRes),
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
                .selectableGroup()
                .testTag(groupTag),
        ) {
            Column { options() }
        }
    }
}

@Composable
private fun SettingOption(
    labelRes: StringResource,
    descriptionRes: StringResource?,
    selected: Boolean,
    tag: String,
    onSelect: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // Toda la fila selecciona, no sólo el círculo: un objetivo de 24 dp es incómodo en un
            // teléfono y no hay nada más en la fila que pueda querer el toque.
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .padding(horizontal = HazloSpaces.md, vertical = HazloSpaces.sm)
            .testTag(tag),
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

private fun ThemePreference.labelRes(): StringResource = when (this) {
    ThemePreference.SYSTEM -> Res.string.settings_theme_system
    ThemePreference.LIGHT -> Res.string.settings_theme_light
    ThemePreference.DARK -> Res.string.settings_theme_dark
}

private fun ThemePreference.descriptionRes(): StringResource? = when (this) {
    ThemePreference.SYSTEM -> Res.string.settings_theme_system_description
    else -> null
}

private fun LanguagePreference.labelRes(): StringResource = when (this) {
    LanguagePreference.SYSTEM -> Res.string.settings_language_system
    LanguagePreference.SPANISH -> Res.string.settings_language_spanish
    LanguagePreference.ENGLISH -> Res.string.settings_language_english
}

private fun LanguagePreference.descriptionRes(): StringResource? = when (this) {
    LanguagePreference.SYSTEM -> Res.string.settings_language_system_description
    else -> null
}
