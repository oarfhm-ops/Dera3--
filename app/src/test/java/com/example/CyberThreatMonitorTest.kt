package com.example

import com.example.data.model.PhishingCategory
import com.example.data.model.PhishingNotificationItem
import com.example.data.model.ShieldConfig
import com.example.data.model.ShieldId
import com.example.data.model.ThreatSeverity
import com.example.data.model.UserProfile
import com.example.ui.components.CyberThreatMonitorEngine
import com.example.ui.components.ThreatFeedSource
import com.example.ui.components.ThreatFilterOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CyberThreatMonitorTest {

    @Test
    fun buildTailoredSimulatedPool_skipsDisabledShieldsAndPrioritizesIncompleteOnes() {
        val profile = UserProfile(
            userId = "u1",
            completedChecks = listOf("wifi_1", "wifi_2", "wifi_3", "link_scan"),
        )
        val config = ShieldConfig(
            disabledShieldIds = listOf(ShieldId.SCAM_CALLS.key),
        )

        val pool = CyberThreatMonitorEngine.buildTailoredSimulatedPool(
            userProfile = profile,
            shieldConfig = config,
            baseTimestampSeconds = 1_700_000_000L,
        )

        assertTrue(pool.isNotEmpty())
        assertFalse(pool.any { it.targetShield == ShieldId.SCAM_CALLS })
        // Incomplete shields must appear with isRelevantToUser = true before completed shields
        val firstNotRelevantIndex = pool.indexOfFirst { !it.isRelevantToUser }
        val lastRelevantIndex = pool.indexOfLast { it.isRelevantToUser }
        assertTrue(lastRelevantIndex in 0 until firstNotRelevantIndex)

        val wifiAlert = pool.first { it.targetShield == ShieldId.WIFI }
        assertFalse(wifiAlert.isRelevantToUser)

        val waAlert = pool.first { it.targetShield == ShieldId.WHATSAPP }
        assertTrue(waAlert.isRelevantToUser)
    }

    @Test
    fun allSimulatedTemplatesRespectFirestoreRulesLengthConstraints() {
        val pool = CyberThreatMonitorEngine.buildTailoredSimulatedPool(
            userProfile = UserProfile(userId = "u1"),
            shieldConfig = ShieldConfig(),
            baseTimestampSeconds = 1_700_000_000L,
        )
        pool.forEach { item ->
            assertTrue(item.titleAr.isNotBlank() && item.titleAr.length <= 120)
            assertTrue(item.titleEn.isNotBlank() && item.titleEn.length <= 120)
            assertTrue(item.messageAr.isNotBlank() && item.messageAr.length <= 500)
            assertTrue(item.messageEn.isNotBlank() && item.messageEn.length <= 500)
            assertTrue(item.indicator.isNotBlank() && item.indicator.length <= 180)
        }
    }

    @Test
    fun mergeAndFilterFeed_combinesFirestoreAndSimulatedAlertsDeduplicatesAndFilters() {
        val profile = UserProfile(
            userId = "u1",
            completedChecks = listOf("wifi_1", "wifi_2", "wifi_3"),
        )
        val config = ShieldConfig()

        val simulated = CyberThreatMonitorEngine.buildTailoredSimulatedPool(
            userProfile = profile,
            shieldConfig = config,
            baseTimestampSeconds = 1_700_000_100L,
        )
        val duplicateIndicator = simulated.first { it.targetShield == ShieldId.LINK_SCANNER }.indicator

        val firestoreAlerts = listOf(
            PhishingNotificationItem(
                id = "cloud_1",
                isPersonalDetection = false,
                titleAr = "تنبيه تصيد بنكي",
                titleEn = "Bank Phishing Campaign",
                messageAr = "انتحال خدمة عملاء البنوك",
                messageEn = "Active bank impersonation alerts",
                category = PhishingCategory.BANK_IMPERSONATION,
                severity = ThreatSeverity.CRITICAL,
                indicator = "https://bank-otp-fake.example",
                targetShield = ShieldId.SCAM_CALLS,
                read = false,
                dismissed = false,
                timestampSeconds = 1_700_000_200L,
            ),
            PhishingNotificationItem(
                id = "pers_1",
                isPersonalDetection = true,
                titleAr = "رصد محلي محفوظ",
                titleEn = "Saved Detection",
                messageAr = "تم حفظ المؤشر في Firestore",
                messageEn = "Persisted indicator in Firestore",
                category = PhishingCategory.SMS_DELIVERY,
                severity = ThreatSeverity.HIGH,
                indicator = duplicateIndicator,
                targetShield = ShieldId.LINK_SCANNER,
                read = true,
                dismissed = false,
                timestampSeconds = 1_700_000_050L,
            ),
        )

        val allMerged = CyberThreatMonitorEngine.mergeAndFilterFeed(
            firestoreAlerts = firestoreAlerts,
            simulatedAlerts = simulated,
            filter = ThreatFilterOption.ALL,
            userProfile = profile,
            shieldConfig = config,
        )

        // Duplicate simulated indicator matching Firestore personal detection should be deduplicated
        val matchingIndicatorCount = allMerged.count { it.indicator == duplicateIndicator }
        assertEquals(1, matchingIndicatorCount)
        assertEquals(
            ThreatFeedSource.FIRESTORE_PERSONAL,
            allMerged.first { it.indicator == duplicateIndicator }.source,
        )

        // Check filter: CRITICAL_HIGH
        val criticalOnly = CyberThreatMonitorEngine.mergeAndFilterFeed(
            firestoreAlerts = firestoreAlerts,
            simulatedAlerts = simulated,
            filter = ThreatFilterOption.CRITICAL_HIGH,
            userProfile = profile,
            shieldConfig = config,
        )
        assertTrue(criticalOnly.all { it.severity == ThreatSeverity.CRITICAL || it.severity == ThreatSeverity.HIGH })

        // Check filter: RELEVANT_TO_ME excludes WIFI since all wifi_* checks are done
        val relevantOnly = CyberThreatMonitorEngine.mergeAndFilterFeed(
            firestoreAlerts = firestoreAlerts,
            simulatedAlerts = simulated,
            filter = ThreatFilterOption.RELEVANT_TO_ME,
            userProfile = profile,
            shieldConfig = config,
        )
        assertTrue(relevantOnly.all { it.isRelevantToUser })
        assertFalse(relevantOnly.any { it.targetShield == ShieldId.WIFI && it.source == ThreatFeedSource.SIMULATED_LIVE })
    }
}
