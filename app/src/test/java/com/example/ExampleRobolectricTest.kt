package com.example

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.example.localization.AppLanguage
import com.example.notifications.Dera3NotificationHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("DERA3", appName)
    }

    @Test
    fun `package name matches applicationId`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("com.dera3.app", context.packageName)
    }

    @Test
    fun `notification channels are registered with localized metadata`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        Dera3NotificationHelper.ensureChannels(context, AppLanguage.AR)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val alertsChannel = manager.getNotificationChannel(Dera3NotificationHelper.CHANNEL_SECURITY_ALERTS)
            val updatesChannel = manager.getNotificationChannel(Dera3NotificationHelper.CHANNEL_SHIELD_UPDATES)
            assertNotNull(alertsChannel)
            assertNotNull(updatesChannel)
            assertEquals(NotificationManager.IMPORTANCE_HIGH, alertsChannel.importance)
            assertTrue(alertsChannel.name.isNotBlank())
        }
    }
}
