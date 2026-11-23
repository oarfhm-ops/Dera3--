package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AppBlocking
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhoneMissed
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SmsFailed
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.PhishingCategory
import com.example.data.model.PhishingNotificationItem
import com.example.data.model.ShieldId
import com.example.data.model.ThreatSeverity
import com.example.data.model.UiState
import com.example.localization.AppLanguage
import com.example.localization.LocalStrings
import com.example.localization.LocalizationManager
import com.example.ui.theme.LocalDera3Colors
import com.example.ui.theme.MonoFontFamily

@Composable
fun PhishingAlertBanner(
    visible: Boolean,
    alert: PhishingNotificationItem?,
    unreadCount: Int,
    language: AppLanguage,
    onInspect: (PhishingNotificationItem) -> Unit,
    onViewAll: () -> Unit,
    onDismiss: (PhishingNotificationItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val dera3Colors = LocalDera3Colors.current

    AnimatedVisibility(
        visible = visible && alert != null,
        enter = fadeIn(tween(250)) + expandVertically(),
        exit = fadeOut(tween(200)) + shrinkVertically(),
        modifier = modifier,
    ) {
        if (alert != null) {
            val isMedium = alert.severity == ThreatSeverity.MEDIUM
            val accentColor = if (isMedium) dera3Colors.caution else dera3Colors.danger
            val containerColor = if (isMedium) dera3Colors.cautionContainer else dera3Colors.dangerContainer
            val title = LocalizationManager.resolveLocalizedText(language, alert.titleAr, alert.titleEn)
            val message = LocalizationManager.resolveLocalizedText(language, alert.messageAr, alert.messageEn)

            val pulseTransition = rememberInfiniteTransition(label = "phishing_banner_pulse")
            val pulseAlpha by pulseTransition.animateFloat(
                initialValue = 0.35f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 850),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "pulse_alpha",
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("phishing_live_banner"),
                shape = RoundedCornerShape(20.dp),
                color = containerColor,
                border = BorderStroke(1.5.dp, accentColor.copy(alpha = 0.65f)),
                shadowElevation = 4.dp,
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .alpha(pulseAlpha)
                                .clip(CircleShape)
                                .background(accentColor),
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Filled.NotificationsActive,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = strings.phishingLiveBannerTag,
                            style = MaterialTheme.typography.labelLarge,
                            color = accentColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                        )
                        if (unreadCount > 1) {
                            Surface(
                                color = accentColor,
                                shape = RoundedCornerShape(50),
                            ) {
                                Text(
                                    text = "+${unreadCount - 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                )
                            }
                        }
                        IconButton(
                            onClick = { onDismiss(alert) },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("phishing_banner_dismiss_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = strings.phishingDismissBtn,
                                tint = accentColor,
                            )
                        }
                    }

                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )

                    if (alert.indicator.isNotBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Text(
                                text = alert.indicator,
                                style = MaterialTheme.typography.labelLarge.copy(fontFamily = MonoFontFamily),
                                color = accentColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = { onInspect(alert) },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("phishing_banner_inspect_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Security,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = strings.phishingInspectBtn,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        OutlinedButton(
                            onClick = onViewAll,
                            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("phishing_banner_view_all_button"),
                        ) {
                            Text(
                                text = strings.phishingViewAllBtn,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhishingAlertsBottomSheet(
    notificationsState: UiState<List<PhishingNotificationItem>>,
    language: AppLanguage,
    isSystemNotificationPermissionGranted: Boolean = true,
    onRequestNotificationPermission: () -> Unit = {},
    onDismissSheet: () -> Unit,
    onMarkRead: (PhishingNotificationItem) -> Unit,
    onMarkAllRead: () -> Unit,
    onOpenShield: (PhishingNotificationItem, ShieldId) -> Unit,
) {
    val strings = LocalStrings.current
    val dera3Colors = LocalDera3Colors.current
    val itemsList = (notificationsState as? UiState.Success)?.data.orEmpty()
    val hasUnread = itemsList.any { !it.read }

    ModalBottomSheet(
        onDismissRequest = onDismissSheet,
        modifier = Modifier.testTag("phishing_alerts_sheet"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(dera3Colors.dangerContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.WarningAmber,
                            contentDescription = null,
                            tint = dera3Colors.danger,
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = strings.phishingAlertsTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = strings.phishingAlertsSubtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                if (hasUnread) {
                    TextButton(
                        onClick = onMarkAllRead,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("phishing_mark_all_read_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DoneAll,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(strings.phishingMarkAllReadBtn)
                    }
                }
            }

            HorizontalDivider()

            if (!isSystemNotificationPermissionGranted) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = strings.notifPermission.permissionCardTitle,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                        )
                        Button(
                            onClick = onRequestNotificationPermission,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("alerts_sheet_allow_notifications_button"),
                        ) {
                            Text(strings.notifPermission.permissionAllowBtn)
                        }
                    }
                }
            }

            when (notificationsState) {
                UiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is UiState.Error -> {
                    Surface(
                        color = dera3Colors.dangerContainer,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = notificationsState.message,
                            color = dera3Colors.danger,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(14.dp),
                        )
                    }
                }
                is UiState.Success -> {
                    if (itemsList.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 28.dp, horizontal = 12.dp)
                                .testTag("phishing_alerts_empty"),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(dera3Colors.safeContainer),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.VerifiedUser,
                                    contentDescription = null,
                                    tint = dera3Colors.safe,
                                    modifier = Modifier.size(28.dp),
                                )
                            }
                            Text(
                                text = strings.phishingEmptyTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = strings.phishingEmptySubtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 460.dp),
                            contentPadding = PaddingValues(bottom = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(
                                items = itemsList,
                                key = { "${it.isPersonalDetection}_${it.id}" },
                            ) { item ->
                                PhishingAlertCard(
                                    item = item,
                                    language = language,
                                    onMarkRead = { onMarkRead(item) },
                                    onOpenShield = { onOpenShield(item, item.targetShield) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PhishingAlertCard(
    item: PhishingNotificationItem,
    language: AppLanguage,
    onMarkRead: () -> Unit,
    onOpenShield: () -> Unit,
) {
    val strings = LocalStrings.current
    val dera3Colors = LocalDera3Colors.current
    val isMedium = item.severity == ThreatSeverity.MEDIUM
    val accentColor = if (isMedium) dera3Colors.caution else dera3Colors.danger
    val badgeBg = if (isMedium) dera3Colors.cautionContainer else dera3Colors.dangerContainer
    val severityText = when (item.severity) {
        ThreatSeverity.CRITICAL -> strings.phishingSeverityCritical
        ThreatSeverity.HIGH -> strings.phishingSeverityHigh
        ThreatSeverity.MEDIUM -> strings.phishingSeverityMedium
    }
    val categoryIcon: ImageVector = when (item.category) {
        PhishingCategory.LINK_PHISHING -> Icons.Filled.Link
        PhishingCategory.BANK_IMPERSONATION -> Icons.Filled.PhoneMissed
        PhishingCategory.FAKE_APP -> Icons.Filled.AppBlocking
        PhishingCategory.OTP_SCAM -> Icons.Filled.Security
        PhishingCategory.SMS_DELIVERY -> Icons.Filled.SmsFailed
    }
    val title = LocalizationManager.resolveLocalizedText(language, item.titleAr, item.titleEn)
    val message = LocalizationManager.resolveLocalizedText(language, item.messageAr, item.messageEn)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("phishing_alert_card_${item.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.read) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (!item.read) BorderStroke(1.dp, accentColor.copy(alpha = 0.5f)) else null,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(badgeBg),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = categoryIcon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp),
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = if (item.isPersonalDetection) strings.phishingSourceDetected else strings.phishingSourceAdvisory,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Surface(
                    color = badgeBg,
                    shape = RoundedCornerShape(50),
                ) {
                    Text(
                        text = severityText,
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (item.indicator.isNotBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = item.indicator,
                        style = MaterialTheme.typography.labelLarge.copy(fontFamily = MonoFontFamily),
                        color = accentColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!item.read) {
                    OutlinedButton(
                        onClick = onMarkRead,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("phishing_card_mark_read_${item.id}"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(strings.phishingMarkReadBtn)
                    }
                } else {
                    Surface(
                        color = dera3Colors.safeContainer,
                        shape = RoundedCornerShape(50),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = dera3Colors.safe,
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                text = strings.phishingReadBadge,
                                style = MaterialTheme.typography.labelSmall,
                                color = dera3Colors.safe,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    Spacer(Modifier.weight(1f))
                }

                Button(
                    onClick = onOpenShield,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("phishing_card_open_shield_${item.id}"),
                ) {
                    Text(
                        text = strings.phishingOpenShieldBtn,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}
