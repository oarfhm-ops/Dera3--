package com.example.notifications

import android.util.Log
import com.example.data.preferences.PreferencesRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

class Dera3FirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed FCM registration token (length=${token.length})")
        val prefsRepo = PreferencesRepository(applicationContext)
        val notificationsEnabled = runCatching {
            runBlocking {
                withTimeoutOrNull(1500L) {
                    prefsRepo.preferencesFlow.first().notificationsEnabled
                } ?: true
            }
        }.getOrDefault(true)
        Dera3NotificationHelper.syncFcmAlertSubscription(notificationsEnabled)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val prefsRepo = PreferencesRepository(applicationContext)
        val prefs = runCatching {
            runBlocking {
                withTimeoutOrNull(1500L) {
                    prefsRepo.preferencesFlow.first()
                }
            }
        }.getOrNull()

        if (prefs != null && !prefs.notificationsEnabled) {
            return
        }

        val language = prefs?.language ?: com.example.localization.AppLanguage.AR
        Dera3NotificationHelper.showRemotePushNotification(
            context = applicationContext,
            messageId = remoteMessage.messageId,
            data = remoteMessage.data,
            notificationTitle = remoteMessage.notification?.title,
            notificationBody = remoteMessage.notification?.body,
            language = language,
        )
    }

    private companion object {
        const val TAG = "Dera3FcmService"
    }
}
