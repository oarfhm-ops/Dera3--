package com.example.domain

import java.net.URI
import java.security.SecureRandom

enum class RiskVerdict { SAFE, CAUTION, DANGER }

data class WifiAnalysisResult(
    val networkName: String,
    val isDangerous: Boolean,
)

data class LinkCheckItem(
    val index: Int,
    val passed: Boolean,
)

data class LinkAnalysisResult(
    val url: String,
    val isDangerous: Boolean,
    val checks: List<LinkCheckItem>,
)

enum class PasswordStrength { WEAK, MEDIUM, STRONG }

data class FakeAppAnalysisResult(
    val appName: String,
    val isFakeOrModded: Boolean,
)

enum class CallerOrigin { EGYPTIAN, FOREIGN_OR_UNKNOWN }

object SecurityAnalyzers {

    private val riskyWifiKeywords = listOf(
        "free", "مجاني", "مجانى", "كافيه", "cafe", "coffee",
        "مطعم", "restaurant", "open", "guest", "مفتوح", "مفتوحة", "public", "mall", "airport"
    )

    private val shortenerDomains = setOf(
        "bit.ly", "tinyurl.com", "t.co", "goo.gl", "rb.gy", "is.gd", "cutt.ly", "ow.ly", "shorturl.at"
    )

    private val suspiciousUrlWords = listOf(
        "free", "gift", "win", "prize", "bonus", "claim", "urgent", "verify", "update-bank",
        "هدية", "مبروك", "جائزة", "ربح", "مجاني", "تحديث"
    )

    private val typosquatDomains = listOf(
        "facebok", "facbook", "fb-login", "g00gle", "gooogle", "instagrm", "instagrarn",
        "whatsap-", "watsapp", "paypa1", "paypai", "amaz0n", "micr0soft", "cib-egypt", "banquemisr-"
    )

    private val riskyTlds = listOf(".xyz", ".top", ".click", ".tk", ".ml", ".ga", ".cf", ".gq", ".zip")

    private val fakeAppKeywords = listOf(
        "واتساب ذهبي", "الذهبي", "واتس ذهبي", "whatsapp gold", "gbwhatsapp", "gb whatsapp",
        "yowhatsapp", "fmwhatsapp", "whatsapp plus", "واتساب عمر", "واتساب بلس",
        "انستا مهكر", "مهكر", "mod apk", "cracked", "lucky patcher"
    )

    fun analyzeWifiName(rawName: String): WifiAnalysisResult {
        val cleaned = rawName.trim()
        val lower = cleaned.lowercase()
        val dangerous = riskyWifiKeywords.any { lower.contains(it) }
        return WifiAnalysisResult(networkName = cleaned, isDangerous = dangerous)
    }

    fun analyzeLink(rawUrl: String): LinkAnalysisResult {
        val trimmed = rawUrl.trim()
        val lower = trimmed.lowercase()
        val normalized = if (lower.startsWith("http://") || lower.startsWith("https://")) trimmed else "http://$trimmed"
        val host = try {
            URI(normalized).host?.lowercase().orEmpty()
        } catch (_: Exception) {
            ""
        }

        // 1. HTTPS check
        val hasHttps = lower.startsWith("https://")

        // 2. Shortener check
        val notShortened = shortenerDomains.none { host == it || host.endsWith(".$it") || lower.contains("$it/") }

        // 3. Suspicious keywords
        val noSuspiciousWords = suspiciousUrlWords.none { lower.contains(it) }

        // 4. Raw IPv4 instead of domain
        val ipRegex = Regex("""\b\d{1,3}(\.\d{1,3}){3}\b""")
        val noRawIp = !ipRegex.containsMatchIn(host.ifBlank { lower })

        // 5. Typosquatting lookalikes
        val noTyposquatting = typosquatDomains.none { lower.contains(it) }

        // 6. Strange TLD / @ trick / excessive hyphens
        val hasAtTrick = trimmed.contains("@")
        val hasRiskyTld = riskyTlds.any { host.endsWith(it) || lower.contains("$it/") }
        val excessiveHyphens = host.count { it == '-' } >= 3
        val cleanShape = !hasAtTrick && !hasRiskyTld && !excessiveHyphens && host.contains(".")

        val checks = listOf(
            LinkCheckItem(0, hasHttps),
            LinkCheckItem(1, notShortened),
            LinkCheckItem(2, noSuspiciousWords),
            LinkCheckItem(3, noRawIp),
            LinkCheckItem(4, noTyposquatting),
            LinkCheckItem(5, cleanShape),
        )

        val isDangerous = !hasHttps || !notShortened || !noRawIp || !noTyposquatting || checks.count { !it.passed } >= 2
        return LinkAnalysisResult(url = trimmed, isDangerous = isDangerous, checks = checks)
    }

    fun evaluatePasswordStrength(password: String): PasswordStrength {
        if (password.length < 8) return PasswordStrength.WEAK
        val hasUpper = password.any { it.isUpperCase() }
        val hasLower = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecial = password.any { !it.isLetterOrDigit() }
        return if (hasUpper && hasLower && hasDigit && (hasSpecial || password.length >= 12)) {
            PasswordStrength.STRONG
        } else if (hasUpper && hasDigit) {
            PasswordStrength.STRONG
        } else {
            PasswordStrength.MEDIUM
        }
    }

    fun generateStrongPassword(length: Int = 12): String {
        val upper = "ABCDEFGHJKLMNPQRSTUVWXYZ"
        val lower = "abcdefghijkmnopqrstuvwxyz"
        val digits = "23456789"
        val symbols = "!@#$%&*?+-"
        val all = upper + lower + digits + symbols
        val random = SecureRandom()

        val chars = mutableListOf(
            upper[random.nextInt(upper.length)],
            lower[random.nextInt(lower.length)],
            digits[random.nextInt(digits.length)],
            symbols[random.nextInt(symbols.length)],
        )
        while (chars.size < length) {
            chars.add(all[random.nextInt(all.length)])
        }
        chars.shuffle(random)
        return chars.joinToString("")
    }

    fun analyzeAppName(rawName: String): FakeAppAnalysisResult {
        val cleaned = rawName.trim()
        val lower = cleaned.lowercase()
        val isFake = fakeAppKeywords.any { lower.contains(it) }
        return FakeAppAnalysisResult(appName = cleaned, isFakeOrModded = isFake)
    }

    fun analyzeCallerNumber(rawNumber: String): CallerOrigin {
        val normalized = rawNumber.trim().replace(" ", "").replace("-", "")
        return if (normalized.startsWith("01") || normalized.startsWith("+20") || normalized.startsWith("0020")) {
            CallerOrigin.EGYPTIAN
        } else {
            CallerOrigin.FOREIGN_OR_UNKNOWN
        }
    }
}
