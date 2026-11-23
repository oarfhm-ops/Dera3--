package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import com.example.data.model.AppPreferences
import com.example.data.model.PhishingAlert
import com.example.data.model.PhishingCategory
import com.example.data.model.ShieldConfig
import com.example.data.model.ShieldId
import com.example.data.model.ThreatSeverity
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.localization.AppLanguage
import com.example.localization.LocalStrings
import com.example.localization.LocalizationManager
import com.example.ui.theme.LocalDera3Colors
import com.example.ui.theme.MonoFontFamily

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AdminScreen(
    allUsersState: UiState<List<UserProfile>>,
    adminPhishingAlertsState: UiState<List<PhishingAlert>>,
    shieldConfig: ShieldConfig,
    preferences: AppPreferences,
    hasOwnProfile: Boolean,
    onBackToMyHome: () -> Unit,
    onEnterAsUser: (UserProfile) -> Unit,
    onToggleShield: (String, Boolean) -> Unit,
    onPublishPhishingAlert: (String, String, String, String, PhishingCategory, ThreatSeverity, String) -> Unit,
    onTogglePhishingAlertActive: (String, Boolean) -> Unit,
    onDeletePhishingAlert: (String) -> Unit,
    onToggleDarkMode: () -> Unit,
    onToggleLanguage: () -> Unit,
    onSignOut: () -> Unit,
) {
    val strings = LocalStrings.current
    val dera3Colors = LocalDera3Colors.current
    var searchQuery by remember { mutableStateOf("") }
    var alertTitleAr by remember { mutableStateOf("") }
    var alertTitleEn by remember { mutableStateOf("") }
    var alertMsgAr by remember { mutableStateOf("") }
    var alertMsgEn by remember { mutableStateOf("") }
    var alertIndicator by remember { mutableStateOf("") }
    var alertCategory by remember { mutableStateOf(PhishingCategory.LINK_PHISHING) }
    var alertSeverity by remember { mutableStateOf(ThreatSeverity.HIGH) }

    BackHandler { onBackToMyHome() }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = onBackToMyHome,
                        modifier = Modifier.testTag("admin_back_button"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = strings.backLabel,
                        )
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.AdminPanelSettings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = strings.adminPanelTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onToggleLanguage,
                        modifier = Modifier.testTag("admin_language_toggle"),
                    ) {
                        Icon(Icons.Filled.Language, contentDescription = strings.switchToLanguageDesc)
                    }
                    IconButton(
                        onClick = onToggleDarkMode,
                        modifier = Modifier.testTag("admin_theme_toggle"),
                    ) {
                        Icon(
                            imageVector = if (preferences.darkMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                            contentDescription = if (preferences.darkMode) strings.lightMode else strings.darkMode,
                        )
                    }
                    IconButton(
                        onClick = onSignOut,
                        modifier = Modifier.testTag("admin_sign_out_button"),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = strings.signOutLabel)
                    }
                },
            )
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
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 640.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Live Total Registered Users Card
                item(key = "admin_kpi_card") {
                    val userCount = (allUsersState as? UiState.Success)?.data?.size ?: 0
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("admin_total_users_card"),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Group,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp),
                            )
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = strings.totalUsersLabel,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                                Text(
                                    text = "$userCount",
                                    style = MaterialTheme.typography.displaySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }

                // Search Users Input
                item(key = "admin_search") {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text(strings.searchUsersHint) },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("admin_search_input"),
                    )
                }

                // Registered Users List
                when (allUsersState) {
                    UiState.Loading -> {
                        item(key = "admin_users_loading") {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                    is UiState.Error -> {
                        item(key = "admin_users_error") {
                            StatusBanner(text = allUsersState.message, isDanger = true)
                        }
                    }
                    is UiState.Success -> {
                        val filtered = allUsersState.data.filter { user ->
                            val q = searchQuery.trim().lowercase()
                            q.isEmpty() ||
                                user.fullName.lowercase().contains(q) ||
                                user.email.lowercase().contains(q)
                        }
                        if (filtered.isEmpty()) {
                            item(key = "admin_users_empty") {
                                Text(
                                    text = strings.noResultsFound,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 12.dp),
                                )
                            }
                        } else {
                            items(
                                items = filtered,
                                key = { it.userId.ifEmpty { it.id } },
                            ) { user ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("admin_user_card_${user.userId}"),
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text(
                                                text = user.fullName,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                            )
                                            Surface(
                                                color = dera3Colors.safeContainer,
                                                shape = RoundedCornerShape(50),
                                            ) {
                                                Text(
                                                    text = "${user.securityScore}/100",
                                                    color = dera3Colors.safe,
                                                    style = MaterialTheme.typography.labelLarge,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                )
                                            }
                                        }
                                        Text(
                                            text = user.email,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Text(
                                            text = "${strings.phoneLabel}: ${user.phoneNumber.ifBlank { "-" }}",
                                            style = MaterialTheme.typography.bodyMedium,
                                        )
                                        Text(
                                            text = "${strings.adminPasswordLabel}: ${user.passwordMasked.ifBlank { "••••••••" }}",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = MonoFontFamily),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Button(
                                            onClick = { onEnterAsUser(user) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(48.dp)
                                                .testTag("enter_as_user_${user.userId}"),
                                        ) {
                                            Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text(strings.enterAsUserBtn)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Enable/Disable 12 Shields Controls
                item(key = "admin_shields_header") {
                    Text(
                        text = strings.shieldsControlTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }

                itemsIndexed(
                    items = ShieldId.entries,
                    key = { _, s -> "toggle_${s.key}" },
                ) { idx, shield ->
                    val enabled = !shieldConfig.disabledShieldIds.contains(shield.key)
                    val title = strings.shieldTitles.getOrElse(idx) { shield.name }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = if (enabled) strings.shieldEnabled else strings.shieldDisabled,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (enabled) dera3Colors.safe else dera3Colors.danger,
                                )
                            }
                            Switch(
                                checked = enabled,
                                onCheckedChange = { onToggleShield(shield.key, it) },
                                modifier = Modifier.testTag("admin_shield_switch_${shield.key}"),
                            )
                        }
                    }
                }

                // Real-Time Phishing Scam Alert Broadcaster
                item(key = "admin_phishing_broadcaster") {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_phishing_broadcaster_card"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Campaign,
                                    contentDescription = null,
                                    tint = dera3Colors.danger,
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = strings.adminAlertsSectionTitle,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        text = strings.adminAlertsSectionSubtitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }

                            Text(
                                text = strings.adminQuickTemplatesLabel,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                            )

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                val presets = listOf(
                                    Triple(
                                        strings.adminPresetBank,
                                        PhishingCategory.BANK_IMPERSONATION to ThreatSeverity.CRITICAL,
                                        listOf(
                                            "تحذير: مكالمات انتحال البنوك لسرقة كود OTP",
                                            "Alert: Fake Bank Calls Stealing OTP Codes",
                                            "محتالون يتصلون مدعين أنهم من البنك لطلب كود التفعيل أو بيانات الفيزا — اقفل المكالمة فوراً.",
                                            "Scammers call pretending to be bank support asking for card PINs or OTP codes — hang up immediately.",
                                            "Caller asks for 6-digit OTP",
                                        ),
                                    ),
                                    Triple(
                                        strings.adminPresetWallet,
                                        PhishingCategory.LINK_PHISHING to ThreatSeverity.HIGH,
                                        listOf(
                                            "تحذير: روابط جوائز ومساعدات مالية وهمية",
                                            "Alert: Fake Cash Prize & Wallet Links",
                                            "تنتشر روابط مختصرة تدّعي ربح جوائز أو تحديث المحفظة الإلكترونية لسرقة حسابك.",
                                            "Shortened phishing URLs claim cash rewards or wallet updates to steal your mobile wallet PIN.",
                                            "http://bit.ly/free-wallet-gift",
                                        ),
                                    ),
                                    Triple(
                                        strings.adminPresetDelivery,
                                        PhishingCategory.SMS_DELIVERY to ThreatSeverity.HIGH,
                                        listOf(
                                            "تحذير: رسائل SMS مزيفة لتوصيل الشحنات",
                                            "Alert: Fake Parcel Delivery Fee SMS",
                                            "رسالة تطلب دفع رسوم رمزية عبر رابط مشبوه لاستلام طرد بريدي — رابط تصيد لسرقة البطاقة.",
                                            "SMS asks for a small customs/delivery fee via an untrusted link to steal payment card details.",
                                            "track-post-fee-pay.xyz",
                                        ),
                                    ),
                                    Triple(
                                        strings.adminPresetWhatsappGold,
                                        PhishingCategory.FAKE_APP to ThreatSeverity.HIGH,
                                        listOf(
                                            "تحذير: نسخة واتساب الذهبي APK المزيفة",
                                            "Alert: Fake WhatsApp Gold Mod APK",
                                            "روابط تحميل واتساب الذهبي خارج المتجر الرسمي تسرق محادثاتك وجهات اتصالك.",
                                            "Sideloaded WhatsApp Gold APKs steal chat history and contacts — use official store only.",
                                            "WhatsApp_Gold_Update.apk",
                                        ),
                                    ),
                                )
                                presets.forEachIndexed { idx, (label, meta, texts) ->
                                    AssistChip(
                                        onClick = {
                                            alertCategory = meta.first
                                            alertSeverity = meta.second
                                            alertTitleAr = texts[0]
                                            alertTitleEn = texts[1]
                                            alertMsgAr = texts[2]
                                            alertMsgEn = texts[3]
                                            alertIndicator = texts[4]
                                        },
                                        label = { Text(label) },
                                        modifier = Modifier.testTag("admin_alert_preset_$idx"),
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = alertTitleAr,
                                onValueChange = { alertTitleAr = it },
                                label = { Text(strings.adminAlertTitleArHint) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("admin_alert_title_ar_input"),
                            )
                            OutlinedTextField(
                                value = alertTitleEn,
                                onValueChange = { alertTitleEn = it },
                                label = { Text(strings.adminAlertTitleEnHint) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("admin_alert_title_en_input"),
                            )
                            OutlinedTextField(
                                value = alertMsgAr,
                                onValueChange = { alertMsgAr = it },
                                label = { Text(strings.adminAlertMsgArHint) },
                                modifier = Modifier.fillMaxWidth().testTag("admin_alert_msg_ar_input"),
                            )
                            OutlinedTextField(
                                value = alertMsgEn,
                                onValueChange = { alertMsgEn = it },
                                label = { Text(strings.adminAlertMsgEnHint) },
                                modifier = Modifier.fillMaxWidth().testTag("admin_alert_msg_en_input"),
                            )
                            OutlinedTextField(
                                value = alertIndicator,
                                onValueChange = { alertIndicator = it },
                                label = { Text(strings.adminAlertIndicatorHint) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("admin_alert_indicator_input"),
                            )

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                ThreatSeverity.entries.forEach { sev ->
                                    val sevLabel = when (sev) {
                                        ThreatSeverity.CRITICAL -> strings.phishingSeverityCritical
                                        ThreatSeverity.HIGH -> strings.phishingSeverityHigh
                                        ThreatSeverity.MEDIUM -> strings.phishingSeverityMedium
                                    }
                                    FilterChip(
                                        selected = alertSeverity == sev,
                                        onClick = { alertSeverity = sev },
                                        label = { Text(sevLabel) },
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    val tAr = alertTitleAr.trim().ifBlank { alertTitleEn.trim() }
                                    val tEn = alertTitleEn.trim().ifBlank { alertTitleAr.trim() }
                                    val mAr = alertMsgAr.trim().ifBlank { alertMsgEn.trim() }
                                    val mEn = alertMsgEn.trim().ifBlank { alertMsgAr.trim() }
                                    if (tAr.isNotEmpty() && mAr.isNotEmpty()) {
                                        onPublishPhishingAlert(
                                            tAr,
                                            tEn,
                                            mAr,
                                            mEn,
                                            alertCategory,
                                            alertSeverity,
                                            alertIndicator.trim(),
                                        )
                                        alertTitleAr = ""
                                        alertTitleEn = ""
                                        alertMsgAr = ""
                                        alertMsgEn = ""
                                        alertIndicator = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = dera3Colors.danger),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("admin_publish_alert_button"),
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(strings.adminPublishAlertBtn)
                            }
                        }
                    }
                }

                item(key = "admin_published_alerts_header") {
                    Text(
                        text = strings.adminAlertPublishedListTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }

                when (adminPhishingAlertsState) {
                    UiState.Loading -> {
                        item(key = "admin_alerts_loading") {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                    is UiState.Error -> {
                        item(key = "admin_alerts_error") {
                            StatusBanner(text = adminPhishingAlertsState.message, isDanger = true)
                        }
                    }
                    is UiState.Success -> {
                        if (adminPhishingAlertsState.data.isEmpty()) {
                            item(key = "admin_alerts_empty") {
                                Text(
                                    text = strings.phishingEmptyTitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        } else {
                            items(
                                items = adminPhishingAlertsState.data,
                                key = { "admin_alert_${it.id}" },
                            ) { alert ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = LocalizationManager.resolveLocalizedText(
                                                    preferences.language,
                                                    alert.titleAr,
                                                    alert.titleEn,
                                                ),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                            )
                                            if (alert.indicator.isNotBlank()) {
                                                Text(
                                                    text = alert.indicator,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                            Text(
                                                text = if (alert.active) strings.adminAlertActiveLabel else strings.adminAlertPausedLabel,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (alert.active) dera3Colors.safe else dera3Colors.danger,
                                            )
                                        }
                                        Switch(
                                            checked = alert.active,
                                            onCheckedChange = { onTogglePhishingAlertActive(alert.id, it) },
                                            modifier = Modifier.testTag("admin_alert_switch_${alert.id}"),
                                        )
                                        IconButton(
                                            onClick = { onDeletePhishingAlert(alert.id) },
                                            modifier = Modifier.size(48.dp).testTag("admin_alert_delete_${alert.id}"),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Delete,
                                                contentDescription = strings.adminDeleteAlertBtn,
                                                tint = dera3Colors.danger,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item(key = "admin_bottom_space") {
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}
