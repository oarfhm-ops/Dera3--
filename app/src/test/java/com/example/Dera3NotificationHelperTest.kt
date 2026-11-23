package com.example

import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.data.model.ThreatSeverity
import com.example.localization.AppLanguage
import com.example.notifications.Dera3NotificationHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Dera3NotificationHelperTest {

    @Test
    fun requiresRuntimeNotificationPermission_onlyOnAndroid13Plus() {
        assertFalse(Dera3NotificationHelper.requiresRuntimeNotificationPermission(Build.VERSION_CODES.S_V2))
        assertTrue(Dera3NotificationHelper.requiresRuntimeNotificationPermission(Build.VERSION_CODES.TIRAMISU))
        assertTrue(Dera3NotificationHelper.requiresRuntimeNotificationPermission(Build.VERSION_CODES.UPSIDE_DOWN_CAKE))
    }

    @Test
    fun shouldPromptForNotificationPermission_checksSdkPrefAndCurrentGrant() {
        assertTrue(
            Dera3NotificationHelper.shouldPromptForNotificationPermission(
                sdkInt = Build.VERSION_CODES.TIRAMISU,
                permissionGranted = false,
                notificationsEnabledPref = true,
            )
        )
        assertFalse(
            Dera3NotificationHelper.shouldPromptForNotificationPermission(
                sdkInt = Build.VERSION_CODES.TIRAMISU,
                permissionGranted = true,
                notificationsEnabledPref = true,
            )
        )
        assertFalse(
            Dera3NotificationHelper.shouldPromptForNotificationPermission(
                sdkInt = Build.VERSION_CODES.TIRAMISU,
                permissionGranted = false,
                notificationsEnabledPref = false,
            )
        )
        assertFalse(
            Dera3NotificationHelper.shouldPromptForNotificationPermission(
                sdkInt = Build.VERSION_CODES.S,
                permissionGranted = false,
                notificationsEnabledPref = true,
            )
        )
    }

    @Test
    fun formatNotificationBody_combinesMessageAndIndicatorCleanly() {
        assertEquals(
            "Warning message\nhttp://bit.ly/scam",
            Dera3NotificationHelper.formatNotificationBody("Warning message", "http://bit.ly/scam"),
        )
        assertEquals(
            "Warning message",
            Dera3NotificationHelper.formatNotificationBody("Warning message", "   "),
        )
        assertEquals(
            "http://bit.ly/scam",
            Dera3NotificationHelper.formatNotificationBody("", "http://bit.ly/scam"),
        )
    }

    @Test
    fun notificationPriorityFor_mapsThreatSeverityAccurately() {
        assertEquals(
            NotificationCompat.PRIORITY_HIGH,
            Dera3NotificationHelper.notificationPriorityFor(ThreatSeverity.CRITICAL),
        )
        assertEquals(
            NotificationCompat.PRIORITY_HIGH,
            Dera3NotificationHelper.notificationPriorityFor(ThreatSeverity.HIGH),
        )
        assertEquals(
            NotificationCompat.PRIORITY_DEFAULT,
            Dera3NotificationHelper.notificationPriorityFor(ThreatSeverity.MEDIUM),
        )
    }

    @Test
    fun resolveRemotePushPayload_localizesDataAndFallsBackToNotificationFields() {
        val data = mapOf(
            "titleAr" to "تحذير تصيد عاجل",
            "titleEn" to "Urgent Phishing Alert",
            "messageAr" to "احذر من رابط البنك الوهمي",
            "messageEn" to "Beware of fake bank OTP link",
            "indicator" to "http://bit.ly/bank-otp",
            "severity" to "CRITICAL",
        )
        val (arTitle, arBody, arSeverity) = Dera3NotificationHelper.resolveRemotePushPayload(
            data = data,
            notificationTitle = null,
            notificationBody = null,
            language = AppLanguage.AR,
        )
        assertEquals("تحذير تصيد عاجل", arTitle)
        assertEquals("احذر من رابط البنك الوهمي\nhttp://bit.ly/bank-otp", arBody)
        assertEquals(ThreatSeverity.CRITICAL, arSeverity)

        val (enTitle, enBody, enSeverity) = Dera3NotificationHelper.resolveRemotePushPayload(
            data = emptyMap(),
            notificationTitle = "Console Alert",
            notificationBody = "Check Shield 6",
            language = AppLanguage.EN,
        )
        assertEquals("Console Alert", enTitle)
        assertEquals("Check Shield 6", enBody)
        assertEquals(ThreatSeverity.HIGH, enSeverity)
    }
}
