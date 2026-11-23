package com.example

import androidx.compose.ui.unit.LayoutDirection
import com.example.localization.AppLanguage
import com.example.localization.ArabicStrings
import com.example.localization.Dera3Strings
import com.example.localization.EnglishStrings
import com.example.localization.LocalizationManager
import com.example.localization.NotificationStrings
import com.example.localization.SafeBrowserStrings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalizationManagerTest {

    @Test
    fun stringsForReturnsCorrectBundleAndDirections() {
        assertEquals(ArabicStrings, LocalizationManager.stringsFor(AppLanguage.AR))
        assertEquals(EnglishStrings, LocalizationManager.stringsFor(AppLanguage.EN))

        assertEquals(LayoutDirection.Rtl, LocalizationManager.layoutDirectionFor(AppLanguage.AR))
        assertEquals(LayoutDirection.Ltr, LocalizationManager.layoutDirectionFor(AppLanguage.EN))

        assertEquals("ar-EG", LocalizationManager.localeTagFor(AppLanguage.AR))
        assertEquals("en-US", LocalizationManager.localeTagFor(AppLanguage.EN))
    }

    @Test
    fun buildStateAndNextLanguageSwitchBetweenArabicAndEnglish() {
        val arState = LocalizationManager.buildState(AppLanguage.AR)
        assertEquals(AppLanguage.AR, arState.language)
        assertEquals(LayoutDirection.Rtl, arState.layoutDirection)
        assertEquals("درعك الرقمي", arState.strings.appTagline)

        val next = LocalizationManager.nextLanguage(arState.language)
        assertEquals(AppLanguage.EN, next)

        val enState = LocalizationManager.buildState(next)
        assertEquals(AppLanguage.EN, enState.language)
        assertEquals(LayoutDirection.Ltr, enState.layoutDirection)
        assertEquals("Your Digital Shield", enState.strings.appTagline)
        assertEquals(AppLanguage.AR, LocalizationManager.nextLanguage(enState.language))
    }

    @Test
    fun resolveLocalizedTextFallsBackWhenOneTranslationIsBlank() {
        assertEquals("تنبيه", LocalizationManager.resolveLocalizedText(AppLanguage.AR, "تنبيه", "Alert"))
        assertEquals("Alert", LocalizationManager.resolveLocalizedText(AppLanguage.EN, "تنبيه", "Alert"))
        assertEquals("Alert", LocalizationManager.resolveLocalizedText(AppLanguage.AR, "   ", "Alert"))
        assertEquals("تنبيه", LocalizationManager.resolveLocalizedText(AppLanguage.EN, "تنبيه", ""))
    }

    @Test
    fun formatStringKeepsWesternDigitsConsistentInBothLanguages() {
        assertEquals(
            "+10 نقطة",
            LocalizationManager.formatString(ArabicStrings.ptsFormat, 10),
        )
        assertEquals(
            "+10 pts",
            LocalizationManager.formatString(EnglishStrings.ptsFormat, 10),
        )
        assertEquals(
            "(+5 نقطة)",
            LocalizationManager.formatString(ArabicStrings.ptsInlineFormat, 5),
        )
        assertEquals(
            "(+5 pts)",
            LocalizationManager.formatString(EnglishStrings.ptsInlineFormat, 5),
        )
    }

    @Test
    fun arabicAndEnglishStringsHaveCompleteNonBlankParity() {
        fun verifyParity(clazz: Class<*>, arObj: Any, enObj: Any) {
            val fields = clazz.declaredFields
            assertTrue("Expected ${clazz.simpleName} to have fields", fields.isNotEmpty())
            for (field in fields) {
                field.isAccessible = true
                val arVal = field.get(arObj)
                val enVal = field.get(enObj)
                when (arVal) {
                    is String -> {
                        assertTrue("Arabic field ${field.name} should not be blank", arVal.isNotBlank())
                        assertTrue(
                            "English field ${field.name} should not be blank",
                            (enVal as String).isNotBlank(),
                        )
                    }
                    is List<*> -> {
                        val enList = enVal as List<*>
                        assertEquals(
                            "List size mismatch for ${field.name}",
                            arVal.size,
                            enList.size,
                        )
                        assertTrue("List ${field.name} should not be empty", arVal.isNotEmpty())
                        arVal.forEach { item ->
                            assertTrue("Arabic list item in ${field.name} blank", item.toString().isNotBlank())
                        }
                        enList.forEach { item ->
                            assertTrue("English list item in ${field.name} blank", item.toString().isNotBlank())
                        }
                    }
                    is SafeBrowserStrings -> {
                        verifyParity(SafeBrowserStrings::class.java, arVal, enVal as SafeBrowserStrings)
                    }
                    is NotificationStrings -> {
                        verifyParity(NotificationStrings::class.java, arVal, enVal as NotificationStrings)
                    }
                }
            }
        }
        verifyParity(Dera3Strings::class.java, ArabicStrings, EnglishStrings)
        assertEquals(12, ArabicStrings.shieldTitles.size)
        assertEquals(12, ArabicStrings.shieldSubtitles.size)
    }
}
