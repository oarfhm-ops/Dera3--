package com.example.notifications

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.localization.AppLanguage
import com.example.localization.LocalAppLanguage
import com.example.localization.LocalStrings
import com.example.ui.theme.LocalDera3Colors

@Stable
class NotificationPermissionController(
    val isSystemPermissionGranted: Boolean,
    val requestOrOpenSettings: () -> Unit,
    val sendTestNotification: () -> Unit,
)

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun rememberNotificationPermissionController(
    onEnableNotificationsPref: () -> Unit,
    onFeedbackMessage: (String) -> Unit,
): NotificationPermissionController {
    val context = LocalContext.current
    val strings = LocalStrings.current
    val language = LocalAppLanguage.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isGranted by remember(context) {
        mutableStateOf(Dera3NotificationHelper.hasNotificationPermission(context))
    }
    var hasAskedPermission by remember { mutableStateOf(false) }
    var sendTestAfterGrant by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isGranted = Dera3NotificationHelper.hasNotificationPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasAskedPermission = true
        isGranted = granted && Dera3NotificationHelper.hasNotificationPermission(context)
        if (isGranted) {
            Dera3NotificationHelper.ensureChannels(context, language)
            onEnableNotificationsPref()
            if (sendTestAfterGrant) {
                sendTestAfterGrant = false
                if (Dera3NotificationHelper.showTestAlertNotification(context, language)) {
                    onFeedbackMessage(strings.notifPermission.testNotificationSentToast)
                }
            } else {
                onFeedbackMessage(strings.notifPermission.permissionGrantedToast)
            }
        } else {
            sendTestAfterGrant = false
            onFeedbackMessage(strings.notifPermission.permissionDeniedToast)
        }
    }

    val triggerPermissionFlow: (Boolean) -> Unit = { testAfter ->
        val currentlyGranted = Dera3NotificationHelper.hasNotificationPermission(context)
        isGranted = currentlyGranted
        if (currentlyGranted) {
            Dera3NotificationHelper.ensureChannels(context, language)
            onEnableNotificationsPref()
            if (testAfter) {
                if (Dera3NotificationHelper.showTestAlertNotification(context, language)) {
                    onFeedbackMessage(strings.notifPermission.testNotificationSentToast)
                }
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val activity = context.findActivity()
            val canShowSystemPrompt = !hasAskedPermission || (
                activity != null && ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.POST_NOTIFICATIONS,
                )
            )
            if (canShowSystemPrompt) {
                sendTestAfterGrant = testAfter
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                Dera3NotificationHelper.openAppNotificationSettings(context)
            }
        } else {
            Dera3NotificationHelper.openAppNotificationSettings(context)
        }
    }

    return NotificationPermissionController(
        isSystemPermissionGranted = isGranted,
        requestOrOpenSettings = { triggerPermissionFlow(false) },
        sendTestNotification = { triggerPermissionFlow(true) },
    )
}

@Composable
fun NotificationPermissionPromptCard(
    onAllowClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val dera3Colors = LocalDera3Colors.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("notification_permission_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.NotificationsActive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.notifPermission.permissionCardTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Surface(
                        color = dera3Colors.cautionContainer,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        Text(
                            text = strings.notifPermission.permissionDeniedBadge,
                            style = MaterialTheme.typography.labelSmall,
                            color = dera3Colors.caution,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("dismiss_notification_prompt_button"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = strings.phishingDismissBtn,
                    )
                }
            }

            Text(
                text = strings.notifPermission.permissionCardSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Button(
                onClick = onAllowClick,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("allow_notifications_button"),
            ) {
                Icon(
                    imageVector = Icons.Filled.NotificationsActive,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = strings.notifPermission.permissionAllowBtn,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
fun NotificationSystemSettingsPanel(
    isSystemPermissionGranted: Boolean,
    notificationsEnabled: Boolean,
    onAllowClick: () -> Unit,
    onSendTestClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val dera3Colors = LocalDera3Colors.current

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = if (isSystemPermissionGranted) {
                        Icons.Filled.VerifiedUser
                    } else {
                        Icons.Filled.NotificationsOff
                    },
                    contentDescription = null,
                    tint = if (isSystemPermissionGranted) dera3Colors.safe else dera3Colors.caution,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = if (isSystemPermissionGranted) {
                        strings.notifPermission.permissionGrantedBadge
                    } else {
                        strings.notifPermission.permissionDeniedBadge
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSystemPermissionGranted) dera3Colors.safe else dera3Colors.caution,
                )
            }

            if (!isSystemPermissionGranted) {
                Button(
                    onClick = onAllowClick,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("settings_allow_notifications_button"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.NotificationsActive,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(strings.notifPermission.permissionAllowBtn, fontWeight = FontWeight.Bold)
                }
            }

            if (notificationsEnabled || isSystemPermissionGranted) {
                FilledTonalButton(
                    onClick = onSendTestClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("settings_test_notification_button"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.NotificationsActive,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(strings.notifPermission.testNotificationBtn, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
