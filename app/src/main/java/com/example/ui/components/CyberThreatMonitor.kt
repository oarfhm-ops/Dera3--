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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AppBlocking
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhoneMissed
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SmsFailed
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.PhishingCategory
import com.example.data.model.PhishingNotificationItem
import com.example.data.model.ShieldConfig
import com.example.data.model.ShieldId
import com.example.data.model.ThreatSeverity
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.localization.AppLanguage
import com.example.localization.LocalStrings
import com.example.localization.LocalizationManager
import com.example.ui.theme.AmberCaution
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.CyanBright
import com.example.ui.theme.EmeraldSafe
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.NavySurface
import com.example.ui.theme.NavySurfaceElevated
import kotlinx.coroutines.delay

enum class ThreatFeedSource {
    FIRESTORE_GLOBAL,
    FIRESTORE_PERSONAL,
    SIMULATED_LIVE,
}

enum class ThreatFilterOption {
    ALL,
    CRITICAL_HIGH,
    RELEVANT_TO_ME,
}

@Immutable
data class CyberThreatFeedItem(
    val id: String,
    val titleAr: String,
    val titleEn: String,
    val messageAr: String,
    val messageEn: String,
    val category: PhishingCategory,
    val severity: ThreatSeverity,
    val indicator: String,
    val targetShield: ShieldId,
    val source: ThreatFeedSource,
    val isRelevantToUser: Boolean,
    val timestampSeconds: Long,
    val isAcknowledged: Boolean = false,
    val firestoreItem: PhishingNotificationItem? = null,
)

private data class SimulatedThreatTemplate(
    val key: String,
    val shield: ShieldId,
    val category: PhishingCategory,
    val severity: ThreatSeverity,
    val indicator: String,
    val titleAr: String,
    val titleEn: String,
    val messageAr: String,
    val messageEn: String,
)

object CyberThreatMonitorEngine {

    private val simulatedTemplates: List<SimulatedThreatTemplate> = listOf(
        SimulatedThreatTemplate(
            key = "sim_wifi_cafe",
            shield = ShieldId.WIFI,
            category = PhishingCategory.LINK_PHISHING,
            severity = ThreatSeverity.HIGH,
            indicator = "SSID: Cafe_Free_WiFi_5G",
            titleAr = "محاكاة: شبكة واي فاي عامة مكشوفة",
            titleEn = "Simulated: Open Café Wi-Fi Trap",
            messageAr = "تم رصد شبكة مفتوحة بدون تشفير قد تعترض كلمات السر — فعّل درع الواي فاي وتجنب المعاملات البنكية عليها.",
            messageEn = "Unencrypted public hotspot detected that can sniff logins — complete Wi-Fi Shield checks before browsing.",
        ),
        SimulatedThreatTemplate(
            key = "sim_wa_linked",
            shield = ShieldId.WHATSAPP,
            category = PhishingCategory.OTP_SCAM,
            severity = ThreatSeverity.CRITICAL,
            indicator = "WA-Web Session • Chrome / Unknown",
            titleAr = "محاكاة: محاولة ربط واتساب على جهاز غريب",
            titleEn = "Simulated: Suspicious WhatsApp Link Request",
            messageAr = "انتحال كود تفعيل 6 أرقام أو جلسة ويب غير معروفة — راجع الأجهزة المرتبطة وفعّل التحقق بخطوتين.",
            messageEn = "Attempted 6-digit verification hijack or rogue web session — audit Linked Devices & 2-Step PIN.",
        ),
        SimulatedThreatTemplate(
            key = "sim_link_delivery",
            shield = ShieldId.LINK_SCANNER,
            category = PhishingCategory.SMS_DELIVERY,
            severity = ThreatSeverity.CRITICAL,
            indicator = "http://bit.ly/egy-post-fee25",
            titleAr = "محاكاة: رابط رسوم شحنة مزيف عبر SMS",
            titleEn = "Simulated: Fake Parcel Fee Phishing URL",
            messageAr = "رسالة تطلب 25 جنيهاً لتحديث عنوان شحنة عبر رابط مختصر مشبوه — افحص الرابط في درع التنبؤ ولا تفتحه.",
            messageEn = "Shortened URL asking for a small parcel delivery fee — verify links in Predictive Link Scanner first.",
        ),
        SimulatedThreatTemplate(
            key = "sim_bank_call",
            shield = ShieldId.SCAM_CALLS,
            category = PhishingCategory.BANK_IMPERSONATION,
            severity = ThreatSeverity.CRITICAL,
            indicator = "+2010-991-BANK-OTP",
            titleAr = "محاكاة: مكالمة تنتحل صفة خدمة عملاء البنك",
            titleEn = "Simulated: Fake Bank Support OTP Call",
            messageAr = "المتصل يطلب كود OTP لتحديث البطاقة البنكية — البنوك لا تطلب الأكواد أبداً، احظر الرقم فوراً.",
            messageEn = "Caller requesting an OTP code to reactivate a card — banks never ask for codes over the phone.",
        ),
        SimulatedThreatTemplate(
            key = "sim_fake_app",
            shield = ShieldId.FAKE_APPS,
            category = PhishingCategory.FAKE_APP,
            severity = ThreatSeverity.HIGH,
            indicator = "WhatsApp_Gold_Pro_v14.apk",
            titleAr = "محاكاة: تحذير من تطبيق واتساب ذهبي معدّل",
            titleEn = "Simulated: Modded WhatsApp Gold APK Alert",
            messageAr = "حزمة APK من خارج المتجر الرسمي قد تسرق الرسائل وجهات الاتصال — افحص التطبيقات في درع التطبيقات المزيفة.",
            messageEn = "Unofficial sideloaded APK risks full chat theft — verify apps inside Fake Apps Shield.",
        ),
        SimulatedThreatTemplate(
            key = "sim_breach_email",
            shield = ShieldId.BREACH,
            category = PhishingCategory.OTP_SCAM,
            severity = ThreatSeverity.HIGH,
            indicator = "Credential Leak Feed #2025-Q2",
            titleAr = "محاكاة: تنبيه تسريب إيميلات على الويب المظلم",
            titleEn = "Simulated: Email Breach Dump Advisory",
            messageAr = "ظهور قوائم إيميلات مسربة تتطلب تغيير كلمة السر وتفعيل المصادقة الثنائية عبر درع التسريب.",
            messageEn = "Fresh credential dump spotted — audit your email in Data Breach Shield and enable 2FA.",
        ),
        SimulatedThreatTemplate(
            key = "sim_privacy_mic",
            shield = ShieldId.PRIVACY,
            category = PhishingCategory.FAKE_APP,
            severity = ThreatSeverity.MEDIUM,
            indicator = "android.permission.RECORD_AUDIO",
            titleAr = "محاكاة: طلب إذن ميكروفون وكاميرا بالخلفية",
            titleEn = "Simulated: Background Mic & Camera Audit",
            messageAr = "بعض التطبيقات تطلب صلاحيات كاميرا وموقع غير لازمة — راجع الأذونات في درع المراقبة.",
            messageEn = "Apps holding unused camera, mic, or GPS permissions — audit access in Privacy Shield.",
        ),
        SimulatedThreatTemplate(
            key = "sim_password_weak",
            shield = ShieldId.PASSWORD,
            category = PhishingCategory.OTP_SCAM,
            severity = ThreatSeverity.MEDIUM,
            indicator = "Credential Stuffing Wave",
            titleAr = "محاكاة: موجة تخمين كلمات السر الضعيفة",
            titleEn = "Simulated: Password Brute-Force Wave",
            messageAr = "كلمات السر القصيرة والمتكررة معرضة للاختراق الآلي — ولّد كلمة سر 12 حرفاً في درع كلمة السر.",
            messageEn = "Reused short passwords are being targeted — generate a 12-char password in Password Shield.",
        ),
        SimulatedThreatTemplate(
            key = "sim_honeypot_probe",
            shield = ShieldId.HONEYPOT,
            category = PhishingCategory.LINK_PHISHING,
            severity = ThreatSeverity.HIGH,
            indicator = "/admin-login-trap • Bot Scan",
            titleAr = "محاكاة: رصد فحص آلي متكرر على المصيدة",
            titleEn = "Simulated: Automated Probe Against Decoy",
            messageAr = "رصد محاولة فحص استطلاعي آلي — فعّل مصيدة الهاكرز لتسجيل أي تسلل مبكراً.",
            messageEn = "Recon scan pattern observed — arm Hacker Honeypot to trap and log suspicious probes.",
        ),
        SimulatedThreatTemplate(
            key = "sim_elderly_scam",
            shield = ShieldId.ELDERLY,
            category = PhishingCategory.BANK_IMPERSONATION,
            severity = ThreatSeverity.HIGH,
            indicator = "Family SOS Shield Check",
            titleAr = "محاكاة: حملة تستهدف كبار السن برسائل جوائز",
            titleEn = "Simulated: Prize Scam Targeting Seniors",
            messageAr = "رسائل جوائز وهمية تستهدف الأهل — احفظ رقم طوارئ العائلة وشارك رسالة التوعية في درع كبار السن.",
            messageEn = "Fake lottery calls targeting seniors — set a family emergency number in Elderly Shield.",
        ),
        SimulatedThreatTemplate(
            key = "sim_safe_browser_ad",
            shield = ShieldId.SAFE_BROWSER,
            category = PhishingCategory.LINK_PHISHING,
            severity = ThreatSeverity.HIGH,
            indicator = "popads.net / Malicious Redirect",
            titleAr = "محاكاة: نافذة إعلانية وموقع حساس ضار",
            titleEn = "Simulated: Malvertising & Sensitive Site Trap",
            messageAr = "نوافذ منبثقة وإعلانات مضللة تحاول تحويلك لمواقع حساسة أو مزيفة — تصفح من درع المتصفح الآمن لحظرها.",
            messageEn = "Pop-up ad trackers attempting sensitive/phishing redirects — browse inside Safe Browser Shield.",
        ),
    )

    fun requiredChecksForShield(shield: ShieldId): List<String> = when (shield) {
        ShieldId.WIFI -> listOf("wifi_1", "wifi_2", "wifi_3")
        ShieldId.WHATSAPP -> listOf("wa_1", "wa_2", "wa_3", "wa_btn")
        ShieldId.BREACH -> listOf("breach_1", "breach_2")
        ShieldId.PRIVACY -> listOf("priv_1", "priv_2", "priv_3")
        ShieldId.ELDERLY -> listOf("elderly_contact")
        ShieldId.LINK_SCANNER -> listOf("link_scan")
        ShieldId.HONEYPOT -> listOf("honeypot_active")
        ShieldId.PASSWORD -> listOf("pw_check", "pw_gen")
        ShieldId.FAKE_APPS -> listOf("fake_app_scan")
        ShieldId.SCAM_CALLS -> listOf("scam_call_scan")
        ShieldId.AI_ASSISTANT -> listOf("ai_ask_1")
        ShieldId.SAFE_BROWSER -> listOf("safe_browser_use")
    }

    fun incompleteShields(userProfile: UserProfile, shieldConfig: ShieldConfig): Set<ShieldId> {
        val completed = userProfile.completedChecks.toSet()
        val disabled = shieldConfig.disabledShieldIds.toSet()
        return ShieldId.entries.filterTo(mutableSetOf()) { shield ->
            !disabled.contains(shield.key) &&
                requiredChecksForShield(shield).any { checkId -> !completed.contains(checkId) }
        }
    }

    fun buildTailoredSimulatedPool(
        userProfile: UserProfile,
        shieldConfig: ShieldConfig,
        baseTimestampSeconds: Long = System.currentTimeMillis() / 1000L,
    ): List<CyberThreatFeedItem> {
        val disabled = shieldConfig.disabledShieldIds.toSet()
        val openShields = incompleteShields(userProfile, shieldConfig)

        val enabledTemplates = simulatedTemplates.filter { !disabled.contains(it.shield.key) }
        val sortedTemplates = enabledTemplates.sortedWith(
            compareByDescending<SimulatedThreatTemplate> { openShields.contains(it.shield) }
                .thenByDescending { it.severity.ordinal }
        )

        return sortedTemplates.mapIndexed { idx, tpl ->
            CyberThreatFeedItem(
                id = "${tpl.key}_$idx",
                titleAr = tpl.titleAr.take(120),
                titleEn = tpl.titleEn.take(120),
                messageAr = tpl.messageAr.take(500),
                messageEn = tpl.messageEn.take(500),
                category = tpl.category,
                severity = tpl.severity,
                indicator = tpl.indicator.take(180),
                targetShield = tpl.shield,
                source = ThreatFeedSource.SIMULATED_LIVE,
                isRelevantToUser = openShields.contains(tpl.shield),
                timestampSeconds = baseTimestampSeconds - (idx * 15L),
            )
        }
    }

    fun mergeAndFilterFeed(
        firestoreAlerts: List<PhishingNotificationItem>,
        simulatedAlerts: List<CyberThreatFeedItem>,
        filter: ThreatFilterOption,
        userProfile: UserProfile,
        shieldConfig: ShieldConfig,
    ): List<CyberThreatFeedItem> {
        val openShields = incompleteShields(userProfile, shieldConfig)
        val disabled = shieldConfig.disabledShieldIds.toSet()

        val firestoreItems = firestoreAlerts.map { alert ->
            val shieldDisabled = disabled.contains(alert.targetShield.key)
            val relevant = alert.isPersonalDetection ||
                (!shieldDisabled && openShields.contains(alert.targetShield))
            CyberThreatFeedItem(
                id = "fs_${if (alert.isPersonalDetection) "p" else "g"}_${alert.id}",
                titleAr = alert.titleAr,
                titleEn = alert.titleEn,
                messageAr = alert.messageAr,
                messageEn = alert.messageEn,
                category = alert.category,
                severity = alert.severity,
                indicator = alert.indicator,
                targetShield = alert.targetShield,
                source = if (alert.isPersonalDetection) {
                    ThreatFeedSource.FIRESTORE_PERSONAL
                } else {
                    ThreatFeedSource.FIRESTORE_GLOBAL
                },
                isRelevantToUser = relevant,
                timestampSeconds = alert.timestampSeconds,
                isAcknowledged = alert.read,
                firestoreItem = alert,
            )
        }

        val firestoreIndicators = firestoreItems.mapNotNull {
            it.indicator.trim().lowercase().takeIf { ind -> ind.isNotBlank() }
        }.toSet()

        // Avoid duplicating an alert that is already synced in Firestore with the same indicator
        val dedupedSimulated = simulatedAlerts.filterNot { sim ->
            val ind = sim.indicator.trim().lowercase()
            ind.isNotBlank() && firestoreIndicators.contains(ind)
        }.map { sim ->
            sim.copy(
                isRelevantToUser = !disabled.contains(sim.targetShield.key) &&
                    openShields.contains(sim.targetShield)
            )
        }

        val merged = (firestoreItems + dedupedSimulated).sortedWith(
            compareBy<CyberThreatFeedItem> { it.isAcknowledged }
                .thenByDescending { it.isRelevantToUser }
                .thenByDescending { it.severity.ordinal }
                .thenByDescending { it.timestampSeconds }
        )

        return when (filter) {
            ThreatFilterOption.ALL -> merged
            ThreatFilterOption.CRITICAL_HIGH -> merged.filter {
                it.severity == ThreatSeverity.CRITICAL || it.severity == ThreatSeverity.HIGH
            }
            ThreatFilterOption.RELEVANT_TO_ME -> merged.filter { it.isRelevantToUser }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CyberThreatMonitor(
    userProfile: UserProfile,
    shieldConfig: ShieldConfig,
    phishingNotificationsState: UiState<List<PhishingNotificationItem>>,
    language: AppLanguage,
    onFocusShield: (ShieldId) -> Unit,
    onAcknowledgeAlert: (PhishingNotificationItem) -> Unit,
    onPersistSimulatedThreat: (CyberThreatFeedItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val firestoreAlerts = (phishingNotificationsState as? UiState.Success)?.data.orEmpty()

    val tailoredPool = remember(
        userProfile.completedChecks,
        shieldConfig.disabledShieldIds,
    ) {
        CyberThreatMonitorEngine.buildTailoredSimulatedPool(userProfile, shieldConfig)
    }

    var isFeedLive by remember { mutableStateOf(true) }
    var selectedFilter by remember { mutableStateOf(ThreatFilterOption.ALL) }
    var showAllAlerts by remember { mutableStateOf(false) }
    var nextPoolCursor by remember { mutableIntStateOf(0) }
    var simulatedStream by remember {
        mutableStateOf(
            tailoredPool.take(2).mapIndexed { index, item ->
                item.copy(
                    id = "sim_init_${item.targetShield.key}_$index",
                    timestampSeconds = (System.currentTimeMillis() / 1000L) - (index * 5L),
                )
            }
        )
    }

    LaunchedEffect(tailoredPool) {
        if (simulatedStream.isEmpty() && tailoredPool.isNotEmpty()) {
            simulatedStream = tailoredPool.take(2)
            nextPoolCursor = 2 % tailoredPool.size
        }
    }

    val emitNextSimulatedAlert: () -> Unit = {
        if (tailoredPool.isNotEmpty()) {
            val idx = nextPoolCursor % tailoredPool.size
            val template = tailoredPool[idx]
            val nowSec = System.currentTimeMillis() / 1000L
            val nextItem = template.copy(
                id = "sim_${template.targetShield.key}_${nowSec}_$nextPoolCursor",
                timestampSeconds = nowSec,
            )
            simulatedStream = (listOf(nextItem) + simulatedStream.filterNot {
                it.indicator == nextItem.indicator
            }).take(6)
            nextPoolCursor = (idx + 1) % tailoredPool.size
        }
    }

    LaunchedEffect(isFeedLive, tailoredPool) {
        if (isFeedLive && tailoredPool.isNotEmpty()) {
            while (true) {
                delay(7500L)
                emitNextSimulatedAlert()
            }
        }
    }

    val allMergedAlerts = remember(
        firestoreAlerts,
        simulatedStream,
        userProfile.completedChecks,
        shieldConfig.disabledShieldIds,
    ) {
        CyberThreatMonitorEngine.mergeAndFilterFeed(
            firestoreAlerts = firestoreAlerts,
            simulatedAlerts = simulatedStream,
            filter = ThreatFilterOption.ALL,
            userProfile = userProfile,
            shieldConfig = shieldConfig,
        )
    }

    val filteredAlerts = remember(
        firestoreAlerts,
        simulatedStream,
        selectedFilter,
        userProfile.completedChecks,
        shieldConfig.disabledShieldIds,
    ) {
        CyberThreatMonitorEngine.mergeAndFilterFeed(
            firestoreAlerts = firestoreAlerts,
            simulatedAlerts = simulatedStream,
            filter = selectedFilter,
            userProfile = userProfile,
            shieldConfig = shieldConfig,
        )
    }

    val relevantCount = remember(allMergedAlerts) { allMergedAlerts.count { it.isRelevantToUser } }
    val cloudCount = remember(firestoreAlerts) { firestoreAlerts.size }
    val visibleAlerts = if (showAllAlerts) filteredAlerts else filteredAlerts.take(4)

    val pulseTransition = rememberInfiniteTransition(label = "cyber_monitor_pulse")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "monitor_live_dot_alpha",
    )

    val liveColor = if (isFeedLive) CyanBright else AmberCaution

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("cyber_threat_monitor_card"),
        shape = RoundedCornerShape(24.dp),
        color = NavyDeep,
        border = BorderStroke(1.dp, liveColor.copy(alpha = 0.32f)),
        shadowElevation = 5.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(NavyDeep, NavySurface, NavySurfaceElevated)
                    )
                )
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                liveColor.copy(alpha = 0.16f),
                                Color.Transparent,
                            ),
                            center = Offset(size.width * 0.85f, size.height * 0.15f),
                            radius = size.maxDimension * 0.55f,
                        ),
                        radius = size.maxDimension * 0.55f,
                        center = Offset(size.width * 0.85f, size.height * 0.15f),
                    )
                }
                .padding(18.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Header with live indicator pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(liveColor.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MonitorHeart,
                                contentDescription = null,
                                tint = liveColor,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = strings.cyberMonitorTitle,
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = strings.cyberMonitorSubtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.72f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    Surface(
                        color = liveColor.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, liveColor.copy(alpha = 0.5f)),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .alpha(if (isFeedLive) pulseAlpha else 0.75f)
                                    .clip(CircleShape)
                                    .background(liveColor),
                            )
                            Text(
                                text = if (isFeedLive) {
                                    strings.cyberMonitorLiveFeedOn
                                } else {
                                    strings.cyberMonitorLiveFeedPaused
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }

                // Telemetry Counters Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MonitorCounterPill(
                        value = "${allMergedAlerts.size}",
                        label = strings.cyberMonitorTotalCountLabel,
                        accent = CyanBright,
                        modifier = Modifier.weight(1f),
                    )
                    MonitorCounterPill(
                        value = "$relevantCount",
                        label = strings.cyberMonitorRelevantCountLabel,
                        accent = if (relevantCount > 0) AmberCaution else EmeraldSafe,
                        modifier = Modifier.weight(1f),
                    )
                    MonitorCounterPill(
                        value = "$cloudCount",
                        label = strings.cyberMonitorCloudCountLabel,
                        accent = EmeraldSafe,
                        modifier = Modifier.weight(1f),
                    )
                }

                // Controls Row: Pause/Resume & Trigger Simulated Alert (>= 48.dp touch target)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = { isFeedLive = !isFeedLive },
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("cyber_monitor_live_toggle"),
                    ) {
                        Icon(
                            imageVector = if (isFeedLive) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (isFeedLive) strings.cyberMonitorPauseBtn else strings.cyberMonitorResumeBtn,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Button(
                        onClick = emitNextSimulatedAlert,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanBright,
                            contentColor = NavyDeep,
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("cyber_monitor_simulate_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Bolt,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = strings.cyberMonitorSimulateBtn,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                // Scrollable Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val filterOptions = listOf(
                        ThreatFilterOption.ALL to strings.cyberMonitorFilterAll,
                        ThreatFilterOption.RELEVANT_TO_ME to strings.cyberMonitorFilterRelevant,
                        ThreatFilterOption.CRITICAL_HIGH to strings.cyberMonitorFilterCritical,
                    )
                    filterOptions.forEach { (option, label) ->
                        val selected = selectedFilter == option
                        FilterChip(
                            selected = selected,
                            onClick = { selectedFilter = option },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color.White.copy(alpha = 0.06f),
                                labelColor = Color.White.copy(alpha = 0.85f),
                                selectedContainerColor = CyanBright.copy(alpha = 0.24f),
                                selectedLabelColor = CyanBright,
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selected,
                                borderColor = Color.White.copy(alpha = 0.2f),
                                selectedBorderColor = CyanBright,
                            ),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("cyber_monitor_filter_${option.name.lowercase()}"),
                        )
                    }
                }

                // Feed Items
                if (visibleAlerts.isEmpty()) {
                    Surface(
                        color = Color.White.copy(alpha = 0.06f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("cyber_monitor_empty_state"),
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.VerifiedUser,
                                contentDescription = null,
                                tint = EmeraldSafe,
                                modifier = Modifier.size(22.dp),
                            )
                            Text(
                                text = strings.cyberMonitorEmptyFiltered,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.85f),
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        visibleAlerts.forEach { item ->
                            CyberThreatFeedCard(
                                item = item,
                                language = language,
                                onSecureShield = {
                                    item.firestoreItem?.let(onAcknowledgeAlert)
                                    onFocusShield(item.targetShield)
                                },
                                onPersistSimulated = { onPersistSimulatedThreat(item) },
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = filteredAlerts.size > 4,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                        TextButton(
                            onClick = { showAllAlerts = !showAllAlerts },
                            colors = ButtonDefaults.textButtonColors(contentColor = CyanBright),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("cyber_monitor_expand_toggle"),
                        ) {
                            Icon(
                                imageVector = if (showAllAlerts) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = null,
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (showAllAlerts) {
                                    strings.cyberMonitorShowLessBtn
                                } else {
                                    LocalizationManager.formatString(
                                        strings.cyberMonitorShowMoreBtn,
                                        filteredAlerts.size,
                                    )
                                },
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }

                // Clear awareness disclaimer
                Surface(
                    color = Color.White.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Sensors,
                            contentDescription = null,
                            tint = CyanBright,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = strings.cyberMonitorDisclaimer,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.72f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MonitorCounterPill(
    value: String,
    label: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.07f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.28f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoFontFamily),
                color = accent,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CyberThreatFeedCard(
    item: CyberThreatFeedItem,
    language: AppLanguage,
    onSecureShield: () -> Unit,
    onPersistSimulated: () -> Unit,
) {
    val strings = LocalStrings.current
    val accentColor = when (item.severity) {
        ThreatSeverity.CRITICAL -> CrimsonAlert
        ThreatSeverity.HIGH -> AmberCaution
        ThreatSeverity.MEDIUM -> CyanBright
    }
    val severityLabel = when (item.severity) {
        ThreatSeverity.CRITICAL -> strings.phishingSeverityCritical
        ThreatSeverity.HIGH -> strings.phishingSeverityHigh
        ThreatSeverity.MEDIUM -> strings.phishingSeverityMedium
    }
    val sourceLabel = when (item.source) {
        ThreatFeedSource.FIRESTORE_GLOBAL -> strings.cyberMonitorSourceFirestoreCloud
        ThreatFeedSource.FIRESTORE_PERSONAL -> strings.cyberMonitorSourceFirestoreDevice
        ThreatFeedSource.SIMULATED_LIVE -> strings.cyberMonitorSourceSimulated
    }
    val sourceIcon: ImageVector = when (item.source) {
        ThreatFeedSource.FIRESTORE_GLOBAL -> Icons.Filled.CloudQueue
        ThreatFeedSource.FIRESTORE_PERSONAL -> Icons.Filled.CloudDone
        ThreatFeedSource.SIMULATED_LIVE -> Icons.Filled.Sensors
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
    val shieldName = strings.shieldTitles.getOrElse(item.targetShield.index) { item.targetShield.name }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("cyber_monitor_item_${item.id}"),
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = if (item.isAcknowledged) 0.04f else 0.08f),
        border = BorderStroke(
            width = 1.dp,
            color = accentColor.copy(alpha = if (item.isRelevantToUser) 0.55f else 0.25f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentColor.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = categoryIcon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp),
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        MonitorSmallBadge(
                            text = severityLabel,
                            color = accentColor,
                        )
                        MonitorSmallBadge(
                            text = sourceLabel,
                            color = if (item.source == ThreatFeedSource.SIMULATED_LIVE) CyanBright else EmeraldSafe,
                            icon = sourceIcon,
                        )
                        if (item.isRelevantToUser) {
                            MonitorSmallBadge(
                                text = strings.cyberMonitorRelevanceHigh,
                                color = AmberCaution,
                            )
                        }
                    }
                }
            }

            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.82f),
            )

            if (item.indicator.isNotBlank()) {
                Surface(
                    color = NavyDeep.copy(alpha = 0.72f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = item.indicator,
                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily),
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
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = onSecureShield,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = if (accentColor == AmberCaution) NavyDeep else Color.White,
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("cyber_monitor_fix_${item.id}"),
                ) {
                    Text(
                        text = "${strings.cyberMonitorSecuringShieldBtn}: $shieldName",
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                }

                if (item.source == ThreatFeedSource.SIMULATED_LIVE) {
                    OutlinedButton(
                        onClick = onPersistSimulated,
                        border = BorderStroke(1.dp, CyanBright.copy(alpha = 0.55f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanBright),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("cyber_monitor_persist_${item.id}"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CloudUpload,
                            contentDescription = strings.cyberMonitorSaveToCloudBtn,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = strings.cyberMonitorSaveToCloudBtn,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MonitorSmallBadge(
    text: String,
    color: Color,
    icon: ImageVector? = null,
) {
    Surface(
        color = color.copy(alpha = 0.16f),
        shape = RoundedCornerShape(50),
        border = BorderStroke(0.8.dp, color.copy(alpha = 0.45f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(12.dp),
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
