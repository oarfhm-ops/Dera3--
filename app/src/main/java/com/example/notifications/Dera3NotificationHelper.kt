package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.PhishingNotificationItem
import com.example.data.model.ThreatSeverity
import com.example.localization.AppLanguage
import com.example.localization.LocalizationManager
import com.google.firebase.Firebase
import com.google.firebase.messaging.messaging

object Dera3NotificationHelper {

    const val CHANNEL_SECURITY_ALERTS = "dera3_security_alerts"
    const val CHANNEL_SHIELD_UPDATES = "dera3_shield_updates"
    const val EXTRA_OPEN_ALERTS_SHEET = "dera3_open_alerts_sheet"
    const val FCM_TOPIC_SECURITY_ALERTS = "dera3_phishing_alerts"

    private const val TAG = "Dera3Notifications"
    private const val TEST_NOTIFICATION_ID = 9001

    fun requiresRuntimeNotificationPermission(sdkInt: Int = Build.VERSION.SDK_INT): Boolean =
        sdkInt >= Build.VERSION_CODES.TIRAMISU

    fun shouldPromptForNotificationPermission(
        sdkInt: Int,
        permissionGranted: Boolean,
        notificationsEnabledPref: Boolean,
    ): Boolean = notificationsEnabledPref && !permissionGranted && sdkInt >= Build.VERSION_CODES.TIRAMISU

    fun hasNotificationPermission(context: Context): Boolean {
        val runtimeGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        if (!runtimeGranted) return false
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun openAppNotificationSettings(context: Context) {
        val intent = Intent().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            } else {
                action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                data = android.net.Uri.fromParts("package", context.packageName, null)
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    fun ensureChannels(context: Context, language: AppLanguage) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val strings = LocalizationManager.stringsFor(language)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        val alertsChannel = NotificationChannel(
            CHANNEL_SECURITY_ALERTS,
            strings.notifPermission.channelAlertsName,
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = strings.notifPermission.channelAlertsDesc
            enableVibration(true)
            enableLights(true)
        }

        val updatesChannel = NotificationChannel(
            CHANNEL_SHIELD_UPDATES,
            strings.notifPermission.channelUpdatesName,
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = strings.notifPermission.channelUpdatesDesc
        }

        manager.createNotificationChannels(listOf(alertsChannel, updatesChannel))
    }

    fun notificationPriorityFor(severity: ThreatSeverity): Int = when (severity) {
        ThreatSeverity.CRITICAL, ThreatSeverity.HIGH -> NotificationCompat.PRIORITY_HIGH
        ThreatSeverity.MEDIUM -> NotificationCompat.PRIORITY_DEFAULT
    }

    fun formatNotificationBody(message: String, indicator: String): String {
        val cleanMsg = message.trim()
        val cleanIndicator = indicator.trim()
        return when {
            cleanIndicator.isBlank() -> cleanMsg
            cleanMsg.isBlank() -> cleanIndicator
            else -> "$cleanMsg\n$cleanIndicator"
        }
    }

    fun showSecurityAlertNotification(
        context: Context,
        item: PhishingNotificationItem,
        language: AppLanguage,
    ): Boolean {
        if (!hasNotificationPermission(context)) return false
        ensureChannels(context, language)

        val title = LocalizationManager.resolveLocalizedText(language, item.titleAr, item.titleEn)
        val message = LocalizationManager.resolveLocalizedText(language, item.messageAr, item.messageEn)
        val body = formatNotificationBody(message, item.indicator)
        val notificationId = ("alert_${item.isPersonalDetection}_${item.id}").hashCode()

        return postNotification(
            context = context,
            notificationId = notificationId,
            channelId = CHANNEL_SECURITY_ALERTS,
            title = title,
            body = body,
            priority = notificationPriorityFor(item.severity),
            openAlertsSheet = true,
        )
    }

    fun showBadgeUnlockedNotification(
        context: Context,
        badgeId: String,
        badgeTitle: String,
        language: AppLanguage,
    ): Boolean {
        if (!hasNotificationPermission(context)) return false
        ensureChannels(context, language)
        val strings = LocalizationManager.stringsFor(language)
        val body = LocalizationManager.formatString(strings.badgeNewCelebrationFormat, badgeTitle)

        return postNotification(
            context = context,
            notificationId = ("badge_$badgeId").hashCode(),
            channelId = CHANNEL_SHIELD_UPDATES,
            title = strings.notifPermission.badgeUnlockedNotifTitle,
            body = body,
            priority = NotificationCompat.PRIORITY_DEFAULT,
            openAlertsSheet = false,
        )
    }

    fun showTestAlertNotification(
        context: Context,
        language: AppLanguage,
    ): Boolean {
        if (!hasNotificationPermission(context)) return false
        ensureChannels(context, language)
        val strings = LocalizationManager.stringsFor(language)

        return postNotification(
            context = context,
            notificationId = TEST_NOTIFICATION_ID,
            channelId = CHANNEL_SECURITY_ALERTS,
            title = strings.notifPermission.testNotificationTitle,
            body = strings.notifPermission.testNotificationBody,
            priority = NotificationCompat.PRIORITY_HIGH,
            openAlertsSheet = true,
        )
    }

    fun resolveRemotePushPayload(
        data: Map<String, String>,
        notificationTitle: String?,
        notificationBody: String?,
        language: AppLanguage,
    ): Triple<String, String, ThreatSeverity> {
        val strings = LocalizationManager.stringsFor(language)
        val localizedTitle = LocalizationManager.resolveLocalizedText(
            language = language,
            arabicText = data["titleAr"].orEmpty().ifBlank { notificationTitle.orEmpty() },
            englishText = data["titleEn"].orEmpty().ifBlank { notificationTitle.orEmpty() },
        ).ifBlank { data["title"].orEmpty().ifBlank { strings.phishingAlertsTitle } }

        val localizedMessage = LocalizationManager.resolveLocalizedText(
            language = language,
            arabicText = data["messageAr"].orEmpty().ifBlank { notificationBody.orEmpty() },
            englishText = data["messageEn"].orEmpty().ifBlank { notificationBody.orEmpty() },
        ).ifBlank { data["body"].orEmpty().ifBlank { strings.notifPermission.testNotificationBody } }

        val body = formatNotificationBody(localizedMessage, data["indicator"].orEmpty())
        val severity = ThreatSeverity.fromRaw(data["severity"])
        return Triple(localizedTitle, body, severity)
    }

    fun showRemotePushNotification(
        context: Context,
        messageId: String?,
        data: Map<String, String>,
        notificationTitle: String?,
        notificationBody: String?,
        language: AppLanguage,
    ): Boolean {
        if (!hasNotificationPermission(context)) return false
        ensureChannels(context, language)

        val (title, body, severity) = resolveRemotePushPayload(
            data = data,
            notificationTitle = notificationTitle,
            notificationBody = notificationBody,
            language = language,
        )
        val stableId = (messageId ?: "${title}_${body}_${System.currentTimeMillis()}").hashCode()

        return postNotification(
            context = context,
            notificationId = stableId,
            channelId = CHANNEL_SECURITY_ALERTS,
            title = title,
            body = body,
            priority = notificationPriorityFor(severity),
            openAlertsSheet = true,
        )
    }

    fun syncFcmAlertSubscription(enabled: Boolean) {
        runCatching {
            val messaging = Firebase.messaging
            val task = if (enabled) {
                messaging.subscribeToTopic(FCM_TOPIC_SECURITY_ALERTS)
            } else {
                messaging.unsubscribeFromTopic(FCM_TOPIC_SECURITY_ALERTS)
            }
            task.addOnFailureListener { error ->
                Log.w(TAG, "FCM topic sync failed for enabled=$enabled", error)
            }
        }.onFailure { error ->
            Log.w(TAG, "FCM unavailable during topic sync", error)
        }
    }

    private fun postNotification(
        context: Context,
        notificationId: Int,
        channelId: String,
        title: String,
        body: String,
        priority: Int,
        openAlertsSheet: Boolean,
    ): Boolean {
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_ALERTS_SHEET, openAlertsSheet)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_shield_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(priority)
            .setCategory(
                if (channelId == CHANNEL_SECURITY_ALERTS) {
                    NotificationCompat.CATEGORY_ALARM
                } else {
                    NotificationCompat.CATEGORY_STATUS
                }
            )
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }
}
