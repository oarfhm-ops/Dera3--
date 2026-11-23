package com.example.data.model

import androidx.annotation.Keep
import com.example.localization.AppLanguage
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class ShieldId(val key: String, val index: Int, val maxPoints: Int) {
    WIFI("shield_1", 0, 15),
    WHATSAPP("shield_2", 1, 20),
    BREACH("shield_3", 2, 10),
    PRIVACY("shield_4", 3, 15),
    ELDERLY("shield_5", 4, 10),
    LINK_SCANNER("shield_6", 5, 10),
    HONEYPOT("shield_7", 6, 10),
    PASSWORD("shield_8", 7, 10),
    FAKE_APPS("shield_9", 8, 5),
    SCAM_CALLS("shield_10", 9, 5),
    AI_ASSISTANT("shield_11", 10, 5),
    SAFE_BROWSER("shield_12", 11, 10),
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fullName: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val phoneVerified: Boolean = false,
    val securityScore: Long = 0L,
    val completedChecks: List<String> = emptyList(),
    val emergencyContact: String = "",
    val honeypotLogs: List<String> = emptyList(),
    val passwordMasked: String = "••••••••",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

enum class SecurityBadge(
    val badgeId: String,
    val threshold: Int,
    val titleEn: String,
    val titleAr: String,
) {
    CYBER_DEFENDER(
        badgeId = "cyber_defender",
        threshold = 50,
        titleEn = "Cyber Defender",
        titleAr = "حارس الأمان",
    ),
    SECURITY_EXPERT(
        badgeId = "security_expert",
        threshold = 100,
        titleEn = "Security Expert",
        titleAr = "خبير الأمان",
    ),
}

@Keep
@IgnoreExtraProperties
data class UserBadge(
    @DocumentId val id: String = "",
    val badgeId: String = "",
    val userId: String = "",
    val threshold: Long = 0L,
    val titleEn: String = "",
    val titleAr: String = "",
    @ServerTimestamp val unlockedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class ShieldConfig(
    @DocumentId val id: String = "",
    val disabledShieldIds: List<String> = emptyList(),
    val updatedBy: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

data class AppPreferences(
    val darkMode: Boolean = false,
    val language: AppLanguage = AppLanguage.AR,
    val notificationsEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
)

enum class PhishingCategory(val targetShield: ShieldId) {
    LINK_PHISHING(ShieldId.LINK_SCANNER),
    BANK_IMPERSONATION(ShieldId.SCAM_CALLS),
    FAKE_APP(ShieldId.FAKE_APPS),
    OTP_SCAM(ShieldId.WHATSAPP),
    SMS_DELIVERY(ShieldId.LINK_SCANNER);

    companion object {
        fun fromRaw(raw: String?): PhishingCategory =
            entries.firstOrNull { it.name == raw } ?: LINK_PHISHING
    }
}

enum class ThreatSeverity {
    MEDIUM,
    HIGH,
    CRITICAL;

    companion object {
        fun fromRaw(raw: String?): ThreatSeverity =
            entries.firstOrNull { it.name == raw } ?: HIGH
    }
}

@Keep
@IgnoreExtraProperties
data class PhishingAlert(
    @DocumentId val id: String = "",
    val titleAr: String = "",
    val titleEn: String = "",
    val messageAr: String = "",
    val messageEn: String = "",
    val category: String = PhishingCategory.LINK_PHISHING.name,
    val severity: String = ThreatSeverity.HIGH.name,
    val indicator: String = "",
    val active: Boolean = true,
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class AlertAck(
    @DocumentId val id: String = "",
    val alertId: String = "",
    val userId: String = "",
    val read: Boolean = false,
    val dismissed: Boolean = false,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class DetectedThreat(
    @DocumentId val id: String = "",
    val userId: String = "",
    val shieldKey: String = "",
    val category: String = PhishingCategory.LINK_PHISHING.name,
    val severity: String = ThreatSeverity.HIGH.name,
    val indicator: String = "",
    val titleAr: String = "",
    val titleEn: String = "",
    val messageAr: String = "",
    val messageEn: String = "",
    val read: Boolean = false,
    val dismissed: Boolean = false,
    @ServerTimestamp val detectedAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

data class PhishingNotificationItem(
    val id: String,
    val isPersonalDetection: Boolean,
    val titleAr: String,
    val titleEn: String,
    val messageAr: String,
    val messageEn: String,
    val category: PhishingCategory,
    val severity: ThreatSeverity,
    val indicator: String,
    val targetShield: ShieldId,
    val read: Boolean,
    val dismissed: Boolean,
    val timestampSeconds: Long,
)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
