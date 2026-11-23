package com.example.localization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.preferences.PreferencesRepository
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class LocalizationState(
    val language: AppLanguage = AppLanguage.AR,
    val strings: Dera3Strings = ArabicStrings,
    val layoutDirection: LayoutDirection = LayoutDirection.Rtl,
    val localeTag: String = "ar-EG",
)

class LocalizationManager(
    private val preferencesRepository: PreferencesRepository,
    private val scope: CoroutineScope,
) {
    val state: StateFlow<LocalizationState> = preferencesRepository.preferencesFlow
        .map { prefs -> buildState(prefs.language) }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = buildState(AppLanguage.AR),
        )

    val currentLanguage: AppLanguage
        get() = state.value.language

    val currentStrings: Dera3Strings
        get() = state.value.strings

    fun setLanguage(language: AppLanguage) {
        if (state.value.language == language) return
        scope.launch {
            preferencesRepository.setLanguage(language)
        }
    }

    fun toggleLanguage() {
        val next = nextLanguage(state.value.language)
        scope.launch {
            preferencesRepository.setLanguage(next)
        }
    }

    fun getLocalizedText(arabicText: String, englishText: String): String =
        resolveLocalizedText(state.value.language, arabicText, englishText)

    fun format(selector: (Dera3Strings) -> String, vararg args: Any): String =
        formatString(selector(state.value.strings), *args)

    companion object {
        fun stringsFor(language: AppLanguage): Dera3Strings = when (language) {
            AppLanguage.AR -> ArabicStrings
            AppLanguage.EN -> EnglishStrings
        }

        fun layoutDirectionFor(language: AppLanguage): LayoutDirection = when (language) {
            AppLanguage.AR -> LayoutDirection.Rtl
            AppLanguage.EN -> LayoutDirection.Ltr
        }

        fun localeTagFor(language: AppLanguage): String = when (language) {
            AppLanguage.AR -> "ar-EG"
            AppLanguage.EN -> "en-US"
        }

        fun localeFor(language: AppLanguage): Locale =
            Locale.forLanguageTag(localeTagFor(language))

        fun nextLanguage(current: AppLanguage): AppLanguage =
            if (current == AppLanguage.AR) AppLanguage.EN else AppLanguage.AR

        fun buildState(language: AppLanguage): LocalizationState = LocalizationState(
            language = language,
            strings = stringsFor(language),
            layoutDirection = layoutDirectionFor(language),
            localeTag = localeTagFor(language),
        )

        fun resolveLocalizedText(
            language: AppLanguage,
            arabicText: String,
            englishText: String,
        ): String = when (language) {
            AppLanguage.AR -> arabicText.ifBlank { englishText }
            AppLanguage.EN -> englishText.ifBlank { arabicText }
        }

        fun formatString(template: String, vararg args: Any): String =
            String.format(Locale.US, template, *args)
    }
}

val LocalLocalizationManager = staticCompositionLocalOf<LocalizationManager?> { null }
val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.AR }

@Composable
fun ProvideDera3Localization(
    localizationManager: LocalizationManager,
    content: @Composable () -> Unit,
) {
    val locState by localizationManager.state.collectAsStateWithLifecycle()

    SideEffect {
        Locale.setDefault(LocalizationManager.localeFor(locState.language))
    }

    CompositionLocalProvider(
        LocalLocalizationManager provides localizationManager,
        LocalAppLanguage provides locState.language,
        LocalStrings provides locState.strings,
        LocalLayoutDirection provides locState.layoutDirection,
    ) {
        content()
    }
}
