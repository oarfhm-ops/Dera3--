package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AppBlocking
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Elderly
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneMissed
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.ExtendedFloatingActionButton
import com.example.data.ai.AiAssistantState
import com.example.data.ai.ChatMessage
import com.example.data.model.AppPreferences
import com.example.data.model.PhishingCategory
import com.example.data.model.PhishingNotificationItem
import com.example.data.model.SecurityBadge
import com.example.data.model.ShieldConfig
import com.example.data.model.ShieldId
import com.example.data.model.ThreatSeverity
import com.example.data.model.UiState
import com.example.data.model.UserBadge
import com.example.data.model.UserProfile
import com.example.localization.AppLanguage
import com.example.localization.LocalLocalizationManager
import com.example.localization.LocalStrings
import com.example.localization.LocalizationManager
import com.example.notifications.NotificationPermissionPromptCard
import com.example.notifications.NotificationSystemSettingsPanel
import com.example.notifications.rememberNotificationPermissionController
import androidx.compose.material.icons.filled.Sms
import androidx.compose.ui.text.style.TextOverflow
import com.example.ui.auth.SmsVerificationBottomSheet
import com.example.ui.components.CyberThreatMonitor
import com.example.ui.components.LanguageSegmentedSelector
import com.example.ui.components.PhishingAlertBanner
import com.example.ui.components.PhishingAlertsBottomSheet
import com.example.ui.components.SafeBrowserShieldContent
import com.example.ui.components.SecurityAssistantChatSheet
import com.example.ui.components.ThreatScannerDashboard
import com.example.ui.theme.LocalDera3Colors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    userProfile: UserProfile,
    badgesState: UiState<List<UserBadge>>,
    phishingNotificationsState: UiState<List<PhishingNotificationItem>>,
    shieldConfig: ShieldConfig,
    preferences: AppPreferences,
    aiState: AiAssistantState,
    chatMessages: List<ChatMessage>,
    isChatGenerating: Boolean,
    isAdminUser: Boolean,
    isImpersonating: Boolean,
    isSavingAuthStep: Boolean = false,
    onExitImpersonation: () -> Unit,
    onOpenAdminPanel: () -> Unit,
    onToggleDarkMode: () -> Unit,
    onToggleLanguage: () -> Unit,
    onSetNotifications: (Boolean) -> Unit,
    onSetSound: (Boolean) -> Unit,
    onSignOut: () -> Unit,
    onToggleCheck: (String, Boolean) -> Unit,
    onSaveEmergencyContact: (String, () -> Unit) -> Unit,
    onTriggerHoneypot: (Boolean) -> Unit,
    onAskAi: (String) -> Unit,
    onRetryChatMessage: (ChatMessage) -> Unit,
    onClearChat: () -> Unit,
    onAcknowledgeNotification: (PhishingNotificationItem, Boolean, Boolean) -> Unit,
    onMarkAllNotificationsRead: () -> Unit,
    onReportThreat: (String, PhishingCategory, ThreatSeverity, String, String, String, String, String) -> Unit,
    onVerifyPhoneViaSms: (String) -> Unit = {},
    openAlertsOnLaunch: Boolean = false,
    onShowToastMessage: (String) -> Unit = {},
) {
    val strings = LocalStrings.current
    val dera3Colors = LocalDera3Colors.current
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var expandedShieldKey by remember { mutableStateOf<String?>(null) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showAlertsSheet by remember(openAlertsOnLaunch) { mutableStateOf(openAlertsOnLaunch) }
    var showAssistantChatSheet by remember { mutableStateOf(false) }
    var showSmsVerificationSheet by remember { mutableStateOf(false) }
    var dismissNotificationPrompt by remember { mutableStateOf(false) }

    val notificationController = rememberNotificationPermissionController(
        onEnableNotificationsPref = {
            if (!preferences.notificationsEnabled) {
                onSetNotifications(true)
            }
        },
        onFeedbackMessage = onShowToastMessage,
    )

    val notificationsList = (phishingNotificationsState as? UiState.Success)?.data.orEmpty()
    val unreadAlerts = notificationsList.filter { !it.read }
    val activeBannerAlert = unreadAlerts.firstOrNull { !it.dismissed }
    val showLiveBanner = preferences.notificationsEnabled && activeBannerAlert != null
    val showNotificationPermissionCard = !isImpersonating &&
        !notificationController.isSystemPermissionGranted &&
        !dismissNotificationPrompt

    val focusShield: (ShieldId) -> Unit = { targetShield ->
        expandedShieldKey = targetShield.key
        val headerOffset = (if (isImpersonating) 1 else 0) +
            (if (showLiveBanner) 1 else 0) +
            (if (showNotificationPermissionCard) 1 else 0) +
            5
        coroutineScope.launch {
            listState.animateScrollToItem(headerOffset + targetShield.index)
        }
    }

    val shieldIcons: List<ImageVector> = remember {
        listOf(
            Icons.Filled.Wifi,
            Icons.Filled.Chat,
            Icons.Filled.Email,
            Icons.Filled.VisibilityOff,
            Icons.Filled.Elderly,
            Icons.Filled.Link,
            Icons.Filled.BugReport,
            Icons.Filled.Password,
            Icons.Filled.AppBlocking,
            Icons.Filled.PhoneMissed,
            Icons.Filled.SmartToy,
            Icons.Filled.Public,
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = strings.appName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = strings.appTagline,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    if (isAdminUser && !isImpersonating) {
                        IconButton(
                            onClick = onOpenAdminPanel,
                            modifier = Modifier.testTag("open_admin_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AdminPanelSettings,
                                contentDescription = strings.adminPanelBtn,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    IconButton(
                        onClick = { showAlertsSheet = true },
                        modifier = Modifier.testTag("home_alerts_bell_button"),
                    ) {
                        BadgedBox(
                            badge = {
                                if (unreadAlerts.isNotEmpty()) {
                                    Badge(containerColor = dera3Colors.danger) {
                                        Text("${unreadAlerts.size.coerceAtMost(99)}")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (unreadAlerts.isNotEmpty()) {
                                    Icons.Filled.NotificationsActive
                                } else {
                                    Icons.Filled.Notifications
                                },
                                contentDescription = strings.phishingAlertsTitle,
                                tint = if (unreadAlerts.isNotEmpty()) dera3Colors.danger else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                    IconButton(
                        onClick = onToggleLanguage,
                        modifier = Modifier.testTag("home_language_toggle"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Language,
                            contentDescription = strings.switchToLanguageDesc,
                        )
                    }
                    IconButton(
                        onClick = onToggleDarkMode,
                        modifier = Modifier.testTag("home_theme_toggle"),
                    ) {
                        Icon(
                            imageVector = if (preferences.darkMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                            contentDescription = if (preferences.darkMode) strings.lightMode else strings.darkMode,
                        )
                    }
                    IconButton(
                        onClick = { showSettingsSheet = true },
                        modifier = Modifier.testTag("home_settings_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = strings.settingsTitle,
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            if (!shieldConfig.disabledShieldIds.contains(ShieldId.AI_ASSISTANT.key)) {
                ExtendedFloatingActionButton(
                    onClick = { showAssistantChatSheet = true },
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                        )
                    },
                    text = { Text(strings.securityAssistantTitle, fontWeight = FontWeight.Bold) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("home_security_assistant_fab"),
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 640.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (isImpersonating) {
                    item(key = "impersonation_banner") {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = LocalizationManager.formatString(strings.viewingAsUserFormat, userProfile.fullName),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.weight(1f),
                                )
                                Spacer(Modifier.width(8.dp))
                                Button(
                                    onClick = onExitImpersonation,
                                    modifier = Modifier.testTag("exit_impersonation_button"),
                                ) {
                                    Text(strings.returnToAdmin)
                                }
                            }
                        }
                    }
                }

                if (showLiveBanner) {
                    item(key = "phishing_live_banner") {
                        PhishingAlertBanner(
                            visible = true,
                            alert = activeBannerAlert,
                            unreadCount = unreadAlerts.size,
                            language = preferences.language,
                            onInspect = { alert ->
                                onAcknowledgeNotification(alert, true, true)
                                focusShield(alert.targetShield)
                            },
                            onViewAll = { showAlertsSheet = true },
                            onDismiss = { alert ->
                                onAcknowledgeNotification(alert, alert.read, true)
                            },
                        )
                    }
                }

                // Threat Scanner Dashboard with Circular Safety Score Gauge
                item(key = "hero_score_card") {
                    ThreatScannerDashboard(
                        userProfile = userProfile,
                        shieldConfig = shieldConfig,
                        onFocusShield = focusShield,
                    )
                }

                // User Name & Email Card right below Score Card
                item(key = "user_identity_card") {
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("user_info_card"),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = userProfile.fullName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = userProfile.email,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Surface(
                                color = if (userProfile.phoneVerified) {
                                    dera3Colors.safeContainer
                                } else {
                                    dera3Colors.cautionContainer
                                },
                                shape = RoundedCornerShape(50),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable(enabled = !isImpersonating) {
                                        showSmsVerificationSheet = true
                                    }
                                    .testTag("user_sms_status_badge"),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Sms,
                                        contentDescription = if (userProfile.phoneVerified) {
                                            strings.smsVerifiedBadge
                                        } else {
                                            strings.smsUnverifiedBadge
                                        },
                                        tint = if (userProfile.phoneVerified) dera3Colors.safe else dera3Colors.caution,
                                        modifier = Modifier.size(14.dp),
                                    )
                                    Text(
                                        text = if (userProfile.phoneVerified) {
                                            strings.smsVerifiedBadge
                                        } else {
                                            strings.smsUnverifiedBadge
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (userProfile.phoneVerified) dera3Colors.safe else dera3Colors.caution,
                                    )
                                }
                            }
                        }
                    }
                }

                if (showNotificationPermissionCard) {
                    item(key = "notification_permission_prompt") {
                        NotificationPermissionPromptCard(
                            onAllowClick = notificationController.requestOrOpenSettings,
                            onDismiss = { dismissNotificationPrompt = true },
                        )
                    }
                }

                // Security Milestone Badges (50% Cyber Defender & 100% Security Expert)
                item(key = "badges_section") {
                    val earnedList = (badgesState as? UiState.Success)?.data.orEmpty()
                    val earnedIds = earnedList.map { it.badgeId }.toSet()
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.EmojiEvents,
                                contentDescription = null,
                                tint = dera3Colors.caution,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = strings.badgesTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            SecurityBadge.entries.forEach { badge ->
                                val isUnlocked = earnedIds.contains(badge.badgeId)
                                val storedBadge = earnedList.firstOrNull { it.badgeId == badge.badgeId }
                                val title = if (preferences.language == AppLanguage.AR) {
                                    storedBadge?.titleAr?.ifBlank { badge.titleAr } ?: badge.titleAr
                                } else {
                                    storedBadge?.titleEn?.ifBlank { badge.titleEn } ?: badge.titleEn
                                }
                                val badgeIcon = if (badge == SecurityBadge.SECURITY_EXPERT) {
                                    Icons.Filled.WorkspacePremium
                                } else {
                                    Icons.Filled.VerifiedUser
                                }
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("badge_card_${badge.badgeId}"),
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isUnlocked) {
                                            dera3Colors.cautionContainer
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                        }
                                    ),
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isUnlocked) dera3Colors.caution
                                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                                                ),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                imageVector = if (isUnlocked) badgeIcon else Icons.Filled.Lock,
                                                contentDescription = title,
                                                tint = Color.White,
                                                modifier = Modifier.size(24.dp),
                                            )
                                        }
                                        Text(
                                            text = title,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isUnlocked) {
                                                dera3Colors.caution
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                        )
                                        Surface(
                                            color = if (isUnlocked) {
                                                dera3Colors.safeContainer
                                            } else {
                                                MaterialTheme.colorScheme.surface
                                            },
                                            shape = RoundedCornerShape(50),
                                        ) {
                                            Text(
                                                text = if (isUnlocked) {
                                                    "${strings.badgeUnlockedLabel} (${badge.threshold}%)"
                                                } else {
                                                    LocalizationManager.formatString(strings.badgeLockedTargetFormat, badge.threshold)
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isUnlocked) {
                                                    dera3Colors.safe
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                },
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Cyber Threat Monitor (Firestore live alerts + tailored simulated feed)
                item(key = "cyber_threat_monitor") {
                    CyberThreatMonitor(
                        userProfile = userProfile,
                        shieldConfig = shieldConfig,
                        phishingNotificationsState = phishingNotificationsState,
                        language = preferences.language,
                        onFocusShield = focusShield,
                        onAcknowledgeAlert = { item ->
                            onAcknowledgeNotification(item, true, item.dismissed)
                        },
                        onPersistSimulatedThreat = { item ->
                            onReportThreat(
                                item.targetShield.key,
                                item.category,
                                item.severity,
                                item.indicator,
                                item.titleAr,
                                item.titleEn,
                                item.messageAr,
                                item.messageEn,
                            )
                        },
                    )
                }

                item(key = "shields_header") {
                    Text(
                        text = strings.allShieldsTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }

                // All 12 Shields Sequentially
                itemsIndexed(
                    items = ShieldId.entries,
                    key = { _, shield -> shield.key },
                ) { idx, shield ->
                    val isDisabled = shieldConfig.disabledShieldIds.contains(shield.key)
                    val isExpanded = expandedShieldKey == shield.key && !isDisabled
                    val title = strings.shieldTitles.getOrElse(idx) { shield.name }
                    val subtitle = strings.shieldSubtitles.getOrElse(idx) { "" }
                    val icon = shieldIcons.getOrElse(idx) { Icons.Filled.Shield }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("shield_card_${shield.key}"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDisabled) {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (isExpanded) 4.dp else 1.dp),
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !isDisabled) {
                                        expandedShieldKey = if (isExpanded) null else shield.key
                                    }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            if (isDisabled) MaterialTheme.colorScheme.surfaceVariant
                                            else MaterialTheme.colorScheme.primaryContainer
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = title,
                                        tint = if (isDisabled) {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        } else {
                                            MaterialTheme.colorScheme.primary
                                        },
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Text(
                                            text = title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f, fill = false),
                                        )
                                        Surface(
                                            color = if (shield == ShieldId.AI_ASSISTANT) {
                                                MaterialTheme.colorScheme.primaryContainer
                                            } else {
                                                dera3Colors.safeContainer
                                            },
                                            shape = RoundedCornerShape(50),
                                        ) {
                                            Text(
                                                text = if (shield == ShieldId.AI_ASSISTANT) {
                                                    strings.onlineAiBadge
                                                } else {
                                                    strings.offlineBadge
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (shield == ShieldId.AI_ASSISTANT) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    dera3Colors.safe
                                                },
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = if (isDisabled) strings.disabledByAdmin else subtitle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isDisabled) dera3Colors.danger else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                if (!isDisabled) {
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                        contentDescription = null,
                                    )
                                }
                            }

                            AnimatedVisibility(visible = isExpanded) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                                ) {
                                    HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))
                                    when (shield) {
                                        ShieldId.WIFI -> WifiShieldContent(
                                            completedChecks = userProfile.completedChecks,
                                            onToggleCheck = onToggleCheck,
                                        )
                                        ShieldId.WHATSAPP -> WhatsAppShieldContent(
                                            completedChecks = userProfile.completedChecks,
                                            onToggleCheck = onToggleCheck,
                                        )
                                        ShieldId.BREACH -> BreachShieldContent(
                                            completedChecks = userProfile.completedChecks,
                                            onToggleCheck = onToggleCheck,
                                        )
                                        ShieldId.PRIVACY -> PrivacyShieldContent(
                                            completedChecks = userProfile.completedChecks,
                                            onToggleCheck = onToggleCheck,
                                        )
                                        ShieldId.ELDERLY -> ElderlyShieldContent(
                                            savedEmergencyContact = userProfile.emergencyContact,
                                            onSaveEmergencyContact = onSaveEmergencyContact,
                                        )
                                        ShieldId.LINK_SCANNER -> LinkScannerShieldContent(
                                            completedChecks = userProfile.completedChecks,
                                            onScanCompleted = { onToggleCheck("link_scan", true) },
                                            onPhishingDetected = { badUrl ->
                                                onReportThreat(
                                                    ShieldId.LINK_SCANNER.key,
                                                    PhishingCategory.LINK_PHISHING,
                                                    ThreatSeverity.CRITICAL,
                                                    badUrl,
                                                    "تم رصد رابط تصيد خطر",
                                                    "Phishing Link Detected",
                                                    "فحص الروابط اكتشف مؤشرات احتيال في هذا الرابط — تجنب فتحه تماماً.",
                                                    "Link Scanner detected phishing indicators in this URL — do not open it.",
                                                )
                                            },
                                        )
                                        ShieldId.HONEYPOT -> HoneypotShieldContent(
                                            completedChecks = userProfile.completedChecks,
                                            honeypotLogs = userProfile.honeypotLogs,
                                            onTriggerHoneypot = onTriggerHoneypot,
                                        )
                                        ShieldId.PASSWORD -> PasswordShieldContent(
                                            completedChecks = userProfile.completedChecks,
                                            onAwardCheck = { checkId -> onToggleCheck(checkId, true) },
                                        )
                                        ShieldId.FAKE_APPS -> FakeAppsShieldContent(
                                            completedChecks = userProfile.completedChecks,
                                            onAwardScan = { onToggleCheck("fake_app_scan", true) },
                                            onFakeAppDetected = { appName ->
                                                onReportThreat(
                                                    ShieldId.FAKE_APPS.key,
                                                    PhishingCategory.FAKE_APP,
                                                    ThreatSeverity.HIGH,
                                                    appName,
                                                    "تحذير: تطبيق مزيف أو معدّل",
                                                    "Fake or Modded App Warning",
                                                    "تم رصد اسم تطبيق غير رسمي قد يسرق رسائلك وبياناتك — احذفه فوراً.",
                                                    "Detected an unofficial or modded app name that risks account theft — uninstall immediately.",
                                                )
                                            },
                                        )
                                        ShieldId.SCAM_CALLS -> ScamCallsShieldContent(
                                            completedChecks = userProfile.completedChecks,
                                            onAwardScan = { onToggleCheck("scam_call_scan", true) },
                                            onScamCallerDetected = { caller ->
                                                onReportThreat(
                                                    ShieldId.SCAM_CALLS.key,
                                                    PhishingCategory.BANK_IMPERSONATION,
                                                    ThreatSeverity.HIGH,
                                                    caller,
                                                    "تنبيه: رقم اتصال مشبوه",
                                                    "Suspicious Caller Alert",
                                                    "الرقم خارجي أو غير مألوف — لا تشارك أي أكواد بنكية أو OTP وحظره فوراً.",
                                                    "Unknown international caller flagged — never share banking OTP codes and block immediately.",
                                                )
                                            },
                                        )
                                        ShieldId.AI_ASSISTANT -> AiAssistantShieldContent(
                                            aiState = aiState,
                                            chatMessages = chatMessages,
                                            onAskQuestion = onAskAi,
                                            onOpenFullChat = { showAssistantChatSheet = true },
                                        )
                                        ShieldId.SAFE_BROWSER -> SafeBrowserShieldContent(
                                            completedChecks = userProfile.completedChecks,
                                            onAwardUse = { onToggleCheck("safe_browser_use", true) },
                                            onPhishingBlocked = { badUrl ->
                                                onReportThreat(
                                                    ShieldId.SAFE_BROWSER.key,
                                                    PhishingCategory.LINK_PHISHING,
                                                    ThreatSeverity.HIGH,
                                                    badUrl,
                                                    "حظر رابط تصيد في المتصفح الآمن",
                                                    "Phishing URL Blocked in Safe Browser",
                                                    "المتصفح الآمن منع فتح هذا الرابط لوجود مؤشرات احتيال.",
                                                    "Safe Browser blocked this URL due to phishing indicators.",
                                                )
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item(key = "bottom_spacer") {
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }

    if (showAssistantChatSheet) {
        SecurityAssistantChatSheet(
            messages = chatMessages,
            isGenerating = isChatGenerating,
            safetyScore = userProfile.securityScore.toInt().coerceIn(0, 100),
            onSendMessage = onAskAi,
            onRetryMessage = onRetryChatMessage,
            onClearChat = onClearChat,
            onDismiss = { showAssistantChatSheet = false },
        )
    }

    if (showAlertsSheet) {
        PhishingAlertsBottomSheet(
            notificationsState = phishingNotificationsState,
            language = preferences.language,
            isSystemNotificationPermissionGranted = notificationController.isSystemPermissionGranted,
            onRequestNotificationPermission = notificationController.requestOrOpenSettings,
            onDismissSheet = { showAlertsSheet = false },
            onMarkRead = { item ->
                onAcknowledgeNotification(item, true, item.dismissed)
            },
            onMarkAllRead = onMarkAllNotificationsRead,
            onOpenShield = { item, shield ->
                showAlertsSheet = false
                onAcknowledgeNotification(item, true, true)
                focusShield(shield)
            },
        )
    }

    if (showSettingsSheet) {
        ModalBottomSheet(onDismissRequest = { showSettingsSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = strings.settingsTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Notifications, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Text(strings.notificationsLabel, modifier = Modifier.weight(1f))
                    Switch(
                        checked = preferences.notificationsEnabled,
                        onCheckedChange = { enabled ->
                            onSetNotifications(enabled)
                            if (enabled && !notificationController.isSystemPermissionGranted) {
                                notificationController.requestOrOpenSettings()
                            }
                        },
                        modifier = Modifier.testTag("settings_notifications_switch"),
                    )
                }

                NotificationSystemSettingsPanel(
                    isSystemPermissionGranted = notificationController.isSystemPermissionGranted,
                    notificationsEnabled = preferences.notificationsEnabled,
                    onAllowClick = notificationController.requestOrOpenSettings,
                    onSendTestClick = notificationController.sendTestNotification,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.VolumeUp, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Text(strings.soundLabel, modifier = Modifier.weight(1f))
                    Switch(
                        checked = preferences.soundEnabled,
                        onCheckedChange = onSetSound,
                        modifier = Modifier.testTag("settings_sound_switch"),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = if (preferences.darkMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                        contentDescription = null,
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = if (preferences.darkMode) strings.darkMode else strings.lightMode,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = preferences.darkMode,
                        onCheckedChange = { onToggleDarkMode() },
                        modifier = Modifier.testTag("settings_theme_switch"),
                    )
                }

                val localizationManager = LocalLocalizationManager.current
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Language, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = strings.chooseLanguageLabel,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )
                }
                LanguageSegmentedSelector(
                    currentLanguage = preferences.language,
                    onSelectLanguage = { selected ->
                        if (localizationManager != null) {
                            localizationManager.setLanguage(selected)
                        } else if (selected != preferences.language) {
                            onToggleLanguage()
                        }
                    },
                    modifier = Modifier.testTag("settings_language_button"),
                    arButtonTestTag = "settings_lang_ar",
                    enButtonTestTag = "settings_lang_en",
                )

                if (!isImpersonating) {
                    FilledTonalButton(
                        onClick = {
                            showSettingsSheet = false
                            showSmsVerificationSheet = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("settings_sms_verify_button"),
                    ) {
                        Icon(Icons.Filled.Sms, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(strings.smsVerifyNowBtn, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            showSettingsSheet = false
                            onSignOut()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = dera3Colors.danger),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("settings_sign_out_button"),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(strings.signOutLabel)
                    }
                }

                Spacer(Modifier.height(12.dp))
            }
        }
    }

    if (showSmsVerificationSheet && !isImpersonating) {
        SmsVerificationBottomSheet(
            initialPhoneNumber = userProfile.phoneNumber,
            isSaving = isSavingAuthStep,
            onVerified = onVerifyPhoneViaSms,
            onDismiss = { showSmsVerificationSheet = false },
        )
    }
}
