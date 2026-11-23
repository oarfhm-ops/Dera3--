package com.example

import com.example.data.model.ShieldConfig
import com.example.data.model.ShieldId
import com.example.data.model.UserProfile
import com.example.data.repository.ShieldScorePoints
import com.example.domain.CallerOrigin
import com.example.domain.PasswordStrength
import com.example.domain.SecurityAnalyzers
import com.example.ui.components.buildShieldAuditSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityAnalyzersTest {

    @Test
    fun wifiAnalyzer_flagsOpenCafeKeywords() {
        assertTrue(SecurityAnalyzers.analyzeWifiName("Cafe_Free_WiFi").isDangerous)
        assertTrue(SecurityAnalyzers.analyzeWifiName("واي فاي مجاني كافيه").isDangerous)
        assertFalse(SecurityAnalyzers.analyzeWifiName("Home_Fiber_5G").isDangerous)
    }

    @Test
    fun linkAnalyzer_runsAllSixChecks() {
        val dangerous = SecurityAnalyzers.analyzeLink("http://bit.ly/free-gift")
        assertTrue(dangerous.isDangerous)
        assertEquals(6, dangerous.checks.size)
        assertFalse(dangerous.checks[0].passed) // no https
        assertFalse(dangerous.checks[1].passed) // shortener
        assertFalse(dangerous.checks[2].passed) // suspicious keywords

        val ipLink = SecurityAnalyzers.analyzeLink("https://192.168.1.10/login")
        assertTrue(ipLink.isDangerous)
        assertFalse(ipLink.checks[3].passed) // raw IP

        val typoLink = SecurityAnalyzers.analyzeLink("https://facebok-secure.com/login")
        assertTrue(typoLink.isDangerous)
        assertFalse(typoLink.checks[4].passed) // typosquatting

        val safeLink = SecurityAnalyzers.analyzeLink("https://www.google.com")
        assertFalse(safeLink.isDangerous)
        assertTrue(safeLink.checks.all { it.passed })
    }

    @Test
    fun passwordAnalyzerAndGenerator_workAccurately() {
        assertEquals(PasswordStrength.WEAK, SecurityAnalyzers.evaluatePasswordStrength("12345"))
        assertEquals(PasswordStrength.MEDIUM, SecurityAnalyzers.evaluatePasswordStrength("abcdefghij"))
        assertEquals(PasswordStrength.STRONG, SecurityAnalyzers.evaluatePasswordStrength("Dera3@2026!"))

        val generated = SecurityAnalyzers.generateStrongPassword(12)
        assertEquals(12, generated.length)
        assertEquals(PasswordStrength.STRONG, SecurityAnalyzers.evaluatePasswordStrength(generated))
    }

    @Test
    fun fakeAppAndScamCallAnalyzers_detectThreats() {
        assertTrue(SecurityAnalyzers.analyzeAppName("واتساب ذهبي").isFakeOrModded)
        assertTrue(SecurityAnalyzers.analyzeAppName("GBWhatsApp Pro").isFakeOrModded)
        assertFalse(SecurityAnalyzers.analyzeAppName("WhatsApp Messenger").isFakeOrModded)

        assertEquals(CallerOrigin.EGYPTIAN, SecurityAnalyzers.analyzeCallerNumber("01012345678"))
        assertEquals(CallerOrigin.EGYPTIAN, SecurityAnalyzers.analyzeCallerNumber("+201122334455"))
        assertEquals(CallerOrigin.FOREIGN_OR_UNKNOWN, SecurityAnalyzers.analyzeCallerNumber("+447911123456"))
    }

    @Test
    fun shieldScore_capsAt100() {
        val allIds = ShieldScorePoints.pointsByCheckId.keys.toList()
        assertEquals(100L, ShieldScorePoints.calculateScore(allIds))
        assertEquals(0L, ShieldScorePoints.calculateScore(emptyList()))
        assertEquals(15L, ShieldScorePoints.calculateScore(listOf("wifi_1", "link_scan")))
    }

    @Test
    fun threatScannerAudit_countsSecuredPausedAndNextRiskAccurately() {
        val initialAudit = buildShieldAuditSummary(
            userProfile = UserProfile(securityScore = 0L, completedChecks = emptyList()),
            shieldConfig = ShieldConfig(disabledShieldIds = listOf(ShieldId.WIFI.key)),
        )
        assertEquals(0, initialAudit.scorePercent)
        assertEquals(0, initialAudit.securedCount)
        assertEquals(11, initialAudit.atRiskCount)
        assertEquals(1, initialAudit.pausedCount)
        assertEquals(ShieldId.WHATSAPP, initialAudit.nextRiskShield)

        val partialAudit = buildShieldAuditSummary(
            userProfile = UserProfile(
                securityScore = 35L,
                completedChecks = listOf("wifi_1", "wifi_2", "wifi_3", "link_scan", "honeypot_active"),
            ),
            shieldConfig = ShieldConfig(),
        )
        assertEquals(35, partialAudit.scorePercent)
        assertEquals(3, partialAudit.securedCount)
        assertEquals(9, partialAudit.atRiskCount)
        assertEquals(0, partialAudit.pausedCount)
        assertEquals(ShieldId.WHATSAPP, partialAudit.nextRiskShield)

        val fullAudit = buildShieldAuditSummary(
            userProfile = UserProfile(
                securityScore = 100L,
                completedChecks = ShieldScorePoints.pointsByCheckId.keys.toList(),
            ),
            shieldConfig = ShieldConfig(),
        )
        assertEquals(100, fullAudit.scorePercent)
        assertEquals(12, fullAudit.securedCount)
        assertEquals(0, fullAudit.atRiskCount)
        assertNull(fullAudit.nextRiskShield)
    }
}
